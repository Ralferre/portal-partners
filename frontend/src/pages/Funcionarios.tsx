import { useState, useEffect } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Alert,
} from "@mui/material";
import { Add as AddIcon, Delete as DeleteIcon } from "@mui/icons-material";
import { AppLayout } from "./AppLayout";
import api from "../services/api";
import { formatCpf, isValidCpf, onlyDigits } from "../utils/validators";

type Funcionario = {
  id: number;
  cpf: string;
  nomeCompleto: string;
};

type FuncionariosPage = {
  content: Funcionario[];
  totalPages: number;
  number: number;
};

export function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number }>({
    page: 0,
    totalPages: 0,
  });
  const [openDialog, setOpenDialog] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [cpfTouched, setCpfTouched] = useState(false);

  const [formData, setFormData] = useState({
    cpf: "",
    nomeCompleto: "",
  });

  useEffect(() => {
    loadFuncionarios(0);
  }, []);

  const loadFuncionarios = async (page: number) => {
    try {
      const response = await api.get<FuncionariosPage>(
        `/api/contratadas/funcionarios/paged?page=${page}&size=10`
      );
      setFuncionarios(response.data.content ?? []);
      setPageInfo({ page: response.data.number ?? page, totalPages: response.data.totalPages ?? 0 });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar funcionários");
    }
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const cpfDigits = onlyDigits(formData.cpf);
      if (!isValidCpf(cpfDigits)) {
        setError("CPF inválido");
        return;
      }

      await api.post("/api/contratadas/funcionarios", {
        ...formData,
        cpf: cpfDigits,
      });
      setSuccess("Funcionário cadastrado com sucesso!");
      setOpenDialog(false);
      setFormData({
        cpf: "",
        nomeCompleto: "",
      });
      setCpfTouched(false);
      loadFuncionarios(0);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao cadastrar funcionário");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Deseja realmente excluir este funcionário?")) return;

    try {
      await api.delete(`/api/contratadas/funcionarios/${id}`);
      setSuccess("Funcionário excluído com sucesso!");
      loadFuncionarios(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao excluir funcionário");
    }
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
            Funcionários
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
            sx={{ backgroundColor: "#1b6c72ff" }}
          >
            Novo Funcionário
          </Button>
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

        <Card>
          <CardContent>
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>
                      <strong>Nome Completo</strong>
                    </TableCell>
                    <TableCell>
                      <strong>CPF</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Ações</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {funcionarios.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={3} align="center">
                        Nenhum funcionário cadastrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    funcionarios.map((funcionario) => (
                      <TableRow key={funcionario.id}>
                        <TableCell>{funcionario.nomeCompleto}</TableCell>
                        <TableCell>{formatCpf(funcionario.cpf)}</TableCell>
                        <TableCell align="center">
                          <IconButton
                            color="error"
                            onClick={() => handleDelete(funcionario.id)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Novo Funcionário</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome Completo"
                fullWidth
                required
                value={formData.nomeCompleto}
                onChange={(e) =>
                  setFormData({ ...formData, nomeCompleto: e.target.value })
                }
              />
              <TextField
                label="CPF"
                fullWidth
                required
                value={formData.cpf}
                onChange={(e) =>
                  setFormData({ ...formData, cpf: formatCpf(e.target.value) })
                }
                onBlur={() => setCpfTouched(true)}
                error={cpfTouched && !!formData.cpf && !isValidCpf(formData.cpf)}
                helperText={
                  cpfTouched && !!formData.cpf && !isValidCpf(formData.cpf)
                    ? "CPF inválido"
                    : ""
                }
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancelar</Button>
            <Button
              onClick={handleSubmit}
              variant="contained"
              disabled={loading}
              sx={{ backgroundColor: "#1b6c72ff" }}
            >
              {loading ? "Salvando..." : "Salvar"}
            </Button>
          </DialogActions>
        </Dialog>

        <Box sx={{ display: "flex", justifyContent: "center", mt: 2, gap: 1 }}>
          <Button
            variant="outlined"
            disabled={pageInfo.page <= 0}
            onClick={() => loadFuncionarios(pageInfo.page - 1)}
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
            onClick={() => loadFuncionarios(pageInfo.page + 1)}
          >
            Próxima
          </Button>
        </Box>
      </Box>
    </AppLayout>
  );
}
