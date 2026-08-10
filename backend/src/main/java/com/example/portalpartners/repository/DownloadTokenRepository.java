package com.example.portalpartners.repository;

import com.example.portalpartners.model.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DownloadTokenRepository extends JpaRepository<DownloadToken, Long> {

    Optional<DownloadToken> findByToken(String token);

    /**
     * Consome o token de forma atomica.
     *
     * A condicao `consumidoEm is null` faz parte do UPDATE, e nao de uma
     * verificacao previa em memoria: duas requisicoes simultaneas com o mesmo
     * token disputam a mesma linha e apenas uma recebe 1 como retorno. Sem
     * isso, o "uso unico" seria vulneravel a corrida.
     */
    @Modifying
    @Query("update DownloadToken t set t.consumidoEm = :agora, t.ipConsumo = :ip "
            + "where t.token = :token and t.consumidoEm is null and t.expiraEm > :agora")
    int consumir(@Param("token") String token,
                 @Param("agora") LocalDateTime agora,
                 @Param("ip") String ip);

    @Modifying
    @Query("delete from DownloadToken t where t.expiraEm < :limite")
    int removerExpirados(@Param("limite") LocalDateTime limite);
}
