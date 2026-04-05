import { Typography, Link } from "@mui/material";
import { Button } from "../components/common/Button";
import { InputField } from "../components/common/InputField";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { JSX, useMemo, useState } from "react";
import { Auth } from "./Auth";
import api from "../services/api";
import { useSearchParams } from "react-router-dom";

export function ResetPassword(): JSX.Element {
  const [params] = useSearchParams();

  const email = useMemo(() => params.get("email") ?? "", [params]);
  const token = useMemo(() => params.get("token") ?? "", [params]);

  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacao, setConfirmacao] = useState("");
  const [showNovaSenha, setShowNovaSenha] = useState(false);
  const [showConfirmacao, setShowConfirmacao] = useState(false);

  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const senhaValida = novaSenha.length >= 8;
  const confirmacaoOk = novaSenha === confirmacao;

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);
    setSubmitSuccess(null);

    if (!email || !token) {
      setSubmitError("Link inválido: email ou token ausente.");
      return;
    }

    if (!senhaValida) {
      setSubmitError("A senha deve ter ao menos 8 caracteres.");
      return;
    }

    if (!confirmacaoOk) {
      setSubmitError("A confirmação não confere.");
      return;
    }

    try {
      setLoading(true);
      const response = await api.post("/api/auth/reset-password", {
        email,
        token,
        novaSenha,
      });
      const msg = typeof response.data === "string" ? response.data : "Senha alterada.";
      setSubmitSuccess(msg);
      setNovaSenha("");
      setConfirmacao("");
    } catch (err: any) {
      const message = err?.response?.data;
      setSubmitError(message || "Falha ao resetar senha");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Auth
      left={
        <>
          <form onSubmit={handleSubmit}>
            <Typography
              variant="h4"
              fontWeight="bold"
              gutterBottom
              align="center"
            >
              Redefinir senha
            </Typography>

            <Typography variant="body1" color="text.secondary" mt={3} mb={3}>
              Defina uma nova senha para o usuário: <strong>{email || "(não informado)"}</strong>
            </Typography>

            <Typography fontSize={12} variant="body1" color="text.secondary" mb={0}>
              Nova senha
            </Typography>
            <InputField
              id="novaSenha"
              value={novaSenha}
              required
              type={showNovaSenha ? "text" : "password"}
              onChange={(e) => setNovaSenha((e.target as HTMLInputElement).value)}
              label="Digite sua nova senha"
              startIcon={<LockOutlinedIcon />}
              endIcon={
                showNovaSenha ? (
                  <VisibilityOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => setShowNovaSenha(false)}
                  />
                ) : (
                  <VisibilityOffOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => setShowNovaSenha(true)}
                  />
                )
              }
              error={!!novaSenha && !senhaValida}
              helperText={!!novaSenha && !senhaValida ? "Mínimo 8 caracteres" : undefined}
            />

            <Typography fontSize={12} variant="body1" color="text.secondary" mb={0}>
              Confirmar senha
            </Typography>
            <InputField
              id="confirmacao"
              value={confirmacao}
              required
              type={showConfirmacao ? "text" : "password"}
              onChange={(e) => setConfirmacao((e.target as HTMLInputElement).value)}
              label="Confirme a nova senha"
              startIcon={<LockOutlinedIcon />}
              endIcon={
                showConfirmacao ? (
                  <VisibilityOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => setShowConfirmacao(false)}
                  />
                ) : (
                  <VisibilityOffOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => setShowConfirmacao(true)}
                  />
                )
              }
              error={!!confirmacao && !confirmacaoOk}
              helperText={!!confirmacao && !confirmacaoOk ? "A confirmação não confere" : undefined}
            />

            {submitError ? (
              <Typography color="error" mt={1}>
                {submitError}
              </Typography>
            ) : null}

            {submitSuccess ? (
              <Typography color="success.main" mt={1}>
                {submitSuccess}
              </Typography>
            ) : null}

            <Button type="submit" disabled={loading}>
              Salvar
            </Button>

            <Typography textAlign="right" mt={2}>
              <Link href="/login" underline="hover">
                Ir para o login
              </Link>
            </Typography>
          </form>
        </>
      }
    ></Auth>
  );
}
