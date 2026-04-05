import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";

export function usePageQuery<T>(
  key: any[],
  queryFn: () => Promise<T>,
  refreshIntervalSeconds: number,
) {
  const [active, setActive] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  // Track mount/unmount
  useEffect(() => {
    setActive(true);
    return () => setActive(false);
  }, []);

  // Setup query
  const query = useQuery({
    queryKey: key,
    queryFn,
    enabled: active,
    placeholderData: keepPreviousData,
    refetchInterval:
      active && refreshIntervalSeconds > 0
        ? refreshIntervalSeconds * 1000
        : false,
    refetchIntervalInBackground: false,
  });

  // Update lastUpdated when fetch completes
  useEffect(() => {
    if (!query.isFetching && query.data) {
      setLastUpdated(new Date());
    }
  }, [query.isFetching, query.data]);

  // Progress bar

  return {
    ...query,
    lastUpdated,
  };
}
