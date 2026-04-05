import { Typography, Link } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { JSX, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Auth } from "./Auth";
import { Button } from "../components/common/Button";
import { InputField } from "../components/common/InputField";
import api from "../services/api";
import { useAuth } from "../contexts/AuthContext";

export function FirstAccessChangePassword(): JSX.Element {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [showAtual, setShowAtual] = useState(false);
  const [showNova, setShowNova] = useState(false);
  const [showConfirmacao, setShowConfirmacao] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const senhaValida = novaSenha.length >= 8;
  const confirmacaoOk = novaSenha === confirmacao;

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);

    if (!senhaAtual || !novaSenha || !confirmacao) {
      setSubmitError("Preencha todos os campos.");
      return;
    }
    if (!senhaValida) {
      setSubmitError("A nova senha deve ter no mínimo 8 caracteres.");
      return;
    }
    if (!confirmacaoOk) {
      setSubmitError("A confirmação da nova senha não confere.");
      return;
    }

    try {
      setLoading(true);
      await api.post("/api/auth/change-password-first-access", {
        senhaAtual,
        novaSenha,
      });
      logout();
      navigate("/login", {
        replace: true,
        state: { message: "Senha alterada com sucesso. Faça login novamente." },
      });
    } catch (err: any) {
      setSubmitError(err?.response?.data?.message || "Erro ao alterar senha.");
    } finally {
      setLoading(false);
    }
  };

  const eyeIcon = (visible: boolean, onClick: () => void) =>
    visible ? (
      <VisibilityOutlinedIcon sx={{ cursor: "pointer" }} onClick={onClick} />
    ) : (
      <VisibilityOffOutlinedIcon sx={{ cursor: "pointer" }} onClick={onClick} />
    );

  return (
    <Auth
      left={
        <form onSubmit={handleSubmit}>
          <Typography variant="h4" fontWeight="bold" gutterBottom align="center">
            Primeiro acesso
          </Typography>

          <Typography variant="body1" color="text.secondary" mt={3} mb={3}>
            Por segurança, altere sua senha antes de utilizar a aplicação.
          </Typography>

          <Typography fontSize={12} variant="body1" color="text.secondary" mb={0}>
            Senha atual
          </Typography>
          <InputField
            label="Digite sua senha atual"
            type={showAtual ? "text" : "password"}
            value={senhaAtual}
            onChange={(e) => setSenhaAtual((e.target as HTMLInputElement).value)}
            startIcon={<LockOutlinedIcon />}
            endIcon={eyeIcon(showAtual, () => setShowAtual((prev) => !prev))}
            required
          />

          <Typography fontSize={12} variant="body1" color="text.secondary" mb={0} mt={2}>
            Nova senha
          </Typography>
          <InputField
            label="Digite sua nova senha"
            type={showNova ? "text" : "password"}
            value={novaSenha}
            onChange={(e) => setNovaSenha((e.target as HTMLInputElement).value)}
            startIcon={<LockOutlinedIcon />}
            endIcon={eyeIcon(showNova, () => setShowNova((prev) => !prev))}
            error={!!novaSenha && !senhaValida}
            helperText={!!novaSenha && !senhaValida ? "Mínimo 8 caracteres" : undefined}
            required
          />

          <Typography fontSize={12} variant="body1" color="text.secondary" mb={0} mt={2}>
            Confirmar nova senha
          </Typography>
          <InputField
            label="Confirme sua nova senha"
            type={showConfirmacao ? "text" : "password"}
            value={confirmacao}
            onChange={(e) => setConfirmacao((e.target as HTMLInputElement).value)}
            startIcon={<LockOutlinedIcon />}
            endIcon={eyeIcon(showConfirmacao, () => setShowConfirmacao((prev) => !prev))}
            error={!!confirmacao && !confirmacaoOk}
            helperText={!!confirmacao && !confirmacaoOk ? "A confirmação não confere" : undefined}
            required
          />

          {submitError ? (
            <Typography color="error" mt={1}>
              {submitError}
            </Typography>
          ) : null}

          <Button type="submit" disabled={loading}>
            Alterar senha
          </Button>

          <Typography textAlign="right" mt={2}>
            <Link
              component="button"
              type="button"
              underline="hover"
              onClick={() => {
                logout();
                navigate("/login", { replace: true });
              }}
            >
              Voltar ao login
            </Link>
          </Typography>
        </form>
      }
    />
  );
}
