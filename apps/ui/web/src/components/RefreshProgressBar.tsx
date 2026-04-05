import { Box, LinearProgress, type SxProps, type Theme } from "@mui/material";
import { memo, useEffect, useState } from "react";

export const RefreshProgressBar = memo(function RefreshProgressBar({
  refreshInterval,
  isFetching,
  sx,
}: {
  refreshInterval: number;
  isFetching: boolean;
  sx?: SxProps<Theme> | undefined;
}) {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (isFetching) {
      setProgress(0);
      return;
    }

    if (refreshInterval === 0) return;

    const step = 100 / (refreshInterval * 10); // 10 updates per second

    const id = setInterval(() => {
      setProgress((p) => Math.min(100, p + step));
    }, 100);

    return () => clearInterval(id);
  }, [isFetching, refreshInterval]);

  if (refreshInterval === 0) return null;

  return (
    <Box sx={{ width: "100%", ...sx }}>
      <LinearProgress
        variant="determinate"
        value={progress}
        sx={{ height: 4, borderRadius: 2 }}
      />
    </Box>
  );
});
