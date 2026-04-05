package com.example.portalpartners.controller;

import com.example.portalpartners.dto.LgpdConsentRequest;
import com.example.portalpartners.dto.LgpdConsentResponse;
import com.example.portalpartners.dto.LgpdTermoAtualResponse;
import com.example.portalpartners.service.LgpdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lgpd")
@RequiredArgsConstructor
public class LgpdController {

    private final LgpdService lgpdService;

    /**
     * Registra o consentimento LGPD do usuario autenticado.
     * Deve ser chamado pelo frontend antes de habilitar o fluxo de upload.
     */
    @PostMapping("/consentimento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LgpdConsentResponse> registrarConsentimento(
            @Valid @RequestBody LgpdConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(lgpdService.registrarConsentimento(request, httpRequest));
    }

    /**
     * Verifica se o usuario autenticado ja aceitou a versao corrente do termo.
     * O frontend chama este endpoint ao abrir a tela de upload.
     * Se valido=false, exibe o banner LGPD com checkbox obrigatorio.
     */
    @GetMapping("/consentimento/valido")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LgpdConsentResponse> verificarConsentimento() {
        return ResponseEntity.ok(lgpdService.verificarConsentimentoValido());
    }

    @GetMapping("/termo-atual")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LgpdTermoAtualResponse> obterTermoAtual() {
        return ResponseEntity.ok(lgpdService.obterTermoAtual());
    }
}
