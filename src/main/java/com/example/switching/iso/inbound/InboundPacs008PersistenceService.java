package com.example.switching.iso.inbound;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.switching.common.util.TransferRefGenerator;
import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoMessageDirection;
import com.example.switching.iso.enums.IsoMessageType;
import com.example.switching.iso.enums.IsoSecurityStatus;
import com.example.switching.iso.enums.IsoValidationStatus;
import com.example.switching.iso.repository.IsoMessageRepository;
import com.example.switching.transfer.entity.TransferEntity;
import com.example.switching.transfer.entity.TransferStatusHistoryEntity;
import com.example.switching.transfer.enums.TransferStatus;
import com.example.switching.transfer.repository.TransferRepository;
import com.example.switching.transfer.repository.TransferStatusHistoryRepository;

@Service
public class InboundPacs008PersistenceService {

    private static final String ISO_INBOUND_CHANNEL_ID = "ISO20022_XML";

    private final TransferRefGenerator transferRefGenerator;
    private final TransferRepository transferRepository;
    private final TransferStatusHistoryRepository transferStatusHistoryRepository;
    private final IsoMessageRepository isoMessageRepository;

    public InboundPacs008PersistenceService(
            TransferRefGenerator transferRefGenerator,
            TransferRepository transferRepository,
            TransferStatusHistoryRepository transferStatusHistoryRepository,
            IsoMessageRepository isoMessageRepository
    ) {
        this.transferRefGenerator = transferRefGenerator;
        this.transferRepository = transferRepository;
        this.transferStatusHistoryRepository = transferStatusHistoryRepository;
        this.isoMessageRepository = isoMessageRepository;
    }

    @Transactional
    public InboundPacs008PersistResult persistAcceptedInboundPacs008(
            Pacs008InboundRequest request,
            String rawXml
    ) {
        String transferRef = transferRefGenerator.generate();
        LocalDateTime now = LocalDateTime.now();

        String clientTransferId = firstNonBlank(
                request.getInstructionId(),
                request.getEndToEndId(),
                request.getMessageId(),
                transferRef
        );

        String idempotencyKey = "ISO20022:"
                + safePart(request.getMessageId())
                + ":"
                + safePart(request.getInstructionId())
                + ":"
                + safePart(request.getEndToEndId());

        TransferEntity transfer = new TransferEntity();

        setValue(transfer, "transferRef", transferRef);

        // Required by transfers.channel_id.
        setValueIfPresent(transfer, ISO_INBOUND_CHANNEL_ID, "channelId", "channelID");

        // Required / useful for ISO-only inbound tracking.
        setValueIfPresent(transfer, clientTransferId, "clientTransferId", "clientTransactionId");
        setValueIfPresent(transfer, idempotencyKey, "idempotencyKey");

        // ISO-only inbound transfer has no inquiryRef.
        setValueIfPresent(transfer, null, "inquiryRef");

        // Existing project fields appear to use *_bank_code and *_account_no.
        setValueIfPresent(transfer, request.getDebtorAgentBic(), "sourceBankCode", "sourceBank");
        setValueIfPresent(transfer, request.getCreditorAgentBic(), "destinationBankCode", "destinationBank");

        setValueIfPresent(transfer, request.getDebtorAccount(), "sourceAccountNo", "debtorAccount");
        setValueIfPresent(transfer, request.getCreditorAccount(), "destinationAccountNo", "creditorAccount");

        setValueIfPresent(transfer, "ISO INBOUND RECEIVER", "destinationAccountName");
        setValueIfPresent(transfer, request.getAmount(), "amount");
        setValueIfPresent(transfer, request.getCurrency(), "currency");
        setValueIfPresent(transfer, request.getRemittanceInformation(), "reference");

        // These are intentionally null in ISO-IN-1B.1.
        // ISO-IN-1B.2 will populate them after routing resolution.
        setValueIfPresent(transfer, null, "routeCode");
        setValueIfPresent(transfer, null, "connectorName");
        setValueIfPresent(transfer, null, "externalReference");
        setValueIfPresent(transfer, null, "errorCode");
        setValueIfPresent(transfer, null, "errorMessage");

        setEnumOrStringValue(transfer, "status", TransferStatus.RECEIVED);

        setValueIfPresent(transfer, now, "createdAt", "createdDate", "createdOn");
        setValueIfPresent(transfer, now, "updatedAt", "updatedDate", "updatedOn");

        transferRepository.save(transfer);

        TransferStatusHistoryEntity history = new TransferStatusHistoryEntity();

        setValueIfPresent(history, transferRef, "transferRef");
        setEnumOrStringValue(history, "status", TransferStatus.RECEIVED);
        setValueIfPresent(history, null, "reasonCode");
        setValueIfPresent(
                history,
                "Inbound PACS.008 accepted from member bank",
                "message",
                "description",
                "reason",
                "remarks",
                "note"
        );
        setValueIfPresent(history, now, "createdAt", "createdDate", "createdOn");

        transferStatusHistoryRepository.save(history);

        IsoMessageEntity isoMessage = new IsoMessageEntity();

        setValueIfPresent(isoMessage, transferRef, "correlationRef");
        setValueIfPresent(isoMessage, transferRef, "transferRef");
        setValueIfPresent(isoMessage, null, "inquiryRef");

        setValueIfPresent(isoMessage, request.getMessageId(), "messageId");
        setValueIfPresent(isoMessage, request.getEndToEndId(), "endToEndId");

        setEnumOrStringValue(isoMessage, "messageType", IsoMessageType.PACS_008);
        setEnumOrStringValue(isoMessage, "direction", IsoMessageDirection.INBOUND);
        setEnumOrStringValue(isoMessage, "validationStatus", IsoValidationStatus.VALID);
        setEnumOrStringValue(isoMessage, "securityStatus", IsoSecurityStatus.PLAIN);

        setValueIfPresent(
                isoMessage,
                rawXml,
                "messageXml",
                "rawXml",
                "xmlPayload",
                "payload",
                "messagePayload",
                "messageXmlContent",
                "xmlContent",
                "content",
                "isoXml"
        );

        setValueIfPresent(isoMessage, null, "errorCode");
        setValueIfPresent(isoMessage, null, "errorMessage");
        setValueIfPresent(isoMessage, now, "createdAt", "createdDate", "createdOn");
        setValueIfPresent(isoMessage, now, "updatedAt", "updatedDate", "updatedOn");

        isoMessageRepository.save(isoMessage);

        return new InboundPacs008PersistResult(transferRef);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "UNKNOWN";
    }

    private String safePart(String value) {
        if (value == null || value.isBlank()) {
            return "NA";
        }
        return value.trim();
    }

    private void setEnumOrStringValue(Object target, String propertyName, Enum<?> enumValue) {
        if (enumValue == null) {
            setValue(target, propertyName, null);
            return;
        }

        Class<?> propertyType = findPropertyType(target.getClass(), propertyName);

        if (propertyType == null) {
            return;
        }

        if (String.class.equals(propertyType)) {
            setValue(target, propertyName, enumValue.name());
            return;
        }

        if (propertyType.isEnum()) {
            setValue(target, propertyName, enumValue);
            return;
        }

        setValue(target, propertyName, enumValue.name());
    }

    private void setValueIfPresent(Object target, Object value, String... possiblePropertyNames) {
        for (String propertyName : possiblePropertyNames) {
            if (hasProperty(target.getClass(), propertyName)) {
                setValue(target, propertyName, value);
                return;
            }
        }
    }

    private boolean hasProperty(Class<?> targetClass, String propertyName) {
        return findSetter(targetClass, propertyName) != null || findField(targetClass, propertyName) != null;
    }

    private Class<?> findPropertyType(Class<?> targetClass, String propertyName) {
        Method setter = findSetter(targetClass, propertyName);
        if (setter != null) {
            return setter.getParameterTypes()[0];
        }

        Field field = findField(targetClass, propertyName);
        if (field != null) {
            return field.getType();
        }

        return null;
    }

    private void setValue(Object target, String propertyName, Object value) {
        Method setter = findSetter(target.getClass(), propertyName);

        if (setter != null) {
            try {
                setter.invoke(target, convertValue(value, setter.getParameterTypes()[0]));
                return;
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to call setter for " + target.getClass().getSimpleName() + "." + propertyName,
                        e
                );
            }
        }

        Field field = findField(target.getClass(), propertyName);

        if (field == null) {
            return;
        }

        try {
            field.setAccessible(true);
            field.set(target, convertValue(value, field.getType()));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to set field " + target.getClass().getSimpleName() + "." + propertyName,
                    e
            );
        }
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (String.class.equals(targetType) && value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        if (targetType.isEnum() && value instanceof String stringValue) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumObject = Enum.valueOf((Class<? extends Enum>) targetType, stringValue);
            return enumObject;
        }

        return value;
    }

    private Method findSetter(Class<?> targetClass, String propertyName) {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);

        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }

        return null;
    }

    private Field findField(Class<?> targetClass, String propertyName) {
        Class<?> current = targetClass;

        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(propertyName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}