import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Card,
  CardContent,
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

type Funcionario = {
  id: number;
  nomeCompleto: string;
  cpf: string;
};

type PageResponse<T> = {
  content: T[];
  totalPages: number;
  number: number;
};

function formatCpf(value: string) {
  const digits = (value ?? "").replace(/\D/g, "").slice(0, 11);
  const p1 = digits.slice(0, 3);
  const p2 = digits.slice(3, 6);
  const p3 = digits.slice(6, 9);
  const p4 = digits.slice(9, 11);
  let out = p1;
  if (p2) out += `.${p2}`;
  if (p3) out += `.${p3}`;
  if (p4) out += `-${p4}`;
  return out;
}

export function AdminFuncionarios() {
  const [items, setItems] = useState<Funcionario[]>([]);
  const [error, setError] = useState("");
  const [pageInfo, setPageInfo] = useState<{ page: number; totalPages: number }>({
    page: 0,
    totalPages: 0,
  });

  useEffect(() => {
    load(0);
  }, []);

  const load = async (page: number) => {
    setError("");
    try {
      const response = await api.get<PageResponse<Funcionario>>(
        `/api/admin/funcionarios?page=${page}&size=10`
      );
      setItems(response.data.content ?? []);
      setPageInfo({
        page: response.data.number ?? page,
        totalPages: response.data.totalPages ?? 0,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao carregar funcionários");
    }
  };

  return (
    <AppLayout>
      <Box>
        <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
          Funcionários (Admin)
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
                      <strong>Nome Completo</strong>
                    </TableCell>
                    <TableCell>
                      <strong>CPF</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={2} align="center">
                        Nenhum funcionário cadastrado
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((f) => (
                      <TableRow key={f.id}>
                        <TableCell>{f.nomeCompleto}</TableCell>
                        <TableCell>{formatCpf(f.cpf)}</TableCell>
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
      </Box>
    </AppLayout>
  );
}
