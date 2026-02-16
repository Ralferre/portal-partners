package com.example.portalpartners.service;

import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Usuario;

public interface UsuarioLogadoService {
    Usuario getUsuario();

    Contratante getContratanteLogada();

    Contratada getContratadaLogada();

    boolean isAdmin();
}
