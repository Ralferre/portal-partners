package com.example.portalpartners.audit;

import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.StatusAuditoria;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Aspecto de auditoria transversal.
 *
 * Intercepta todos os metodos anotados com @Auditavel e:
 * 1. Captura o contexto de seguranca (usuario, role, organizacaoId).
 * 2. Captura IP e User-Agent da requisicao HTTP corrente.
 * 3. Executa o metodo alvo.
 * 4. Em caso de sucesso: dispara evento assincrono com status SUCCESS.
 * 5. Em caso de excecao: dispara evento assincrono com status FAILURE
 *    e relanca a excecao original para nao alterar o comportamento do metodo.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final UsuarioRepository usuarioRepository;

    @Around("@annotation(auditavel)")
    public Object interceptar(ProceedingJoinPoint joinPoint, Auditavel auditavel) throws Throwable {

        // Captura contexto ANTES da execucao (SecurityContext pode ser limpo em erros)
        ContextoAuditoria contexto = capturarContexto(joinPoint, auditavel);

        try {
            Object resultado = joinPoint.proceed();
            String entidadeId = extrairEntidadeId(resultado);
            dispararEvento(contexto, auditavel, entidadeId, StatusAuditoria.SUCCESS, null);
            return resultado;

        } catch (Throwable ex) {
            dispararEvento(contexto, auditavel, null, StatusAuditoria.FAILURE, ex.getMessage());
            throw ex; // relanca sem alteracao
        }
    }

    // -------------------------------------------------------------------------

    private ContextoAuditoria capturarContexto(ProceedingJoinPoint joinPoint, Auditavel auditavel) {
        ContextoAuditoria ctx = new ContextoAuditoria();

        // Contexto de seguranca
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            ctx.email = auth.getName();
            Optional<Usuario> usuario = usuarioRepository.findByEmail(ctx.email);
            usuario.ifPresent(u -> {
                ctx.userId        = u.getId();
                ctx.role          = u.getRole() != null ? u.getRole().name() : null;
                ctx.organizacaoId = resolverOrganizacaoId(u);
            });
        } else {
            // Caso especial de login: o usuario ainda nao esta no SecurityContext
            // Tenta extrair o email do primeiro argumento se for um request de auth
            ctx.email = tentarExtrairEmailDosArgs(joinPoint.getArgs());
        }

        // Contexto HTTP
        HttpServletRequest request = obterRequestAtual();
        if (request != null) {
            ctx.ip        = extrairIp(request);
            ctx.userAgent = request.getHeader("User-Agent");
        }

        // Args opcionais
        if (auditavel.capturarArgs()) {
            ctx.detalhes = new HashMap<>();
            ctx.detalhes.put("args_count", joinPoint.getArgs().length);
        }

        return ctx;
    }

    private void dispararEvento(ContextoAuditoria ctx, Auditavel auditavel,
                                String entidadeId, StatusAuditoria status, String mensagemErro) {
        AuditEventDTO evento = AuditEventDTO.builder()
                .acao(auditavel.acao())
                .entidade(auditavel.entidade())
                .entidadeId(entidadeId)
                .email(ctx.email)
                .userId(ctx.userId)
                .role(ctx.role)
                .organizacaoId(ctx.organizacaoId)
                .ip(ctx.ip)
                .userAgent(ctx.userAgent)
                .status(status)
                .mensagemErro(mensagemErro)
                .detalhes(ctx.detalhes)
                .build();

        auditService.registrar(evento);
    }

    /**
     * Tenta extrair o ID da entidade retornada via reflexao.
     * Se o objeto retornado tiver um metodo getId() acessivel, usa o valor
     * como entidadeId no log sem precisar de configuracao adicional.
     */
    private String extrairEntidadeId(Object resultado) {
        if (resultado == null) return null;
        try {
            Method getId = resultado.getClass().getMethod("getId");
            Object id = getId.invoke(resultado);
            return id != null ? id.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long resolverOrganizacaoId(Usuario usuario) {
        if (usuario.getRole() == Role.CONTRATANTE && usuario.getContratante() != null) {
            return usuario.getContratante().getId();
        }
        if (usuario.getRole() == Role.CONTRATADA && usuario.getContratada() != null) {
            return usuario.getContratada().getId();
        }
        return null;
    }

    private String tentarExtrairEmailDosArgs(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                Method getEmail = arg.getClass().getMethod("getEmail");
                Object email = getEmail.invoke(arg);
                if (email instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
            // Tenta como record com campo email()
            try {
                Method email = arg.getClass().getMethod("email");
                Object val = email.invoke(arg);
                if (val instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private HttpServletRequest obterRequestAtual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class ContextoAuditoria {
        String email;
        Long userId;
        String role;
        Long organizacaoId;
        String ip;
        String userAgent;
        Map<String, Object> detalhes;
    }
}
