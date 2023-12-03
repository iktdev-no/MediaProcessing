import { Typography } from "@mui/material";


export function UnixTimestamp({ timestamp }: { timestamp?: number }) {
    if (!timestamp) {
        return null;
    }

    const date = new Date(timestamp);
    const day = date.getDate();
    const month = date.toLocaleString('default', { month: 'short' });
    const year = date.getFullYear();
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return (
        <>
            <Typography variant='body1'>
                {day}.{month}.{year} {hours}.{minutes}
            </Typography>
        </>
    )
}
