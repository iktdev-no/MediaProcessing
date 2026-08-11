import type { MediaType } from "../../types/types";

export function MediaTypeColor(mediaType: MediaType | undefined | null) {
    switch (mediaType) {
        case "Serie": {
            return "info"
        }
        case "Movie": {
            return "primary"
        }
        case "Subtitle": {
            return "warning"
        }
        default: {
            "default"
        }
    }
}