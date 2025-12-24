import { Box, Card, CardContent, Typography, IconButton } from "@mui/material";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import { useRef, useState } from "react";

export function FileDropzone() {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [files, setFiles] = useState<File[]>([]);

  function handleFiles(selectedFiles: FileList | null) {
    if (!selectedFiles) return;
    setFiles((prev) => [...prev, ...Array.from(selectedFiles)]);
  }

  function removeFile(index: number) {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  }

  return (
    <Card sx={{ backgroundColor: "#f5f6fa" }}>
      <CardContent>
        <Typography variant="h6" fontWeight={600} mb={0.5}>
          Upload de Arquivos
        </Typography>
        <Typography color="text.secondary" mb={3}>
          Realize o upload de múltiplos arquivos de forma fácil e rápida.
        </Typography>

        <Box
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            handleFiles(e.dataTransfer.files);
          }}
          sx={{
            border: "2px dashed",
            borderColor: "primary.main",
            borderRadius: 2,
            p: 4,
            textAlign: "center",
            cursor: "pointer",
            "&:hover": {
              backgroundColor: "action.hover",
            },
          }}
        >
          <CloudUploadOutlinedIcon
            color="primary"
            sx={{ fontSize: 48, mb: 1 }}
          />

          <Typography fontWeight={500}>
            Clique ou arraste e solte os arquivos aqui para fazer upload
          </Typography>

          <Typography variant="body2" color="text.secondary">
            Postagem de 20 arquivos, tamanho máximo 10MB
          </Typography>

          <input
            ref={inputRef}
            type="file"
            hidden
            multiple
            onChange={(e) => handleFiles(e.target.files)}
          />
        </Box>

        {files.length > 0 && (
          <Box mt={3}>
            {files.map((file, index) => (
              <Box
                key={index}
                display="flex"
                alignItems="center"
                justifyContent="space-between"
                p={1.5}
                borderRadius={1}
                sx={{ backgroundColor: "action.hover", mb: 1 }}
              >
                <Box display="flex" alignItems="center" gap={1}>
                  <CheckCircleOutlineIcon color="success" />
                  <Typography>{file.name}</Typography>
                </Box>

                <IconButton onClick={() => removeFile(index)}>
                  <DeleteOutlineIcon />
                </IconButton>
              </Box>
            ))}
          </Box>
        )}
      </CardContent>
    </Card>
  );
}
