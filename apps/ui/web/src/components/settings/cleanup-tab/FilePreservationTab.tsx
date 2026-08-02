import {
  Checkbox,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import type { PreservedFile } from "../../../types/types";

interface FilePreservationTabProps {
  files: PreservedFile[];
  setFiles: React.Dispatch<React.SetStateAction<PreservedFile[]>>;
}

export default function FilePreservationTab({
  files,
  setFiles,
}: FilePreservationTabProps) {
  function toggleLocal(file: PreservedFile) {
    setFiles((prev) =>
      prev.map((f) =>
        f.filePath === file.filePath ? { ...f, preserved: !f.preserved } : f,
      ),
    );
  }

  return (
    <Stack spacing={3} sx={{ height: "100%", minHeight: 0 }}>
      <Typography variant="h5">File Preservation</Typography>

      <TableContainer
        component={Paper}
        sx={{
          flex: 1,
          minHeight: 0,
          overflow: "auto",
        }}
      >
        <Table stickyHeader size="small">
          <TableHead>
            <TableRow>
              <TableCell>Preserve</TableCell>
              <TableCell>File Name</TableCell>
              <TableCell>Used In</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {files.map((f) => (
              <TableRow key={f.filePath} hover>
                <TableCell>
                  <Checkbox
                    checked={f.preserved}
                    onChange={() => toggleLocal(f)}
                  />
                </TableCell>
                <TableCell>{f.fileName}</TableCell>
                <TableCell>{f.usedInReferences.join(", ")}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Stack>
  );
}
