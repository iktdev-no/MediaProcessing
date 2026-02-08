import AlbumIcon from '@mui/icons-material/Album'
import { Box, LinearProgress, Paper, Stack, Typography } from "@mui/material"
import type { DiskInfo } from '../../types/transfer-model'

export function StoragePanel({ disks }: { disks: DiskInfo[] }) {
    if (!disks) return null

    return (
        <Box
            sx={{
                display: "grid",
                gap: 2,
                mt: 2,
                gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
                alignItems: "stretch",
            }}
        >
            {disks.map(disk => {
                const color =
                    disk.usedPercent > 85 ? "error" :
                        disk.usedPercent > 70 ? "warning" :
                            "success"

                return (
                    <Paper
                        key={disk.mount}
                        sx={{
                            p: 2,
                            border: 1,
                            borderColor: `${color}.main`,
                            borderRadius: 2,
                            display: "flex",
                            flexDirection: "column",
                            height: "100%",
                        }}
                    >
                        <Stack spacing={1} sx={{ flexGrow: 1 }}>
                            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                                <AlbumIcon sx={{ verticalAlign: "middle", mr: 1 }} />
                                {disk.mount}
                            </Typography>

                            <Typography variant="body2">
                                Brukt: {disk.usedPercent.toFixed(1)}%
                            </Typography>

                            <LinearProgress
                                variant="determinate"
                                value={disk.usedPercent}
                                sx={{
                                    height: 10,
                                    borderRadius: 5,
                                    [`& .MuiLinearProgress-bar`]: {
                                        backgroundColor: `${color}.main`,
                                    },
                                    backgroundColor: "rgba(255,255,255,0.1)",
                                }}
                            />

                            <Typography variant="body2" sx={{ opacity: 0.7 }}>
                                Ledig: {(disk.freeBytes / 1024 / 1024 / 1024).toFixed(1)} GB
                            </Typography>
                        </Stack>
                    </Paper>
                )
            })}
        </Box>
    )
}
