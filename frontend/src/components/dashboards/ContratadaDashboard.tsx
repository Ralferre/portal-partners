import { useState, useEffect } from "react";
import { Box, Card, CardContent, Typography, Paper } from "@mui/material";
import {
  Description as DocumentIcon,
  People as PeopleIcon,
  CheckCircle as ApprovedIcon,
  Warning as PendingIcon,
  CloudUpload as UploadIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import {
  SevenDaysDocumentBarChart,
  type DailyDocumentStats,
} from "../common/SevenDaysDocumentBarChart";

type DashboardStats = {
  totalDocumentos: number;
  documentosAprovados: number;
  documentosPendentes: number;
  totalFuncionarios: number;
};

export function ContratadaDashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState<DashboardStats>({
    totalDocumentos: 0,
    documentosAprovados: 0,
    documentosPendentes: 0,
    totalFuncionarios: 0,
  });

  const [chartData, setChartData] = useState<DailyDocumentStats[]>([]);

  useEffect(() => {
    loadStats();
    loadChart();
  }, []);

  const loadStats = async () => {
    try {
      const [docsResponse, funcResponse] = await Promise.all([
        api.get("/api/documentos?size=1000"),
        api.get("/api/contratadas/funcionarios?size=1000"),
      ]);

      const docs = docsResponse.data.content || docsResponse.data;
      const funcionarios = funcResponse.data.content || funcResponse.data;

      setStats({
        totalDocumentos: docs.length,
        documentosAprovados: docs.filter((d: any) => d.statusDocumento === "APROVADO").length,
        documentosPendentes: docs.filter((d: any) => d.statusDocumento === "PENDENTE").length,
        totalFuncionarios: funcionarios.length,
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

  const StatCard = ({ title, value, icon, color, onClick }: any) => (
    <Card sx={{ height: "100%", cursor: onClick ? "pointer" : "default" }} onClick={onClick}>
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
        Dashboard da Contratada
      </Typography>

      <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap", mb: 3 }}>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Meus Funcionários"
            value={stats.totalFuncionarios}
            icon={<PeopleIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#1976d2"
            onClick={() => navigate("/funcionarios")}
          />
        </Box>
        <Box sx={{ flex: "1 1 250px" }}>
          <StatCard
            title="Total de Documentos"
            value={stats.totalDocumentos}
            icon={<DocumentIcon sx={{ color: "#fff", fontSize: 30 }} />}
            color="#9c27b0"
            onClick={() => navigate("/documentos")}
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

      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Box>
            <Typography variant="h6" sx={{ mb: 1 }}>
              Enviar Novo Documento
            </Typography>
            <Typography variant="body1" color="textSecondary">
              Faça upload de documentos da empresa ou de funcionários
            </Typography>
          </Box>
          <Box
            sx={{
              backgroundColor: "#1b6c72ff",
              borderRadius: "50%",
              width: 64,
              height: 64,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
              "&:hover": {
                backgroundColor: "#155a5f",
              },
            }}
            onClick={() => navigate("/upload-documento")}
          >
            <UploadIcon sx={{ color: "#fff", fontSize: 32 }} />
          </Box>
        </Box>
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Ações Rápidas
        </Typography>
        <Typography variant="body1" color="textSecondary">
          • Cadastre funcionários para vincular documentos específicos
          <br />
          • Envie documentos da empresa ou de funcionários
          <br />
          • Acompanhe o status de aprovação dos documentos
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
