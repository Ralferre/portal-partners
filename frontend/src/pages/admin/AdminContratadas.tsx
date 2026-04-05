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
import { Edit as EditIcon, VisibilityOffOutlined, VisibilityOutlined } from "@mui/icons-material";
import api from "../../services/api";
import { AppLayout } from "../AppLayout";

type Contratada = {
  id: number;
  nome: string;
  cnpj: string;
  numeroContrato: string;
  numeroPedido: string;
  email: string;
  contratanteId?: number;
};

type PageResponse<T> = {
  content: T[];
  totalPages: number;
  number: number;
};

function formatCnpj(value: string) {
  const digits = (value ?? "").replace(/\D/g, "").slice(0, 14);
  const p1 = digits.slice(0, 2);
  const p2 = digits.slice(2, 5);
  const p3 = digits.slice(5, 8);
  const p4 = digits.slice(8, 12);
  const p5 = digits.slice(12, 14);
  let out = p1;
  if (p2) out += `.${p2}`;
  if (p3) out += `.${p3}`;
  if (p4) out += `/${p4}`;
  if (p5) out += `-${p5}`;
  return out;
}

export function AdminContratadas() {
  const [items, setItems] = useState<Contratada[]>([]);
  const [error, setError] = useState("");
  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number }>({
    page: 0,
    totalPages: 0,
  });

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

  useEffect(() => {
    load(0);
  }, []);

  const load = async (page: number) => {
    setError("");
    try {
      const response = await api.get<PageResponse<Contratada>>(
        `/api/admin/contratadas?page=${page}`
      );
      setItems(response.data.content ?? []);
      setPageInfo({
        page: response.data.number ?? page,
        totalPages: response.data.totalPages ?? 0,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar contratadas");
    }
  };

  const handleOpenEdit = (c: Contratada) => {
    setError("");
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

    if (editId == null) {
      setError("Contratada inválida");
      return;
    }

    const nome = editNome.trim();
    const email = editEmail.trim();
    const cnpj = editCnpj.trim();
    const numeroContrato = editNumeroContrato.trim();
    const numeroPedido = editNumeroPedido.trim();
    const senha = editSenha;

    if (!nome || !email || !cnpj || !numeroContrato || !numeroPedido) {
      setError("Preencha nome, email, cnpj, nº contrato e nº pedido");
      return;
    }

    if (senha && senha.length > 0 && senha.length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres");
      return;
    }

    setEditing(true);
    try {
      await api.put(`/api/admin/contratadas/${editId}`, {
        nome,
        email,
        senha: senha?.trim() ? senha : null,
        cnpj,
        numeroContrato,
        numeroPedido,
      });
      setOpenEdit(false);
      await load(pageInfo.page);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao atualizar contratada");
    } finally {
      setEditing(false);
    }
  };

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Contratadas (Admin)
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError("")}>
            {error}
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
                    <TableCell>
                      <strong>Contratante ID</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Ações</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        Nenhuma contratada cadastrada
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>{c.nome}</TableCell>
                        <TableCell>{c.email}</TableCell>
                        <TableCell>{formatCnpj(c.cnpj)}</TableCell>
                        <TableCell>{c.numeroContrato}</TableCell>
                        <TableCell>{c.numeroPedido}</TableCell>
                        <TableCell>{c.contratanteId ?? "-"}</TableCell>
                        <TableCell align="center">
                          <IconButton color="primary" onClick={() => handleOpenEdit(c)}>
                            <EditIcon />
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

        <Dialog open={openEdit} onClose={() => setOpenEdit(false)} fullWidth maxWidth="sm">
          <DialogTitle>Editar Contratada</DialogTitle>
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
              <TextField
                label="CNPJ"
                value={editCnpj}
                onChange={(e) => setEditCnpj(e.target.value)}
                fullWidth
              />
              <TextField
                label="Nº Contrato"
                value={editNumeroContrato}
                onChange={(e) => setEditNumeroContrato(e.target.value)}
                fullWidth
              />
              <TextField
                label="Nº Pedido"
                value={editNumeroPedido}
                onChange={(e) => setEditNumeroPedido(e.target.value)}
                fullWidth
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
