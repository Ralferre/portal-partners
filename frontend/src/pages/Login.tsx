import { Typography, Link } from "@mui/material";
import { Button } from "../components/common/Button";
import { InputField } from "../components/common/InputField";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import VisibilityOffOutlinedIcon from "@mui/icons-material/VisibilityOffOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import { JSX, useState } from "react";
import { isValidEmail } from "../utils/validators";
import { Auth } from "./Auth";
import { useAuth } from "../contexts/AuthContext";
import { useLocation, useNavigate } from "react-router-dom";

export function Login(): JSX.Element {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const handleClickShowPassword = () => setShowPassword(!showPassword);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitError(null);

    const res = isValidEmail(email);
    const resPasswordError = password.length >= 8;
    if (!res) {
      setEmailError("Digite um e-mail válido.");
      return;
    }
    if (!resPasswordError) {
      setPasswordError("A senha deve ter pelo menos 8 caracteres.");
      return;
    }

    try {
      setLoading(true);
      const authUser = await login({ email, senha: password });
      navigate(
        authUser.mustChangePassword ? "/primeiro-acesso/alterar-senha" : "/dashboard",
        { replace: true }
      );
    } catch (err: any) {
      const message = err?.response?.data?.message;
      setSubmitError(message || "Falha ao autenticar");
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
              Sistema de Gestão Documentação
            </Typography>

            <Typography variant="body1" color="text.secondary" mt={4} mb={3}>
              Gerencie Documentos e Processos de Forma Segura
            </Typography>

            <Typography
              fontSize={12}
              variant="body1"
              color="text.secondary"
              mb={0}
            >
              E-mail
            </Typography>
            <InputField
              id="email"
              value={email}
              required
              onChange={(e) => {
                const val = (e.target as HTMLInputElement).value;
                setEmail(val);
                const res = isValidEmail(val);
                setEmailError(res ? null : "Digite um e-mail válido.");
              }}
              label="Enter your e-mail"
              startIcon={<EmailOutlinedIcon />}
              error={!!emailError}
              helperText={emailError ?? undefined}
            />
            <Typography
              fontSize={12}
              variant="body1"
              color="text.secondary"
              mb={0}
              mt={2}
            >
              Password
            </Typography>
            <InputField
              id="password"
              type={showPassword ? "text" : "password"}
              value={password}
              label="E-mail"
              required
              onChange={(e) => {
                const val = (e.target as HTMLInputElement).value;
                setPassword(val);
                setPasswordError(
                  val.length < 8
                    ? "A senha deve ter pelo menos 8 caracteres."
                    : null
                );
              }}
              startIcon={<LockOutlinedIcon />}
              endIcon={
                showPassword ? (
                  <VisibilityOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => handleClickShowPassword()}
                  />
                ) : (
                  <VisibilityOffOutlinedIcon
                    sx={{ cursor: "pointer" }}
                    onClick={() => handleClickShowPassword()}
                  />
                )
              }
              error={!!passwordError}
              helperText={passwordError ?? undefined}
            />

            {submitError ? (
              <Typography color="error" mt={1}>
                {submitError}
              </Typography>
            ) : null}

            {location.state?.message ? (
              <Typography color="success.main" mt={1}>
                {String(location.state.message)}
              </Typography>
            ) : null}

            <Button type="submit" disabled={loading}>
              Entrar
            </Button>

            <Typography textAlign="right" mt={2}>
              <Link href="/forgot-password" underline="hover">
                Esqueci minha senha
              </Link>
            </Typography>
          </form>
        </>
      }
    ></Auth>
  );
}
