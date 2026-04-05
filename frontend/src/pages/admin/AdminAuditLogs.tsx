import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { AppLayout } from "../AppLayout";
import api from "../../services/api";

type AuditStatus = "SUCCESS" | "FAILURE";

type AuditLogItem = {
  id: string;
  timestamp: string;
  userId: number | null;
  email: string | null;
  role: string | null;
  organizacaoId: number | null;
  acao: string;
  entidade: string | null;
  entidadeId: string | null;
  detalhesJson: unknown;
  ip: string | null;
  userAgent: string | null;
  status: AuditStatus;
  mensagemErro: string | null;
};

type PageResponse<T> = {
  content: T[];
  totalPages: number;
  number: number;
};

type Filters = {
  email: string;
  acao: string;
  entidade: string;
  status: string;
  startDate: string;
  endDate: string;
};

const initialFilters: Filters = {
  email: "",
  acao: "",
  entidade: "",
  status: "",
  startDate: "",
  endDate: "",
};

function formatDateTime(value: string | null | undefined) {
  if (!value) return "-";
  return new Date(value).toLocaleString("pt-BR");
}

function stringifyDetails(value: unknown) {
  if (!value) return "-";
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

export function AdminAuditLogs() {
  const [items, setItems] = useState<AuditLogItem[]>([]);
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0 });
  const [selectedLog, setSelectedLog] = useState<AuditLogItem | null>(null);

  const statusOptions = useMemo(() => ["", "SUCCESS", "FAILURE"], []);

  useEffect(() => {
    load(0);
  }, []);

  const buildParams = (page: number, activeFilters: Filters) => {
    const params = new URLSearchParams({
      page: String(page),
      size: "20",
      sortBy: "timestamp",
      sortDir: "desc",
    });

    if (activeFilters.email) params.set("email", activeFilters.email);
    if (activeFilters.acao) params.set("acao", activeFilters.acao);
    if (activeFilters.entidade) params.set("entidade", activeFilters.entidade);
    if (activeFilters.status) params.set("status", activeFilters.status);
    if (activeFilters.startDate) {
      params.set("startDate", new Date(activeFilters.startDate).toISOString());
    }
    if (activeFilters.endDate) {
      params.set("endDate", new Date(activeFilters.endDate).toISOString());
    }

    return params.toString();
  };

  const load = async (page: number, activeFilters: Filters = filters) => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get<PageResponse<AuditLogItem>>(
        `/api/admin/audit-log?${buildParams(page, activeFilters)}`
      );
      setItems(response.data.content ?? []);
      setPageInfo({
        page: response.data.number ?? page,
        totalPages: response.data.totalPages ?? 0,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar logs de auditoria");
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (field: keyof Filters, value: string) => {
    setFilters((prev) => ({ ...prev, [field]: value }));
  };

  const handleApplyFilters = () => {
    load(0);
  };

  const handleClearFilters = () => {
    setFilters(initialFilters);
    load(0, initialFilters);
  };

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Logs de Auditoria
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
            {error}
          </Alert>
        )}

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
              <TextField
                label="Email"
                value={filters.email}
                onChange={(e) => handleFilterChange("email", e.target.value)}
                sx={{ minWidth: 220 }}
              />
              <TextField
                label="Ação"
                value={filters.acao}
                onChange={(e) => handleFilterChange("acao", e.target.value)}
                helperText="Aceita múltiplas ações separadas por vírgula"
                sx={{ minWidth: 260 }}
              />
              <TextField
                label="Entidade"
                value={filters.entidade}
                onChange={(e) => handleFilterChange("entidade", e.target.value)}
                sx={{ minWidth: 180 }}
              />
              <TextField
                select
                label="Status"
                value={filters.status}
                onChange={(e) => handleFilterChange("status", e.target.value)}
                sx={{ minWidth: 160 }}
                SelectProps={{ native: true }}
              >
                {statusOptions.map((option) => (
                  <option key={option || "all"} value={option}>
                    {option || "Todos"}
                  </option>
                ))}
              </TextField>
              <TextField
                label="Data inicial"
                type="datetime-local"
                value={filters.startDate}
                onChange={(e) => handleFilterChange("startDate", e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                label="Data final"
                type="datetime-local"
                value={filters.endDate}
                onChange={(e) => handleFilterChange("endDate", e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Box>

            <Box sx={{ display: "flex", gap: 1, mt: 2 }}>
              <Button variant="contained" onClick={handleApplyFilters} disabled={loading}>
                Filtrar
              </Button>
              <Button variant="outlined" onClick={handleClearFilters} disabled={loading}>
                Limpar
              </Button>
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
                      <strong>Data/Hora</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Email</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Ação</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Entidade</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Status</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Detalhes</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        {loading ? "Carregando logs..." : "Nenhum log encontrado"}
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>{formatDateTime(item.timestamp)}</TableCell>
                        <TableCell>{item.email || "-"}</TableCell>
                        <TableCell>{item.acao}</TableCell>
                        <TableCell>{item.entidade || "-"}</TableCell>
                        <TableCell>
                          <Chip
                            label={item.status}
                            color={item.status === "SUCCESS" ? "success" : "error"}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Button size="small" onClick={() => setSelectedLog(item)}>
                            Ver detalhes
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            <Box sx={{ display: "flex", justifyContent: "center", mt: 2, gap: 1 }}>
              <Button
                variant="outlined"
                disabled={pageInfo.page <= 0 || loading}
                onClick={() => load(pageInfo.page - 1)}
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
                disabled={pageInfo.page + 1 >= pageInfo.totalPages || loading}
                onClick={() => load(pageInfo.page + 1)}
              >
                Próxima
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Dialog
          open={!!selectedLog}
          onClose={() => setSelectedLog(null)}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Detalhes do log</DialogTitle>
          <DialogContent dividers>
            {selectedLog && (
              <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                <Typography>
                  <strong>Data/Hora:</strong> {formatDateTime(selectedLog.timestamp)}
                </Typography>
                <Typography>
                  <strong>Email:</strong> {selectedLog.email || "-"}
                </Typography>
                <Typography>
                  <strong>Ação:</strong> {selectedLog.acao}
                </Typography>
                <Typography>
                  <strong>Entidade:</strong> {selectedLog.entidade || "-"}
                </Typography>
                <Typography>
                  <strong>Entidade ID:</strong> {selectedLog.entidadeId || "-"}
                </Typography>
                <Typography>
                  <strong>Status:</strong> {selectedLog.status}
                </Typography>
                <Typography>
                  <strong>IP:</strong> {selectedLog.ip || "-"}
                </Typography>
                <Typography>
                  <strong>User-Agent:</strong> {selectedLog.userAgent || "-"}
                </Typography>
                <Typography>
                  <strong>Mensagem de erro:</strong> {selectedLog.mensagemErro || "-"}
                </Typography>
                <Box>
                  <Typography sx={{ mb: 1 }}>
                    <strong>Detalhes JSON:</strong>
                  </Typography>
                  <Paper sx={{ p: 2, backgroundColor: "#f7f7f7" }}>
                    <pre style={{ margin: 0, whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                      {stringifyDetails(selectedLog.detalhesJson)}
                    </pre>
                  </Paper>
                </Box>
              </Box>
            )}
          </DialogContent>
        </Dialog>
      </Box>
    </AppLayout>
  );
}
