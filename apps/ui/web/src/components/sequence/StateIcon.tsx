import PauseCircleIcon from "@mui/icons-material/PauseCircle"
import PlayCircleIcon from "@mui/icons-material/PlayCircle"
import type { CurrentState } from "../../types/transfer-model"

export function StateIcon({ state }: { state: CurrentState }) {
    switch (state) {
        case "Continuing":
            return <PlayCircleIcon sx={{ color: "#00ff6a" }} /> // neon green

        case "OnHold":
            return <PauseCircleIcon sx={{ color: "#ff0066" }} /> // neon red/pink

        default:
            return null
    }
}
