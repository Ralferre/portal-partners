import { Box, Divider, Grid, TextField, Typography } from "@mui/material";
import { AppLayout } from "./AppLayout";
import { Outlet } from "react-router-dom";
import { FileDropzone } from "../components/common/FileDropZone";

export function PostFiles() {
  return (
    <AppLayout>
      <Box>
        <Typography variant="h5" gutterBottom>
          Postar Documentos
        </Typography>
        <Typography variant="h5" color="text.secondary" fontSize={20}>
          Poste documentos (empresa e funcionários) de forma simples e rápida
        </Typography>
      </Box>
      <Box
        component="form"
        sx={{ "& .MuiTextField-root": { m: 1, width: "30ch" }, mt: 2 }}
      >
        <TextField required id="cnpj" label="CNPJ" />
        <TextField required id="razao-social" label="Razão Social" />
        <TextField required id="nome-fantasia" label="Nome Fantasia" />
        <TextField required id="endereco" label="Endereço" />
        <TextField required id="telefone" label="Telefone" />
        <TextField required id="email" label="E-mail" />
        <Divider sx={{ my: 2 }} />
        <TextField required id="cpf" label="CPF" />
        <TextField required id="nome-completo" label="Nome Completo" />
        <Divider sx={{ my: 2 }} />
      </Box>
      <Grid>
        <FileDropzone />
      </Grid>
    </AppLayout>
  );
}
