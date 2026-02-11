import Box from "@mui/material/Box"
import MenuItem from "@mui/material/MenuItem"
import Pagination from "@mui/material/Pagination"
import Select from "@mui/material/Select"
import Typography from "@mui/material/Typography"

export interface PaginatorProps {
  page: number
  size: number
  total: number
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
}


export function Paginator({
  page,
  size,
  total,
  onPageChange,
  onSizeChange
}: PaginatorProps) {
  const totalPages = Math.ceil(total / size)

  // Skjul paginator hvis det bare er én side


  const start = page * size + 1
  const end = Math.min(total, (page + 1) * size)

  return (
    <Box
      display="flex"
      alignItems="center"
      justifyContent="space-between"
      gap={2}
      sx={{ m: 2 }}
    >
      {/* Page size dropdown */}
      <Select
        size="small"
        value={size}
        onChange={e => onSizeChange(Number(e.target.value))}
      >
        {[10, 25, 50, 100].map(opt => (
          <MenuItem key={opt} value={opt}>
            {opt} per side
          </MenuItem>
        ))}
      </Select>

      {/* Sideknapper */}
      <Pagination
        count={totalPages}
        page={page + 1}
        onChange={(_, value) => onPageChange(value - 1)}
        color="primary"
        shape="rounded"
        size="small"
      />

      {/* Range info */}
      <Typography variant="body2">
        {start}–{end} av {total}
      </Typography>


    </Box>
  )
}
