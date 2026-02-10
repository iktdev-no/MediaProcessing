import AutorenewIcon from "@mui/icons-material/Autorenew"
import BuildIcon from "@mui/icons-material/Build"
import type { Mode } from "../../types/transfer-model"

export function ModeIcon({ mode }: { mode: Mode }) {
    if (mode === "Auto") {
        return <AutorenewIcon sx={{ color: "#00e5ff" }} /> // neon cyan
    }

    return <BuildIcon sx={{ color: "#ffea00" }} /> // neon yellow
}
