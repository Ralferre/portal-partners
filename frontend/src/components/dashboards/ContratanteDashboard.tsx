import { useState, useEffect } from "react";
import { Box, Card, CardContent, Typography, Paper } from "@mui/material";
import {
  Description as DocumentIcon,
  Business as BusinessIcon,
  CheckCircle as ApprovedIcon,
  Warning as PendingIcon,
} from "@mui/icons-material";
import api from "../../services/api";
import {
  SevenDaysDocumentBarChart,
  type DailyDocumentStats,
} from "../common/SevenDaysDocumentBarChart";

type DashboardStats = {
  totalDocumentos: number;
  documentosAprovados: number;
  documentosPendentes: number;
  totalContratadas: number;
};

export function ContratanteDashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalDocumentos: 0,
    documentosAprovados: 0,
    documentosPendentes: 0,
    totalContratadas: 0,
  });

  const [chartData, setChartData] = useState<DailyDocumentStats[]>([]);

  useEffect(() => {
    loadStats();
    loadChart();
  }, []);

  const loadStats = async () => {
    try {
      const [docsResponse, contratadasResponse] = await Promise.all([
        api.get("/api/documentos?size=1000"),
        api.get("/api/contratantes/contratadas?size=1000"),
      ]);

      const docs = docsResponse.data.content || docsResponse.data;
      const contratadas = contratadasResponse.data.content || contratadasResponse.data;

      setStats({
        totalDocumentos: docs.length,
        documentosAprovados: docs.filter((d: any) => d.statusDocumento === "APROVADO").length,
        documentosPendentes: docs.filter((d: any) => d.statusDocumento === "PENDENTE").length,
        totalContratadas: contratadas.length,
      });
    } catch (err) {
      console.error("Erro ao carregar estatísticas:", err);
    }
  };

  const loadChart = async () => {
    try {
      const resp = await api.get<DailyDocumentStats[]>("/api/report/documentos/ultimos-7-dias");
      setChartData(resp.data ?? []);
    } catch (err) {
      console.error("Erro ao carregar gráfico:", err);
    }
  };

  const StatCard = ({ title, value, icon, color }: any) => (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <Box>
            <Typography color="textSecondary" gutterBottom variant="overline">
              {title}
            </Typography>
            <Typography variant="h4" component="div">
              {value}
            </Typography>
          </Box>
          <Box
            sx={{
              backgroundColor: color,
              borderRadius: "50%",
              width: 56,
              height: 56,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" sx={{ mb: 3 }}>
        Dashboard do Contratante
      </Typography>

      <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap", mb: 3 }}>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Minhas Contratadas"
            value={stats.totalContratadas}
            icon={<BusinessIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#1976d2"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Total de Documentos"
            value={stats.totalDocumentos}
            icon={<DocumentIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#9c27b0"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Aprovados"
            value={stats.documentosAprovados}
            icon={<ApprovedIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#2e7d32"
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Pendentes"
            value={stats.documentosPendentes}
            icon={<PendingIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#ed6c02"
          />
        </Box>
      </Box>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Suas Empresas Contratadas
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Gerencie suas empresas contratadas e acompanhe o status dos documentos enviados.
        </Typography>
      </Paper>

      <Paper sx={{ p: 3, mt: 3 }}>
        <SevenDaysDocumentBarChart
          title="Documentos (últimos 7 dias)"
          data={chartData}
        />
      </Paper>
    </Box>
  );
}
