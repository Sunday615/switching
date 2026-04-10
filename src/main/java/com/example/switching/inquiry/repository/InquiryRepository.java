package com.example.switching.inquiry.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.switching.inquiry.entity.InquiryEntity;

public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {

    Optional<InquiryEntity> findByInquiryRef(String inquiryRef);
}
