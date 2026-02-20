import { useState, useEffect } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  TextField,
  MenuItem,
  Alert,
  IconButton,
  Tooltip,
} from "@mui/material";
import {
  CancelOutlined as ReprovadoIcon,
  CheckCircleOutline as AprovadoIcon,
  Delete as DeleteIcon,
  HourglassEmpty as PendenteIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { AppLayout } from "./AppLayout";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

type StatusDocumento = "PENDENTE" | "APROVADO" | "REPROVADO";
type TipoDocumento = string;

type Documento = {
  id: number;
  tipoDocumento: TipoDocumento;
  nomeArquivo: string;
  statusDocumento: StatusDocumento;
  dataPostagem: string;
  dataStatusAtualizado?: string;
  contratadaNome?: string;
  funcionarioNome?: string;
};

type DocumentosPage = {
  content: Documento[];
  totalPages: number;
  number: number;
};

export function Documentos() {
  const { user } = useAuth();
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [tiposDocumento, setTiposDocumento] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number; size: number }>({
    page: 0,
    totalPages: 0,
    size: 20,
  });

  const [filtros, setFiltros] = useState({
    contratada: "",
    funcionario: "",
    tipo: "",
    status: "",
  });

  useEffect(() => {
    loadDocumentos(0);
    loadTiposDocumento();
  }, []);

  useEffect(() => {
    // Recarrega ao mudar filtros
    loadDocumentos(0);
  }, [filtros.contratada, filtros.funcionario, filtros.tipo, filtros.status]);

  const loadDocumentos = async (page: number) => {
    setLoading(true);
    setError("");

    try {
      const params = new URLSearchParams();
      if (filtros.contratada) params.append("contratada", filtros.contratada);
      if (filtros.funcionario) params.append("funcionario", filtros.funcionario);
      if (filtros.tipo) params.append("tipo", filtros.tipo);
      if (filtros.status) params.append("status", filtros.status);
      params.append("page", String(page));
      params.append("size", String(pageInfo.size));

      const response = await api.get<DocumentosPage>(`/api/documentos?${params.toString()}`);
      setDocumentos(response.data.content ?? []);
      setPageInfo((prev) => ({
        ...prev,
        page: response.data.number ?? page,
        totalPages: response.data.totalPages ?? 0,
      }));
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar documentos");
    } finally {
      setLoading(false);
    }
  };

  const loadTiposDocumento = async () => {
    try {
      const response = await api.get<string[]>("/api/documentos/tipos");
      setTiposDocumento(response.data ?? []);
    } catch (err: any) {
      console.error("Erro ao carregar tipos de documento:", err);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Deseja realmente excluir este documento?")) return;

    try {
      await api.delete(`/api/documentos/${id}`);
      setSuccess("Documento excluído com sucesso!");
      loadDocumentos(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao excluir documento");
    }
  };

  const handleDownload = async (doc: Documento) => {
    setError("");
    try {
      const response = await api.get(`/api/documentos/${doc.id}/download`, {
        responseType: "blob",
      });

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = doc.nomeArquivo || `documento-${doc.id}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);

      // atualiza lista para refletir mudança de "novo" (download pela contratante)
      loadDocumentos(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao baixar documento");
    }
  };

  const handleUpdateStatus = async (docId: number, novoStatus: StatusDocumento) => {
    setError("");
    setSuccess("");
    try {
      await api.put(`/api/documentos/status/${docId}`, { statusDocumento: novoStatus });
      setSuccess("Status atualizado com sucesso!");
      loadDocumentos(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao atualizar status");
    }
  };

  const getStatusColor = (status: StatusDocumento) => {
    switch (status) {
      case "APROVADO":
        return "success";
      case "PENDENTE":
        return "warning";
      case "REPROVADO":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusIcon = (status: StatusDocumento) => {
    switch (status) {
      case "APROVADO":
        return <AprovadoIcon fontSize="small" color="success" />;
      case "PENDENTE":
        return <PendenteIcon fontSize="small" color="warning" />;
      case "REPROVADO":
        return <ReprovadoIcon fontSize="small" color="error" />;
      default:
        return null;
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return "-";

    const hasTimezone = /[zZ]$/.test(dateString) || /[+-]\d{2}:?\d{2}$/.test(dateString);
    const normalized = hasTimezone ? dateString : `${dateString}Z`;

    return new Date(normalized).toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <AppLayout>
      <Box>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Typography variant="h4" fontWeight="bold">
            Documentos
          </Typography>
          <Tooltip title="Atualizar">
            <IconButton onClick={() => loadDocumentos(pageInfo.page)} color="primary">
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        </Box>

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

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Filtros
            </Typography>
            <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
              <Box sx={{ flex: "1 1 200px" }}>
                <TextField
                  label="Nome da Contratada"
                  fullWidth
                  size="small"
                  value={filtros.contratada}
                  onChange={(e) =>
                    setFiltros({ ...filtros, contratada: e.target.value })
                  }
                  onKeyPress={(e) => e.key === "Enter" && loadDocumentos(0)}
                />
              </Box>
              <Box sx={{ flex: "1 1 200px" }}>
                <TextField
                  label="Nome do Funcionário"
                  fullWidth
                  size="small"
                  value={filtros.funcionario}
                  onChange={(e) =>
                    setFiltros({ ...filtros, funcionario: e.target.value })
                  }
                  onKeyPress={(e) => e.key === "Enter" && loadDocumentos(0)}
                />
              </Box>
              <Box sx={{ flex: "1 1 200px" }}>
                <TextField
                  select
                  label="Tipo"
                  fullWidth
                  size="small"
                  value={filtros.tipo}
                  onChange={(e) =>
                    setFiltros({ ...filtros, tipo: e.target.value })
                  }
                >
                  <MenuItem value="">Todos</MenuItem>
                  {tiposDocumento.map((t) => (
                    <MenuItem key={t} value={t}>
                      {t}
                    </MenuItem>
                  ))}
                </TextField>
              </Box>
              <Box sx={{ flex: "1 1 200px" }}>
                <TextField
                  select
                  label="Status"
                  fullWidth
                  size="small"
                  value={filtros.status}
                  onChange={(e) =>
                    setFiltros({ ...filtros, status: e.target.value })
                  }
                >
                  <MenuItem value="">Todos</MenuItem>
                  <MenuItem value="PENDENTE">Pendente</MenuItem>
                  <MenuItem value="APROVADO">Aprovado</MenuItem>
                  <MenuItem value="REPROVADO">Reprovado</MenuItem>
                </TextField>
              </Box>
            </Box>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>
                      <strong>Status</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Tipo</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Arquivo</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Contratada</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Funcionário</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Data</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Atualizado em</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Download</strong>
                    </TableCell>
                    {(user?.role === "CONTRATANTE" || user?.role === "ADMIN") && (
                      <TableCell align="center">
                        <strong>Status</strong>
                      </TableCell>
                    )}
                    {(user?.role === "CONTRATADA" || user?.role === "ADMIN") && (
                      <TableCell align="center">
                        <strong>Ações</strong>
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        Carregando...
                      </TableCell>
                    </TableRow>
                  ) : documentos.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        Nenhum documento encontrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    documentos.map((doc) => (
                      <TableRow
                        key={doc.id}
                        hover
                        sx={{ cursor: "pointer" }}
                        onClick={() => handleDownload(doc)}
                      >
                        <TableCell>
                          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                            {getStatusIcon(doc.statusDocumento)}
                            <Chip
                              label={doc.statusDocumento}
                              color={getStatusColor(doc.statusDocumento)}
                              size="small"
                            />
                          </Box>
                        </TableCell>
                        <TableCell>{doc.tipoDocumento}</TableCell>
                        <TableCell>{doc.nomeArquivo}</TableCell>
                        <TableCell>{doc.contratadaNome || "-"}</TableCell>
                        <TableCell>{doc.funcionarioNome || "-"}</TableCell>
                        <TableCell>{formatDate(doc.dataPostagem)}</TableCell>
                        <TableCell>
                          {doc.dataStatusAtualizado ? formatDate(doc.dataStatusAtualizado) : "-"}
                        </TableCell>
                        <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                          <Button size="small" variant="outlined" onClick={() => handleDownload(doc)}>
                            Baixar
                          </Button>
                        </TableCell>
                        {(user?.role === "CONTRATANTE" || user?.role === "ADMIN") && (
                          <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                            <TextField
                              select
                              size="small"
                              value={doc.statusDocumento}
                              onChange={(e) =>
                                handleUpdateStatus(doc.id, e.target.value as StatusDocumento)
                              }
                              sx={{ minWidth: 160 }}
                            >
                              <MenuItem value="PENDENTE">Pendente</MenuItem>
                              <MenuItem value="APROVADO">Aprovado</MenuItem>
                              <MenuItem value="REPROVADO">Reprovado</MenuItem>
                            </TextField>
                          </TableCell>
                        )}
                        {(user?.role === "CONTRATADA" || user?.role === "ADMIN") && (
                          <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                            <IconButton
                              color="error"
                              size="small"
                              onClick={() => handleDelete(doc.id)}
                            >
                              <DeleteIcon />
                            </IconButton>
                          </TableCell>
                        )}
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>
      </Box>

      <Box sx={{ display: "flex", justifyContent: "center", mt: 2, gap: 1 }}>
        <Button
          variant="outlined"
          disabled={pageInfo.page <= 0}
          onClick={() => loadDocumentos(pageInfo.page - 1)}
        >
          Anterior
        </Button>
        <Box sx={{ display: "flex", alignItems: "center" }}>
          <Typography variant="body2">
            Página {pageInfo.page + 1} de {Math.max(pageInfo.totalPages, 1)}
          </Typography>
        </Box>
        <Button
          variant="outlined"
          disabled={pageInfo.page + 1 >= pageInfo.totalPages}
          onClick={() => loadDocumentos(pageInfo.page + 1)}
        >
          Próxima
        </Button>
      </Box>
    </AppLayout>
  );
}
