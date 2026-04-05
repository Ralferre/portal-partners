import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Paper,
} from "@mui/material";
import { Add as AddIcon } from "@mui/icons-material";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import api from "../services/api";
import { AppLayout } from "./AppLayout";
import { isValidEmail } from "../utils/validators";

type UsuarioContratante = {
  id: number;
  nome: string;
  email: string;
  mustChangePassword: boolean;
  principal: boolean;
};

export function UsuariosContratante() {
  const [usuarios, setUsuarios] = useState<UsuarioContratante[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [openCreate, setOpenCreate] = useState(false);
  const [loading, setLoading] = useState(false);
  const [createNome, setCreateNome] = useState("");
  const [createEmail, setCreateEmail] = useState("");
  const [createSenha, setCreateSenha] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const emailValido = useMemo(() => !createEmail || isValidEmail(createEmail), [createEmail]);

  const loadUsuarios = async () => {
    setError("");
    try {
      const response = await api.get<UsuarioContratante[]>("/api/contratantes/usuarios");
      setUsuarios(response.data ?? []);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar usuários da contratante");
    }
  };

  useEffect(() => {
    loadUsuarios();
  }, []);

  const handleCreate = async () => {
    setError("");
    setSuccess("");

    const nome = createNome.trim();
    const email = createEmail.trim().toLowerCase();

    if (!nome || !email || !createSenha) {
      setError("Informe nome, e-mail e senha.");
      return;
    }
    if (!isValidEmail(email)) {
      setError("Informe um e-mail válido.");
      return;
    }
    if (createSenha.length < 8) {
      setError("A senha deve ter no mínimo 8 caracteres.");
      return;
    }

    try {
      setLoading(true);
      await api.post("/api/contratantes/usuarios", {
        nome,
        email,
        senha: createSenha,
      });
      setSuccess("Usuário da contratante cadastrado com sucesso.");
      setOpenCreate(false);
      setCreateNome("");
      setCreateEmail("");
      setCreateSenha("");
      setShowPassword(false);
      await loadUsuarios();
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao cadastrar usuário.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppLayout>
      <Box>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
          <Typography variant="h4" fontWeight="bold">
            Usuários da Contratante
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setError("");
              setSuccess("");
              setOpenCreate(true);
            }}
          >
            Novo Usuário
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
                      <strong>Nome</strong>
                    </TableCell>
                    <TableCell>
                      <strong>E-mail</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Status</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Tipo</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {usuarios.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        Nenhum usuário vinculado à contratante
                      </TableCell>
                    </TableRow>
                  ) : (
                    usuarios.map((usuario) => (
                      <TableRow key={usuario.id}>
                        <TableCell>{usuario.nome || "-"}</TableCell>
                        <TableCell>{usuario.email}</TableCell>
                        <TableCell>
                          {usuario.mustChangePassword ? (
                            <Chip size="small" label="Primeiro acesso pendente" color="warning" />
                          ) : (
                            <Chip size="small" label="Ativo" color="success" />
                          )}
                        </TableCell>
                        <TableCell>
                          {usuario.principal ? (
                            <Chip size="small" label="Principal" />
                          ) : (
                            <Chip size="small" label="Vinculado" variant="outlined" />
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        <Dialog open={openCreate} onClose={() => setOpenCreate(false)} fullWidth maxWidth="sm">
          <DialogTitle>Novo Usuário da Contratante</DialogTitle>
          <DialogContent>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2, mt: 1 }}>
              <TextField
                label="Nome"
                value={createNome}
                onChange={(e) => setCreateNome(e.target.value)}
                fullWidth
              />
              <TextField
                label="E-mail"
                type="email"
                value={createEmail}
                onChange={(e) => setCreateEmail(e.target.value)}
                fullWidth
                error={!emailValido}
                helperText={!emailValido ? "Informe um e-mail válido." : ""}
              />
              <TextField
                label="Senha"
                type={showPassword ? "text" : "password"}
                value={createSenha}
                onChange={(e) => setCreateSenha(e.target.value)}
                fullWidth
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton edge="end" onClick={() => setShowPassword((prev) => !prev)}>
                        {showPassword ? <VisibilityOutlinedIcon /> : <VisibilityOffOutlinedIcon />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button variant="outlined" onClick={() => setOpenCreate(false)} disabled={loading}>
              Cancelar
            </Button>
            <Button variant="contained" onClick={handleCreate} disabled={loading}>
              {loading ? "Salvando..." : "Salvar"}
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </AppLayout>
  );
}
