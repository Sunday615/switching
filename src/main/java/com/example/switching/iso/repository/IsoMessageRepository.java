package com.example.switching.iso.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.switching.iso.entity.IsoMessageEntity;
import com.example.switching.iso.enums.IsoMessageDirection;
import com.example.switching.iso.enums.IsoMessageType;

public interface IsoMessageRepository extends JpaRepository<IsoMessageEntity, Long> {

    @Query("""
           select m
             from IsoMessageEntity m
            where (:messageType is null or m.messageType = :messageType)
              and (:direction is null or m.direction = :direction)
              and (:correlationRef is null or m.correlationRef = :correlationRef)
              and (:inquiryRef is null or m.inquiryRef = :inquiryRef)
              and (:transferRef is null or m.transferRef = :transferRef)
              and (:endToEndId is null or m.endToEndId = :endToEndId)
            order by m.id desc
           """)
    List<IsoMessageEntity> searchIsoMessages(
            @Param("messageType") IsoMessageType messageType,
            @Param("direction") IsoMessageDirection direction,
            @Param("correlationRef") String correlationRef,
            @Param("inquiryRef") String inquiryRef,
            @Param("transferRef") String transferRef,
            @Param("endToEndId") String endToEndId,
            Pageable pageable
    );
}