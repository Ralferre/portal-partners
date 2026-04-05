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
  InputAdornment,
} from "@mui/material";
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  VisibilityOffOutlined,
  VisibilityOutlined,
} from "@mui/icons-material";
import { AppLayout } from "./AppLayout";
import api from "../services/api";
import { formatCnpj, isValidCnpj, onlyDigits } from "../utils/validators";

type Contratada = {
  id: number;
  nome: string;
  cnpj: string;
  numeroContrato: string;
  numeroPedido: string;
  email: string;
};

type ContratadasPage = {
  content: Contratada[];
  totalPages: number;
  number: number;
};

export function Contratadas() {
  const [contratadas, setContratadas] = useState<Contratada[]>([]);
  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number }>({
    page: 0,
    totalPages: 0,
  });
  const [openDialog, setOpenDialog] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [cnpjTouched, setCnpjTouched] = useState(false);

  const [openEdit, setOpenEdit] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editNome, setEditNome] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editSenha, setEditSenha] = useState("");
  const [editCnpj, setEditCnpj] = useState("");
  const [editNumeroContrato, setEditNumeroContrato] = useState("");
  const [editNumeroPedido, setEditNumeroPedido] = useState("");
  const [showEditSenha, setShowEditSenha] = useState(false);
  const [editing, setEditing] = useState(false);
  const [showCreateSenha, setShowCreateSenha] = useState(false);

  const [formData, setFormData] = useState({
    nome: "",
    cnpj: "",
    numeroContrato: "",
    numeroPedido: "",
    email: "",
    senha: "",
  });

  useEffect(() => {
    loadContratadas(0);
  }, []);

  const loadContratadas = async (page: number) => {
    try {
      const response = await api.get<ContratadasPage>(
        `/api/contratantes/contratadas?page=${page}`
      );
      setContratadas(response.data.content ?? []);
      setPageInfo({ page: response.data.number ?? page, totalPages: response.data.totalPages ?? 0 });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar contratadas");
    }
  };

  const handleOpenEdit = (c: Contratada) => {
    setError("");
    setSuccess("");
    setEditId(c.id);
    setEditNome(c.nome ?? "");
    setEditEmail(c.email ?? "");
    setEditSenha("");
    setShowEditSenha(false);
    setEditCnpj(c.cnpj ?? "");
    setEditNumeroContrato(c.numeroContrato ?? "");
    setEditNumeroPedido(c.numeroPedido ?? "");
    setOpenEdit(true);
  };

  const handleEdit = async () => {
    setError("");
    setSuccess("");

    if (editId == null) {
      setError("Contratada inválida");
      return;
    }

    const nome = editNome.trim();
    const email = editEmail.trim();
    const cnpjDigits = onlyDigits(editCnpj);
    const numeroContrato = editNumeroContrato.trim();
    const numeroPedido = editNumeroPedido.trim();
    const senha = editSenha;

    if (!nome || !email || !cnpjDigits || !numeroContrato || !numeroPedido) {
      setError("Preencha nome, email, cnpj, nº contrato e nº pedido");
      return;
    }

    if (!isValidCnpj(cnpjDigits)) {
      setError("CNPJ inválido");
      return;
    }

    if (senha && senha.length > 0 && senha.length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres");
      return;
    }

    setEditing(true);
    try {
      await api.put(`/api/contratantes/contratadas/${editId}`, {
        nome,
        email,
        senha: senha?.trim() ? senha : null,
        cnpj: cnpjDigits,
        numeroContrato,
        numeroPedido,
      });

      setOpenEdit(false);
      setSuccess("Contratada atualizada com sucesso!");
      await loadContratadas(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao atualizar contratada");
    } finally {
      setEditing(false);
    }
  };

  const handleSubmit = async () => {
    setError("");
    setSuccess("");

    if ((formData.senha || "").length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres");
      return;
    }

    const cnpjDigits = onlyDigits(formData.cnpj);
    if (!isValidCnpj(cnpjDigits)) {
      setError("CNPJ inválido");
      return;
    }

    setLoading(true);

    try {
      await api.post("/api/contratantes/contratada", {
        ...formData,
        cnpj: cnpjDigits,
      });
      setSuccess("Contratada cadastrada com sucesso!");
      setOpenDialog(false);
      setFormData({
        nome: "",
        cnpj: "",
        numeroContrato: "",
        numeroPedido: "",
        email: "",
        senha: "",
      });
      setShowCreateSenha(false);
      setCnpjTouched(false);
      loadContratadas(0);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao cadastrar contratada");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Deseja realmente excluir esta contratada?")) return;

    try {
      await api.delete(`/api/contratantes/contratadas/${id}`);
      setSuccess("Contratada excluída com sucesso!");
      loadContratadas(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao excluir contratada");
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
            Empresas Contratadas
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
            sx={{ backgroundColor: "#1b6c72ff" }}
          >
            Nova Contratada
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
                      <strong>Nome</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Email</strong>
                    </TableCell>
                    <TableCell>
                      <strong>CNPJ</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Nº Contrato</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Nº Pedido</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Ações</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {contratadas.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        Nenhuma contratada cadastrada
                      </TableCell>
                    </TableRow>
                  ) : (
                    contratadas.map((contratada) => (
                      <TableRow key={contratada.id}>
                        <TableCell>{contratada.nome}</TableCell>
                        <TableCell>{contratada.email}</TableCell>
                        <TableCell>{formatCnpj(contratada.cnpj)}</TableCell>
                        <TableCell>{contratada.numeroContrato}</TableCell>
                        <TableCell>{contratada.numeroPedido}</TableCell>
                        <TableCell align="center">
                          <IconButton color="primary" onClick={() => handleOpenEdit(contratada)}>
                            <EditIcon />
                          </IconButton>
                          <IconButton
                            color="error"
                            onClick={() => handleDelete(contratada.id)}
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

            <Box sx={{ display: "flex", justifyContent: "center", mt: 2, gap: 1 }}>
              <Button
                variant="outlined"
                disabled={pageInfo.page <= 0}
                onClick={() => loadContratadas(pageInfo.page - 1)}
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
                onClick={() => loadContratadas(pageInfo.page + 1)}
              >
                Próxima
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Nova Contratada</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome da Empresa"
                fullWidth
                required
                value={formData.nome}
                onChange={(e) =>
                  setFormData({ ...formData, nome: e.target.value })
                }
              />
              <TextField
                label="CNPJ"
                fullWidth
                required
                value={formData.cnpj}
                onChange={(e) =>
                  setFormData({ ...formData, cnpj: formatCnpj(e.target.value) })
                }
                onBlur={() => setCnpjTouched(true)}
                error={cnpjTouched && !!formData.cnpj && !isValidCnpj(formData.cnpj)}
                helperText={
                  cnpjTouched && !!formData.cnpj && !isValidCnpj(formData.cnpj)
                    ? "CNPJ inválido"
                    : ""
                }
              />
              <TextField
                label="Número do Contrato"
                fullWidth
                required
                value={formData.numeroContrato}
                onChange={(e) =>
                  setFormData({ ...formData, numeroContrato: e.target.value })
                }
              />
              <TextField
                label="Número do Pedido"
                fullWidth
                required
                value={formData.numeroPedido}
                onChange={(e) =>
                  setFormData({ ...formData, numeroPedido: e.target.value })
                }
              />
              <TextField
                label="Email de Acesso"
                type="email"
                fullWidth
                required
                value={formData.email}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
              />
              <TextField
                label="Senha"
                type={showCreateSenha ? "text" : "password"}
                fullWidth
                required
                value={formData.senha}
                onChange={(e) =>
                  setFormData({ ...formData, senha: e.target.value })
                }
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton edge="end" onClick={() => setShowCreateSenha((prev) => !prev)}>
                        {showCreateSenha ? <VisibilityOutlined /> : <VisibilityOffOutlined />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
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

        <Dialog open={openEdit} onClose={() => setOpenEdit(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Editar Contratada</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome da Empresa"
                fullWidth
                required
                value={editNome}
                onChange={(e) => setEditNome(e.target.value)}
              />
              <TextField
                label="E-mail"
                fullWidth
                required
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
              />
              <TextField
                label="Senha (opcional)"
                fullWidth
                type={showEditSenha ? "text" : "password"}
                value={editSenha}
                onChange={(e) => setEditSenha(e.target.value)}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton edge="end" onClick={() => setShowEditSenha((prev) => !prev)}>
                        {showEditSenha ? <VisibilityOutlined /> : <VisibilityOffOutlined />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <TextField
                label="CNPJ"
                fullWidth
                required
                value={editCnpj}
                onChange={(e) => setEditCnpj(e.target.value)}
              />
              <TextField
                label="Nº Contrato"
                fullWidth
                required
                value={editNumeroContrato}
                onChange={(e) => setEditNumeroContrato(e.target.value)}
              />
              <TextField
                label="Nº Pedido"
                fullWidth
                required
                value={editNumeroPedido}
                onChange={(e) => setEditNumeroPedido(e.target.value)}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button variant="outlined" onClick={() => setOpenEdit(false)} disabled={editing}>
              Cancelar
            </Button>
            <Button variant="contained" onClick={handleEdit} disabled={editing}>
              {editing ? "Salvando..." : "Salvar"}
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </AppLayout>
  );
}
