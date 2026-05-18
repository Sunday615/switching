package com.example.switching.security.oauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.switching.security.oauth.entity.OAuthClientEntity;

@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClientEntity, String> {
}
