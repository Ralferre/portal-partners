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
} from "@mui/material";
import api from "../../services/api";
import { AppLayout } from "../AppLayout";

type Contratante = {
  id: number;
  nome: string;
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
  const [createEmail, setCreateEmail] = useState("");
  const [createSenha, setCreateSenha] = useState("");
  const [creating, setCreating] = useState(false);

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

  const handleOpenCreate = () => {
    setError("");
    setSuccess("");
    setCreateNome("");
    setCreateEmail("");
    setCreateSenha("");
    setOpenCreate(true);
  };

  const handleCreate = async () => {
    setError("");
    setSuccess("");

    const nome = createNome.trim();
    const email = createEmail.trim();
    const senha = createSenha;

    if (!nome || !email || !senha) {
      setError("Informe nome, email e senha");
      return;
    }

    setCreating(true);
    try {
      await api.post("/api/admin/contratante", {
        nome,
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
                      <strong>ID</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Nome</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={2} align="center">
                        Nenhum contratante cadastrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>{c.id}</TableCell>
                        <TableCell>{c.nome}</TableCell>
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
                label="Senha"
                type="password"
                value={createSenha}
                onChange={(e) => setCreateSenha(e.target.value)}
                fullWidth
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
      </Box>
    </AppLayout>
  );
}
