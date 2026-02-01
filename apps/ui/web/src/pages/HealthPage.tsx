import AutorenewIcon from "@mui/icons-material/Autorenew"
import BuildIcon from "@mui/icons-material/Build"
import DnsIcon from "@mui/icons-material/Dns"
import HubIcon from "@mui/icons-material/Hub"
import LanguageIcon from "@mui/icons-material/Language"
import StorageIcon from "@mui/icons-material/Storage"
import VisibilityIcon from "@mui/icons-material/Visibility"
import { Box, Typography } from "@mui/material"
import Grid from "@mui/material/Grid"
import { NodeBox } from "../components/NodeBox"
import { StatusLine } from "../components/StatusLine"

import { useHealth } from "../context/HealthProvider"

interface HealthTopologyProps {
    sseOk: boolean
    restOk: boolean
    coordinatorSseOk: boolean
    coordinatorRestOk: boolean
}


export default function HealthPage() {
    const { status, raw, backend } = useHealth()


    return (
        <Box sx={{ p: 3, display: "flex", flexDirection: "column", gap: 4, textAlign: "center" }}>
            <Typography variant="h4">System health</Typography>

            <HealthTopology
                sseOk={backend?.sseOk ?? false}
                restOk={backend?.restOk ?? false}
                coordinatorSseOk={raw?.coordinatorSse ?? false}
                coordinatorRestOk={raw?.coordinatorRest ?? false}
            />

            <ServicesGrid
                processer={raw?.processer ?? false}
                converter={raw?.converter ?? false}
                pyMetadata={raw?.pyMetadata ?? false}
                pyWatcher={raw?.pyWatcher ?? false}
                coordinator={raw?.coordinatorRest ?? false}
                backendUi={status !== "reconnecting"}
            />
        </Box>
    )
}


export function HealthTopology({ sseOk, restOk, coordinatorSseOk, coordinatorRestOk }: HealthTopologyProps) {
    return (
        <Box sx={{ display: "flex", alignItems: "center", gap: 2, justifyContent: "center" }}>
            <NodeBox
                icon={<LanguageIcon fontSize="large" />}
                label="Web UI"
                healthy={true}
            />

            <Box sx={{ width: 60 }}>
                <StatusLine ok={restOk} />
                <StatusLine ok={sseOk} />

            </Box>
            <NodeBox
                icon={<DnsIcon fontSize="large" />}
                label="Backend UI"
                healthy={restOk}
            />

            <Box sx={{ width: 60 }}>
                <StatusLine ok={coordinatorRestOk} />
                <StatusLine ok={coordinatorSseOk} />
            </Box>

            <NodeBox
                icon={<HubIcon fontSize="large" />}
                label="Coordinator"
                healthy={coordinatorRestOk && coordinatorSseOk}
            />
        </Box>
    )
}


interface ServicesGridProps {
    processer: boolean
    converter: boolean
    pyMetadata: boolean
    pyWatcher: boolean
    coordinator: boolean
    backendUi: boolean
}

export function ServicesGrid({
    processer,
    converter,
    pyMetadata,
    pyWatcher,
    coordinator,
    backendUi,
}: ServicesGridProps) {
    return (
        <Grid container spacing={2} sx={{ mt: 4, justifyContent: "center" }}>
            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<AutorenewIcon fontSize="large" />}
                    label="Processer"
                    healthy={processer}
                />
            </Grid>

            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<BuildIcon fontSize="large" />}
                    label="Converter"
                    healthy={converter}
                />
            </Grid>

            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<StorageIcon fontSize="large" />}
                    label="pyMetadata"
                    healthy={pyMetadata}
                />
            </Grid>

            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<VisibilityIcon fontSize="large" />}
                    label="pyWatcher"
                    healthy={pyWatcher}
                />
            </Grid>

            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<HubIcon fontSize="large" />}
                    label="Coordinator"
                    healthy={coordinator}
                />
            </Grid>

            <Grid item xs={6} md={3}>
                <NodeBox
                    icon={<DnsIcon fontSize="large" />}
                    label="Backend UI"
                    healthy={backendUi}
                />
            </Grid>
        </Grid>
    )
}
