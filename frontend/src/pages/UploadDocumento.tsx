import { useState, useEffect } from "react";
import {
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

export function UploadDocumento() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [tiposDocumento, setTiposDocumento] = useState<string[]>([]);

  const [tipoReferencia, setTipoReferencia] = useState<TipoReferencia>("CONTRATADA");
  const [tipoDocumento, setTipoDocumento] = useState<string>("");
  const [funcionarioId, setFuncionarioId] = useState<number | "">("");
  const [arquivo, setArquivo] = useState<File | null>(null);

  useEffect(() => {
    loadFuncionarios();
    loadTiposDocumento();
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

    try {
      const formData = new FormData();
      formData.append("arquivo", arquivo);
      formData.append("tipoDocumento", tipoDocumento);
      formData.append("tipoReferenciaDocumento", tipoReferencia);

      if (tipoReferencia === "FUNCIONARIO" && funcionarioId) {
        formData.append("funcionarioId", funcionarioId.toString());
      }

      await api.post("/api/documentos/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      setSuccess("Documento enviado com sucesso!");
      setArquivo(null);
      setFuncionarioId("");
      
      const fileInput = document.getElementById("file-input") as HTMLInputElement;
      if (fileInput) fileInput.value = "";
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao enviar documento");
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
                disabled={loading}
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
