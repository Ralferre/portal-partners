import {
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Paper,
  TableContainer,
} from "@mui/material";

function createData(
  companyName: string,
  employeeName: string,
  datePost: string,
  comments: string,
  status: string
) {
  return { companyName, employeeName, datePost, comments, status };
}

const rows = [
  createData("XPTO", "John", "21/12/2025", "Falta guia FGTS", "Em análise"),
  createData("XPTO", "Zé", "21/12/2025", "Falta guia FGTS", "Em análise"),
  createData("ZERO", "Joaquim", "21/12/2025", "Falta guia FGTS", "Em análise"),
  createData("ZERO", "Fiote", "21/12/2025", "Falta guia FGTS", "Em análise"),
  createData(
    "ZERO",
    "Zé da Manga",
    "21/12/2025",
    "Falta guia FGTS",
    "Em análise"
  ),
  createData("ASTM", "John", "21/12/2025", "Falta guia FGTS", "Em análise"),
  createData("WIKA", "John", "21/12/2025", "Falta guia FGTS", "Em análise"),
];

export function AppTable() {
  return (
    <TableContainer sx={{ mt: 2, borderRadius: 3 }} component={Paper}>
      <Table sx={{ minWidth: 650 }} aria-label="simple table">
        <TableHead>
          <TableRow>
            <TableCell sx={{ fontWeight: "bold", fontSize: "1rem" }}>
              Company Name
            </TableCell>
            <TableCell sx={{ fontWeight: "bold", fontSize: "1rem" }}>
              Employee Name
            </TableCell>
            <TableCell sx={{ fontWeight: "bold", fontSize: "1rem" }}>
              Date Post
            </TableCell>
            <TableCell sx={{ fontWeight: "bold", fontSize: "1rem" }}>
              Comments
            </TableCell>
            <TableCell sx={{ fontWeight: "bold", fontSize: "1rem" }}>
              Status
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.companyName + row.employeeName}>
              <TableCell>{row.companyName}</TableCell>
              <TableCell>{row.employeeName}</TableCell>
              <TableCell>{row.datePost}</TableCell>
              <TableCell>{row.comments}</TableCell>
              <TableCell>{row.status}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
