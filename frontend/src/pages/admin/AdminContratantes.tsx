import { useEffect, useState } from "react";
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
  Alert,
  IconButton,
  InputAdornment,
} from "@mui/material";
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
  VisibilityOffOutlined,
  VisibilityOutlined,
} from "@mui/icons-material";
import api from "../../services/api";
import { AppLayout } from "../AppLayout";
import { formatCnpj, isValidCnpj, onlyDigits } from "../../utils/validators";

type Contratante = {
  id: number;
  nome: string;
  cnpj?: string;
  email: string;
  usuarioId?: number;
};

type PageResponse<T> = {
  content: T[];
  totalPages: number;
  number: number;
};

export function AdminContratantes() {
  const [items, setItems] = useState<Contratante[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number }>({
    page: 0,
    totalPages: 0,
  });

  const [openCreate, setOpenCreate] = useState(false);
  const [createNome, setCreateNome] = useState("");
  const [createCnpj, setCreateCnpj] = useState("");
  const [createEmail, setCreateEmail] = useState("");
  const [createSenha, setCreateSenha] = useState("");
  const [showCreateSenha, setShowCreateSenha] = useState(false);
  const [creating, setCreating] = useState(false);

  const [openEdit, setOpenEdit] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editNome, setEditNome] = useState("");
  const [editCnpj, setEditCnpj] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editSenha, setEditSenha] = useState("");
  const [showEditSenha, setShowEditSenha] = useState(false);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    load(0);
  }, []);

  const load = async (page: number) => {
    setError("");
    setSuccess("");
    try {
      const response = await api.get<PageResponse<Contratante>>(
        `/api/admin/contratantes?page=${page}`
      );
      setItems(response.data.content ?? []);
      setPageInfo({
        page: response.data.number ?? page,
        totalPages: response.data.totalPages ?? 0,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar contratantes");
    }
  };

  const handleDelete = async (nome: string) => {
    setError("");
    setSuccess("");

    if (!window.confirm(`Deseja realmente excluir a contratante "${nome}"?`)) return;

    try {
      await api.delete(`/api/admin/contratante/${encodeURIComponent(nome)}`);
      setSuccess("Contratante excluída com sucesso");
      await load(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao excluir contratante");
    }
  };

  const handleOpenCreate = () => {
    setError("");
    setSuccess("");
    setCreateNome("");
    setCreateCnpj("");
    setCreateEmail("");
    setCreateSenha("");
    setShowCreateSenha(false);
    setOpenCreate(true);
  };

  const handleOpenEdit = (c: Contratante) => {
    setError("");
    setSuccess("");
    setEditId(c.id);
    setEditNome(c.nome ?? "");
    setEditCnpj(c.cnpj ?? "");
    setEditEmail(c.email ?? "");
    setEditSenha("");
    setShowEditSenha(false);
    setOpenEdit(true);
  };

  const handleEdit = async () => {
    setError("");
    setSuccess("");

    if (editId == null) {
      setError("Contratante inválida");
      return;
    }

    const nome = editNome.trim();
    const cnpj = onlyDigits(editCnpj);
    const email = editEmail.trim();
    const senha = editSenha;

    if (!nome || !email || !cnpj) {
      setError("Informe nome, CNPJ e email");
      return;
    }
    if (!isValidCnpj(cnpj)) {
      setError("CNPJ inválido");
      return;
    }

    if (senha && senha.length > 0 && senha.length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres");
      return;
    }

    setEditing(true);
    try {
      await api.put(`/api/admin/contratantes/${editId}`, {
        nome,
        cnpj: cnpj || null,
        email,
        senha: senha?.trim() ? senha : null,
      });
      setOpenEdit(false);
      setSuccess("Contratante atualizada com sucesso");
      await load(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao atualizar contratante");
    } finally {
      setEditing(false);
    }
  };

  const handleCreate = async () => {
    setError("");
    setSuccess("");

    const nome = createNome.trim();
    const cnpj = onlyDigits(createCnpj);
    const email = createEmail.trim();
    const senha = createSenha;

    if (!nome || !cnpj || !email || !senha) {
      setError("Informe nome, CNPJ, email e senha");
      return;
    }
    if (!isValidCnpj(cnpj)) {
      setError("CNPJ inválido");
      return;
    }

    if (senha.length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres");
      return;
    }

    setCreating(true);
    try {
      await api.post("/api/admin/contratante", {
        nome,
        cnpj: cnpj || null,
        email,
        senha,
      });
      setOpenCreate(false);
      setSuccess("Contratante cadastrada com sucesso");
      await load(0);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao cadastrar contratante");
    } finally {
      setCreating(false);
    }
  };

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Contratantes (Admin)
        </Typography>

        <Box sx={{ display: "flex", justifyContent: "flex-end", mb: 2 }}>
          <Button variant="contained" onClick={handleOpenCreate}>
            Cadastrar Contratante
          </Button>
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess("")}>
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
                      <strong>CNPJ</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Email</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Nome</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Ações</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        Nenhum contratante cadastrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>{c.cnpj || "-"}</TableCell>
                        <TableCell>{c.email}</TableCell>
                        <TableCell>{c.nome}</TableCell>
                        <TableCell align="center">
                          <IconButton color="primary" onClick={() => handleOpenEdit(c)}>
                            <EditIcon />
                          </IconButton>
                          <IconButton color="error" onClick={() => handleDelete(c.nome)}>
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
                disabled={pageInfo.page + 1 >= pageInfo.totalPages}
                onClick={() => load(pageInfo.page + 1)}
              >
                Próxima
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Dialog open={openCreate} onClose={() => setOpenCreate(false)} fullWidth maxWidth="sm">
          <DialogTitle>Cadastrar Contratante</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome"
                value={createNome}
                onChange={(e) => setCreateNome(e.target.value)}
                fullWidth
              />
              <TextField
                label="Email"
                value={createEmail}
                onChange={(e) => setCreateEmail(e.target.value)}
                fullWidth
              />
              <TextField
                label="CNPJ (obrigatório para nova organização)"
                value={createCnpj}
                onChange={(e) => setCreateCnpj(formatCnpj(e.target.value))}
                fullWidth
              />
              <TextField
                label="Senha"
                type={showCreateSenha ? "text" : "password"}
                value={createSenha}
                onChange={(e) => setCreateSenha(e.target.value)}
                fullWidth
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
            <Button variant="outlined" onClick={() => setOpenCreate(false)} disabled={creating}>
              Cancelar
            </Button>
            <Button variant="contained" onClick={handleCreate} disabled={creating}>
              {creating ? "Salvando..." : "Salvar"}
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog open={openEdit} onClose={() => setOpenEdit(false)} fullWidth maxWidth="sm">
          <DialogTitle>Editar Contratante</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome"
                value={editNome}
                onChange={(e) => setEditNome(e.target.value)}
                fullWidth
              />
              <TextField
                label="Email"
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
                fullWidth
              />
              <TextField
                label="CNPJ"
                value={editCnpj}
                onChange={(e) => setEditCnpj(formatCnpj(e.target.value))}
                fullWidth
              />
              <TextField
                label="Senha (opcional)"
                type={showEditSenha ? "text" : "password"}
                value={editSenha}
                onChange={(e) => setEditSenha(e.target.value)}
                fullWidth
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
