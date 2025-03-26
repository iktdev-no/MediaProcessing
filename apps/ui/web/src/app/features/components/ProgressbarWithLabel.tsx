import * as React from 'react';
import LinearProgress, { LinearProgressProps } from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

export default function ProgressbarWithLabel({indeterminateText, progress}: {indeterminateText?: string, progress?: number | undefined | null}) {
  const variant = (progress) ? "determinate" : "indeterminate"
  return (
    <Box sx={{ display: 'flex', alignItems: 'center' }}>
      <Box sx={{ width: '100%', mr: 1 }}>
        <LinearProgress variant={variant} value={progress ?? 0} />
      </Box>
      <Box sx={{ minWidth: 35 }}>
        <Typography
          variant="body2"
          sx={{ color: 'text.secondary' }}>
            {progress ? (`${Math.round(progress)}%`) :
            indeterminateText}
            
          </Typography>
      </Box>
    </Box>
  );
}

/*export default function LinearWithValueLabel() {
  const [progress, setProgress] = React.useState(10);

  React.useEffect(() => {
    const timer = setInterval(() => {
      setProgress((prevProgress) => (prevProgress >= 100 ? 10 : prevProgress + 10));
    }, 800);
    return () => {
      clearInterval(timer);
    };
  }, []);

  return (
    <Box sx={{ width: '100%' }}>
      <ProgressbarWithLabel value={progress} />
    </Box>
  );
}*/