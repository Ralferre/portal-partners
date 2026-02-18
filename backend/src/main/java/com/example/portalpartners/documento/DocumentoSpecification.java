package com.example.portalpartners.documento;

import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public class DocumentoSpecification {
    public static Specification<Documento> contratadaNomeLike(String nome) {
        return (root, query, cb) -> {
            Join<Object, Object> contratadaJoin =
                    root.join("contratada", JoinType.LEFT);

            return cb.like(
                    cb.lower(contratadaJoin.get("nome")),
                    "%" + nome.toLowerCase() + "%"
            );

        };
    }

    public static Specification<Documento> funcionarioNomeLike(String nome) {
        return (root, query, cb) -> {
            Join<Documento, Funcionario> funcionarioJoin = root.join("funcionario", JoinType.LEFT);
            return cb.like(
                    cb.lower(funcionarioJoin.get("nomeCompleto")),
                    "%" + nome.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Documento> tipoEquals(TipoDocumento tipo) {
        return (root, query, cb) ->
                cb.equal(root.get("tipoDocumento"), tipo);
    }

    public static Specification<Documento> statusEquals(StatusDocumento status) {
        return (root, query, cb) ->
                cb.equal(root.get("statusDocumento"), status);
    }

    // 🔐 filtros de segurança

    public static Specification<Documento> porContratadaId(Long contratadaId) {
        return (root, query, cb) ->
                cb.equal(root.get("contratada").get("id"), contratadaId);
    }

    public static Specification<Documento> porContratanteId(Long contratanteId) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("contratada")
                                .get("contratante")
                                .get("id"),
                        contratanteId
                );
    }
}
