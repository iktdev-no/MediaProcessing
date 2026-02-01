import InfoIcon from "@mui/icons-material/Info"
import { Button } from "@mui/material"

export function DetailsButton({ onClick }: { onClick: () => void }) {
    return (
        <Button
            variant="outlined"
            size="small"
            onClick={onClick}
            startIcon={<InfoIcon />}
            sx={{
                textTransform: "none",
                borderColor: "primary.main",
                color: "primary.main",
                paddingY: 0.25,
                paddingX: 1,
                minHeight: "28px",
            }}
        >
            Detaljer
        </Button>
    )
}
