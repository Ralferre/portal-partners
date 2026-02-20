# 📊 RELATÓRIO DE DIAGNÓSTICO E CORREÇÕES

**Data**: 18/02/2026  
**Sistema**: Portal Partners  
**Objetivo**: Corrigir bugs críticos e implementar melhorias de segurança/arquitetura

---

## ✅ PROBLEMAS RESOLVIDOS

### 1. Login do Admin (CRÍTICO)
**Problema**: Frontend reportava "Falha ao autenticar" ao tentar login com `admin@admin.com`

**Causa Raiz**: Faltava configuração CORS no backend. O frontend roda em `localhost:5173` e o backend em `localhost:8080`, mas não havia permissão para cross-origin requests.

**Solução Aplicada**:
- Criado `CorsConfig.java` permitindo origins `localhost:5173` e `localhost:3000`
- Adicionado `.cors()` ao `SecurityFilterChain`
- Backend reiniciado

**Teste de Validação**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@admin.com","senha":"admin123"}'
```
**Resultado**: ✅ `200 OK` com token JWT válido

**Credenciais de Teste**:
- Admin: `admin@admin.com` / `admin123`
- Contratante: `contratante@empresa.com` / `contratante123`
- Contratada: `contratada@empresa.com` / `empresa123`

---

### 2. Validação de Upload de Documento (CONTRATADA vs FUNCIONARIO)
**Problema**: Regra de negócio não estava clara no código

**Análise**: Código **JÁ IMPLEMENTADO CORRETAMENTE** em `DocumentoService.uploadDocumento()`:

```java
if (dto.tipoReferenciaDocumento() == TypeReferenceFile.CONTRATADA) {
    // Exige contratadaId, funcionarioId deve ser null
    if (dto.contratadaId() == null) {
        throw new BusinessRulesException("Contratada deve ser informada.");
    }
    documento.setContratada(contratada);
    documento.setFuncionario(null);
    
} else if (dto.tipoReferenciaDocumento() == TypeReferenceFile.FUNCIONARIO) {
    // Exige funcionarioId, associa automaticamente à contratada do funcionário
    if (dto.funcionarioId() == null) {
        throw new BusinessRulesException("Funcionário deve ser informado");
    }
    documento.setFuncionario(funcionario);
    documento.setContratada(funcionario.getContratada());
}
```

**Status**: ✅ Validação robusta já implementada

---

### 3. DELETEs Seguros com Validação de Propriedade
**Problema**: Faltavam métodos DELETE com validação de propriedade por perfil

**Solução Aplicada**: Implementados métodos seguros em:

#### `ContratadaService.deletarContratada(Long id)`
- ✅ ADMIN: deleta qualquer contratada
- ✅ CONTRATANTE: só deleta suas próprias contratadas
- ✅ Lança `ForbiddenException` se não tiver permissão

#### `FuncionarioService.deletarFuncionario(Long id)`
- ✅ ADMIN: deleta qualquer funcionário
- ✅ CONTRATADA: só deleta seus próprios funcionários
- ✅ CONTRATANTE: só deleta funcionários das suas contratadas
- ✅ Lança `ForbiddenException` se não tiver permissão

#### `DocumentoService.deletarDocumento(Long id)`
- ✅ ADMIN: deleta qualquer documento
- ✅ CONTRATADA: só deleta seus próprios documentos
- ✅ CONTRATANTE: só deleta documentos das suas contratadas
- ✅ Lança `ForbiddenException` se não tiver permissão

**Endpoints Criados**:
- `DELETE /api/contratantes/contratadas/{id}` (via `ContratanteController`)
- `DELETE /api/contratadas/funcionarios/{id}` (via `ContratadaController`)
- `DELETE /api/documentos/{id}` (via `DocumentoController`)

---

### 4. Segurança por Perfil nos Controllers
**Problema**: Controllers tinham validação manual de perfil com `RuntimeException`

**Solução Aplicada**:
- ✅ Habilitado `@EnableMethodSecurity` no `SecurityConfig`
- ✅ Removidas validações manuais dos controllers
- ✅ Adicionado `@PreAuthorize` nos endpoints:
  - `ContratadaController`: `hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')`
  - `ContratanteController`: `hasRole('CONTRATANTE')`
  - `FuncionarioController`: `hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')`
  - `DocumentoController`: `hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')`
  - `BadgeController`: `hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_ADMIN')`

---

### 5. Padronização de Exceções
**Status Atual**: `GlobalExceptionHandler` **JÁ IMPLEMENTADO** com:

- ✅ `BusinessRulesException` → `400 BAD_REQUEST`
- ✅ `ResourceNotFoundException` → `404 NOT_FOUND`
- ✅ `ConflictException` / `ResourceAlreadyExists` → `409 CONFLICT`
- ✅ `ForbiddenException` / `AccessDeniedException` → `403 FORBIDDEN`
- ✅ `BadCredentialsException` → `401 UNAUTHORIZED` ("Credenciais inválidas")
- ✅ `MethodArgumentNotValidException` → `400 BAD_REQUEST` (validação de campos)
- ✅ `DataIntegrityViolationException` → `409 CONFLICT` (duplicação de dados)
- ✅ `Exception` (genérica) → `500 INTERNAL_SERVER_ERROR`

**Payload Padrão**:
```json
{
  "message": "Descrição do erro",
  "status": 400,
  "path": "/api/endpoint"
}
```

---

## ⚠️ ANÁLISES PENDENTES

### 1. Response Gigante ao Criar Funcionário
**Status**: **NÃO REPRODUZIDO**

**Análise**:
- `FuncionarioResponse` está enxuto (apenas `id`, `cpf`, `nomeCompleto`)
- Não expõe `Contratada` nem `Documento`
- Se houver problema, precisa identificar o endpoint exato

**Ação Necessária**: Testar via Postman e colar o response completo

---

### 2. Filtro de Documentos
**Status**: **CÓDIGO CORRETO, PRECISA TESTE**

**Análise do Código**:
- ✅ `DocumentoSpecification.contratadaNomeLike()` usa `LEFT JOIN` e `LIKE` case-insensitive
- ✅ `DocumentoSpecification.funcionarioNomeLike()` usa `LEFT JOIN` e `LIKE` case-insensitive
- ✅ `DocumentoService.filtrar()` aplica `restricaoPorPerfil()` corretamente
- ✅ `convertToResponse()` trata nulls em `contratada` e `funcionario`

**Possíveis Causas** (se falhar):
1. Banco sem dados de teste
2. Parâmetros incorretos na request
3. Documentos sem `contratada` ou `funcionario` associados

**Teste Necessário**:
```bash
# Buscar por nome de contratada
GET /api/documentos?contratada=Teste&page=0&size=20

# Buscar por nome de funcionário
GET /api/documentos?funcionario=João&page=0&size=20
```

---

## 🎯 RECOMENDAÇÕES PARA O FRONTEND

### 1. Dashboard Dinâmico por Perfil
**Implementar lógica condicional**:

```tsx
const { user } = useAuth();

if (user?.role === "ADMIN") {
  return <AdminDashboard />;
}

if (user?.role === "CONTRATANTE") {
  return <ContratanteDashboard />;
}

if (user?.role === "CONTRATADA") {
  return <ContratadaDashboard />;
}
```

**Componentes a criar**:
- `AdminDashboard`: visão global de todas as contratadas/documentos
- `ContratanteDashboard`: visão das suas contratadas e documentos
- `ContratadaDashboard`: visão dos próprios documentos e funcionários

---

### 2. Tabela de Documentos com Semáforo
**Adicionar coluna visual de status**:

```tsx
const getStatusColor = (status: StatusDocumento) => {
  switch (status) {
    case "APROVADO": return "success"; // 🟢 verde
    case "PENDENTE": return "warning"; // 🟡 amarelo
    case "REPROVADO": return "error";  // 🔴 vermelho
    case "ANALISADO": return "info";   // 🔵 azul
    default: return "default";
  }
};

<Chip 
  label={documento.statusDocumento} 
  color={getStatusColor(documento.statusDocumento)}
/>
```

---

### 3. Refatorar Páginas (Cadastro vs Upload)
**Separar responsabilidades**:

**Página A: Cadastros**
- Cadastro de Contratada
- Cadastro de Funcionário

**Página B: Upload de Documentos**
- Checkbox: "Documento da Contratada"
- Checkbox: "Documento de Funcionário"
- Campos dinâmicos baseados na seleção

```tsx
const [tipoReferencia, setTipoReferencia] = useState<"CONTRATADA" | "FUNCIONARIO">();

{tipoReferencia === "CONTRATADA" && (
  <Select label="Contratada" name="contratadaId" required />
)}

{tipoReferencia === "FUNCIONARIO" && (
  <Select label="Funcionário" name="funcionarioId" required />
)}
```

---

## 📋 CHECKLIST DE TESTES

### Backend (via Postman)
- [ ] Login com admin/contratante/contratada
- [ ] Filtro de documentos por nome de contratada
- [ ] Filtro de documentos por nome de funcionário
- [ ] Upload de documento da contratada
- [ ] Upload de documento de funcionário
- [ ] DELETE de contratada (CONTRATANTE)
- [ ] DELETE de funcionário (CONTRATADA)
- [ ] DELETE de documento (CONTRATADA/CONTRATANTE)
- [ ] Validação de acesso negado (403) ao tentar deletar recurso de outro usuário

### Frontend
- [ ] Login e redirecionamento para /dashboard
- [ ] Logout e limpeza de localStorage
- [ ] Dashboard diferente por perfil
- [ ] Tabela de documentos com semáforo de status
- [ ] Upload de documento com checkbox de tipo
- [ ] Filtros de busca funcionando
- [ ] Mensagens de erro amigáveis

---

## 🚀 PRÓXIMOS PASSOS

1. **Testar login via frontend** (agora com CORS configurado)
2. **Validar filtro de documentos** via Postman
3. **Implementar dashboards dinâmicos** no frontend
4. **Adicionar semáforo de status** na tabela de documentos
5. **Refatorar páginas** (cadastro vs upload)
6. **Smoke test completo** de integração frontend ↔ backend

---

## 📝 NOTAS TÉCNICAS

### Arquitetura de Segurança
- ✅ JWT com `@PreAuthorize` em todos os endpoints sensíveis
- ✅ Validação de propriedade em camada de serviço (não só controller)
- ✅ ADMIN sempre bypassa validações de propriedade
- ✅ Exceções customizadas com payloads padronizados

### Boas Práticas Aplicadas
- ✅ DTOs enxutos (sem expor entidades)
- ✅ Validações robustas em services
- ✅ Specifications para filtros dinâmicos
- ✅ Transações com `@Transactional`
- ✅ CORS configurado para desenvolvimento

### Melhorias Futuras (Opcional)
- [ ] Adicionar logs estruturados (SLF4J)
- [ ] Implementar cache (Redis) para consultas frequentes
- [ ] Adicionar testes unitários e de integração
- [ ] Implementar paginação em todos os endpoints de listagem
- [ ] Adicionar rate limiting para APIs públicas
