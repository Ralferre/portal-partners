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

export function Login(): JSX.Element {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const handleClickShowPassword = () => setShowPassword(!showPassword);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const res = isValidEmail(email);
    const resPasswordError = password.length <= 8;
    if (!res) {
      setEmailError("Digite um e-mail válido.");
      return;
    }
    if (!resPasswordError) {
      setPasswordError("A senha deve ter pelo menos 6 caracteres.");
    }
    console.log("Email testado:", email);
    console.log("Password:", password);
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

            <Button type="submit">Entrar</Button>

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
