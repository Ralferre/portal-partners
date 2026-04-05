package com.example.portalpartners.repository;

import com.example.portalpartners.model.LgpdConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LgpdConsentRepository extends JpaRepository<LgpdConsent, UUID> {

    /**
     * Retorna o registro de consentimento mais recente de um usuario para
     * uma versao especifica do termo. Usado para validar se o usuario ja
     * aceitou o termo antes de permitir upload de documentos.
     */
    Optional<LgpdConsent> findTopByUserIdAndVersaoTermoOrderByTimestampDesc(
            Long userId,
            String versaoTermo
    );
}
