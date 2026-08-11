import type { SequenceActions } from "../../types/types";
import DeleteIcon from '@mui/icons-material/DeleteOutlineOutlined';
import PauseIcon from '@mui/icons-material/PauseOutlined';
import PlayIcon from '@mui/icons-material/PlayArrowOutlined';
import { Button, IconButton, Tooltip } from "@mui/material";

interface SequenceActionButtonProps {
    action: SequenceActions;
    onClick: () => void;
    /** Velger om knappen skal være en kompakt IconButton med Tooltip, eller en full Button med tekst */
    variant?: "button" | "icon";
    /** Egendefinert tooltip-tekst. Hvis ikkes satt, brukes action-navnet */
    tooltipText?: string;
    size?: "small" | "medium" | "large";
}

export function SquenceActionbutton({
    action,
    onClick,
    variant = "button",
    tooltipText,
    size = "small"
}: SequenceActionButtonProps) {
    const { color, icon, defaultTooltip } = ((act: SequenceActions) => {
        switch (act) {
            case "Delete":
                return { color: "error" as const, icon: <DeleteIcon fontSize={size} />, defaultTooltip: "Slett" }
            case "Hold":
                return { color: "warning" as const, icon: <PauseIcon fontSize={size} />, defaultTooltip: "Sett på pause" }
            case "Release":
                return { color: "success" as const, icon: <PlayIcon fontSize={size} />, defaultTooltip: "Fortsett" }
            default:
                return { color: "inherit" as const, icon: null, defaultTooltip: act }
        }
    })(action);

    if (variant === "icon") {
        return (
            <Tooltip title={tooltipText || defaultTooltip}>
                {/* Span trengs for at Tooltip ikke skal krangle om ref hvis knappen blir disabled e.l. */}
                <span>
                    <IconButton size={size} color={color} onClick={onClick}>
                        {icon}
                    </IconButton>
                </span>
            </Tooltip>
        );
    }

    return (
        <Button color={color} endIcon={icon} variant="contained" size={size} onClick={onClick}>
            {action}
        </Button>
    );
}