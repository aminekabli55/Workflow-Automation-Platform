package com.amine.credentialsservice.repository;


import com.amine.credentialsservice.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, String> {

    List<Credential> findByUserId(String userId);

    Optional<Credential> findByUserIdAndServiceTypeAndCredentialName(
            String userId,
            String serviceType,
            String credentialName
    );

    List<Credential> findByUserIdAndServiceType(String userId, String serviceType);

    void deleteByCredentialId(String credentialId);
}