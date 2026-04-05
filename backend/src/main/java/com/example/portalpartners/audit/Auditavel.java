package com.example.portalpartners.audit;

import java.lang.annotation.*;

/**
 * Marca um metodo para geracao automatica de evento no audit_log.
 *
 * O AuditAspect intercepta o metodo anotado, captura contexto de seguranca
 * (usuario, IP, User-Agent) e persiste o evento de forma assincrona.
 *
 * capturarArgs = false por padrao: evita que argumentos do metodo
 * (que podem conter senhas, CPF, dados sensiveis) sejam serializados
 * automaticamente no detalhesJson.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Auditavel {

    /** Acao descritiva do evento (ex: "LOGIN_SUCESSO", "UPLOAD_CONCLUIDO"). */
    String acao();

    /** Nome da entidade alvo (ex: "Usuario", "Documento", "Contratante"). */
    String entidade() default "";

    /**
     * Se true, serializa os argumentos do metodo no detalhesJson.
     * Manter false para metodos que recebem dados pessoais ou senhas.
     */
    boolean capturarArgs() default false;
}
