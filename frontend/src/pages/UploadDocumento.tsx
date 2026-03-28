import { useState, useEffect } from "react";
import {
  Checkbox,
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  TextField,
  MenuItem,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Alert,
  CircularProgress,
  Divider,
  Paper,
} from "@mui/material";
import { CloudUpload as UploadIcon } from "@mui/icons-material";
import { AppLayout } from "./AppLayout";
import api from "../services/api";

type TipoReferencia = "CONTRATADA" | "FUNCIONARIO";

type Funcionario = {
  id: number;
  nomeCompleto: string;
  cpf: string;
};

type LgpdTermoAtualResponse = {
  valido: boolean;
  versaoTermo: string;
  hashTermo: string;
  textoTermo: string;
  timestampConsentimento: string | null;
};

type SolicitarUploadResponse = {
  documentoId: number;
  objectKey: string;
  uploadUrl: string;
  expiresInSeconds: number;
};

export function UploadDocumento() {
  const [loading, setLoading] = useState(false);
  const [loadingConsent, setLoadingConsent] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [tiposDocumento, setTiposDocumento] = useState<string[]>([]);
  const [lgpd, setLgpd] = useState<LgpdTermoAtualResponse | null>(null);
  const [consentChecked, setConsentChecked] = useState(false);

  const [tipoReferencia, setTipoReferencia] = useState<TipoReferencia>("CONTRATADA");
  const [tipoDocumento, setTipoDocumento] = useState<string>("");
  const [funcionarioId, setFuncionarioId] = useState<number | "">("");
  const [arquivo, setArquivo] = useState<File | null>(null);

  useEffect(() => {
    loadFuncionarios();
    loadTiposDocumento();
    loadLgpdTermo();
  }, []);

  const loadFuncionarios = async () => {
    try {
      const response = await api.get("/api/contratadas/funcionarios");
      setFuncionarios(response.data.content || response.data);
    } catch (err: any) {
      console.error("Erro ao carregar funcionários:", err);
    }
  };

  const loadTiposDocumento = async () => {
    try {
      const response = await api.get<string[]>("/api/documentos/tipos");
      const tipos = response.data ?? [];
      setTiposDocumento(tipos);
      setTipoDocumento((prev) => prev || tipos[0] || "");
    } catch (err: any) {
      console.error("Erro ao carregar tipos de documento:", err);
    }
  };

  const loadLgpdTermo = async () => {
    try {
      const response = await api.get<LgpdTermoAtualResponse>("/api/lgpd/termo-atual");
      setLgpd(response.data);
    } catch (err: any) {
      console.error("Erro ao carregar termo LGPD:", err);
    }
  };

  const requiresLgpd = tipoReferencia === "FUNCIONARIO";
  const isLgpdApproved = !requiresLgpd || !!lgpd?.valido;

  const handleRegistrarConsentimento = async () => {
    if (!lgpd) return;
    if (!consentChecked) {
      setError("Marque o aceite do termo LGPD antes de continuar.");
      return;
    }

    setLoadingConsent(true);
    setError("");
    try {
      await api.post("/api/lgpd/consentimento", {
        versaoTermo: lgpd.versaoTermo,
        hashTermo: lgpd.hashTermo,
      });
      setSuccess("Consentimento LGPD registrado com sucesso.");
      setConsentChecked(false);
      await loadLgpdTermo();
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao registrar consentimento LGPD");
    } finally {
      setLoadingConsent(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");

    if (!arquivo) {
      setError("Selecione um arquivo");
      setLoading(false);
      return;
    }

    if (!tipoDocumento) {
      setError("Selecione o tipo do documento");
      setLoading(false);
      return;
    }

    if (tipoReferencia === "FUNCIONARIO" && !funcionarioId) {
      setError("Selecione um funcionário");
      setLoading(false);
      return;
    }

    if (!isLgpdApproved) {
      setError("Registre o consentimento LGPD antes de enviar documentos pessoais.");
      setLoading(false);
      return;
    }

    try {
      const solicitarUploadPayload = {
        nomeArquivo: arquivo.name,
        contentType: arquivo.type || "application/octet-stream",
        tamanhoBytes: arquivo.size,
        tipoDocumento,
        tipoReferencia,
        funcionarioId: tipoReferencia === "FUNCIONARIO" ? Number(funcionarioId) : null,
        contratadaId: null,
      };

      const solicitarUploadResponse = await api.post<SolicitarUploadResponse>(
        "/api/documentos/solicitar-upload",
        solicitarUploadPayload
      );

      const { documentoId, uploadUrl } = solicitarUploadResponse.data;

      const putResponse = await fetch(uploadUrl, {
        method: "PUT",
        headers: {
          "Content-Type": arquivo.type || "application/octet-stream",
        },
        body: arquivo,
      });

      if (!putResponse.ok) {
        throw new Error(`Falha no upload direto ao storage (HTTP ${putResponse.status})`);
      }

      await api.post(`/api/documentos/${documentoId}/confirmar-upload`);

      setSuccess("Documento enviado com sucesso!");
      setArquivo(null);
      setFuncionarioId("");
      
      const fileInput = document.getElementById("file-input") as HTMLInputElement;
      if (fileInput) fileInput.value = "";
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || "Erro ao enviar documento");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Upload de Documento
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert
            severity="success"
            sx={{ mb: 2 }}
            onClose={() => setSuccess("")}
          >
            {success}
          </Alert>
        )}

        <Card>
          <CardContent>
            <Box component="form" onSubmit={handleSubmit} sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
              <FormControl component="fieldset">
                <FormLabel component="legend">Tipo de Documento</FormLabel>
                <RadioGroup
                  row
                  value={tipoReferencia}
                  onChange={(e) => {
                    setTipoReferencia(e.target.value as TipoReferencia);
                    setFuncionarioId("");
                  }}
                >
                  <FormControlLabel
                    value="CONTRATADA"
                    control={<Radio />}
                    label="Documento da Contratada"
                  />
                  <FormControlLabel
                    value="FUNCIONARIO"
                    control={<Radio />}
                    label="Documento de Funcionário"
                  />
                </RadioGroup>
              </FormControl>

              {tipoReferencia === "FUNCIONARIO" && (
                <TextField
                  select
                  label="Selecione o Funcionário"
                  fullWidth
                  required
                  value={funcionarioId}
                  onChange={(e) => setFuncionarioId(Number(e.target.value))}
                >
                  <MenuItem value="">Selecione...</MenuItem>
                  {funcionarios.map((func) => (
                    <MenuItem key={func.id} value={func.id}>
                      {func.nomeCompleto} - {func.cpf}
                    </MenuItem>
                  ))}
                </TextField>
              )}

              {requiresLgpd && lgpd && !lgpd.valido && (
                <Card variant="outlined" sx={{ backgroundColor: "#fffdf6" }}>
                  <CardContent>
                    <Typography variant="h6" sx={{ mb: 1 }}>
                      Consentimento LGPD obrigatório
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      Para enviar documentos pessoais de funcionários, é necessário registrar o
                      aceite do termo vigente.
                    </Typography>
                    <Typography variant="caption" display="block" sx={{ mb: 2 }}>
                      Versão do termo: {lgpd.versaoTermo}
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    <Paper
                      variant="outlined"
                      sx={{ p: 2, maxHeight: 220, overflow: "auto", backgroundColor: "#fafafa" }}
                    >
                      <Typography variant="body2" sx={{ whiteSpace: "pre-wrap" }}>
                        {lgpd.textoTermo}
                      </Typography>
                    </Paper>
                    <FormControlLabel
                      sx={{ mt: 2 }}
                      control={
                        <Checkbox
                          checked={consentChecked}
                          onChange={(e) => setConsentChecked(e.target.checked)}
                        />
                      }
                      label="Li e concordo com o tratamento dos dados pessoais conforme o termo acima."
                    />
                    <Box sx={{ mt: 1 }}>
                      <Button
                        variant="contained"
                        onClick={handleRegistrarConsentimento}
                        disabled={loadingConsent || !consentChecked}
                      >
                        {loadingConsent ? "Registrando aceite..." : "Registrar aceite LGPD"}
                      </Button>
                    </Box>
                  </CardContent>
                </Card>
              )}

              {requiresLgpd && lgpd?.valido && (
                <Alert severity="success">
                  Consentimento LGPD válido registrado em{" "}
                  {lgpd.timestampConsentimento
                    ? new Date(lgpd.timestampConsentimento).toLocaleString("pt-BR")
                    : "data indisponível"}
                  .
                </Alert>
              )}

              <TextField
                select
                label="Tipo do Documento"
                fullWidth
                required
                value={tipoDocumento}
                onChange={(e) => setTipoDocumento(e.target.value)}
              >
                {tiposDocumento.map((tipo) => (
                  <MenuItem key={tipo} value={tipo}>
                    {tipo}
                  </MenuItem>
                ))}
              </TextField>

              <Box>
                <input
                  id="file-input"
                  type="file"
                  accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                  onChange={(e) => setArquivo(e.target.files?.[0] || null)}
                  style={{ display: "none" }}
                />
                <label htmlFor="file-input">
                  <Button
                    variant="outlined"
                    component="span"
                    startIcon={<UploadIcon />}
                    fullWidth
                  >
                    {arquivo ? arquivo.name : "Selecionar Arquivo"}
                  </Button>
                </label>
                <Typography variant="caption" color="textSecondary" sx={{ mt: 1, display: "block" }}>
                  Formatos aceitos: PDF, JPG, PNG, DOC, DOCX (máx. 10MB)
                </Typography>
              </Box>

              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading || !isLgpdApproved}
                sx={{ backgroundColor: "#1b6c72ff" }}
              >
                {loading ? (
                  <>
                    <CircularProgress size={20} sx={{ mr: 1 }} />
                    Enviando...
                  </>
                ) : (
                  "Enviar Documento"
                )}
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </AppLayout>
  );
}
