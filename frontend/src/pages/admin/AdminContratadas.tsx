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

type Contratada = {
  id: number;
  nome: string;
  cnpj: string;
  numeroContrato: string;
  numeroPedido: string;
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
                  </TableRow>
                </TableHead>
                <TableBody>
                  {items.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        Nenhuma contratada cadastrada
                      </TableCell>
                    </TableRow>
                  ) : (
                    items.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>{c.nome}</TableCell>
                        <TableCell>{formatCnpj(c.cnpj)}</TableCell>
                        <TableCell>{c.numeroContrato}</TableCell>
                        <TableCell>{c.numeroPedido}</TableCell>
                        <TableCell>{c.contratanteId ?? "-"}</TableCell>
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
