import { useEffect, useState } from "react";

export function useRefreshProgress(
  refreshInterval: number,
  isFetching: boolean,
) {
  const [progress, setProgress] = useState<number | "indeterminate" | null>(
    null,
  );

  useEffect(() => {
    if (refreshInterval === 0) {
      setProgress(null);
      return;
    }

    if (isFetching) {
      setProgress(0);
      //setProgress("indeterminate");
      return;
    }

    // Fetch er ferdig → start countdown
    setProgress(0);

    const totalMs = refreshInterval * 1000;
    const start = Date.now();

    const id = setInterval(() => {
      const elapsed = Date.now() - start;
      const pct = Math.min(100, (elapsed / totalMs) * 100);
      setProgress(pct);
    }, 100);

    return () => clearInterval(id);
  }, [refreshInterval, isFetching]);

  return progress;
}
