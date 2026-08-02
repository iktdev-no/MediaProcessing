import { useEffect, useState } from "react";
import type { CoordinatorPreference } from "../../types/types";

export function useCoordinatorPreferences() {
  const [prefs, setPrefs] = useState<CoordinatorPreference | null>(null);
  const [original, setOriginal] = useState<CoordinatorPreference | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/preferences")
      .then((r) => r.json())
      .then((data) => {
        setPrefs(data);
        setOriginal(JSON.parse(JSON.stringify(data)));
        setLoading(false);
      });
  }, []);

  const isDirty = JSON.stringify(prefs) !== JSON.stringify(original);

  const save = () => {
    if (!prefs) return;
    fetch("/api/preferences", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(prefs),
    }).then(() => {
      setOriginal(JSON.parse(JSON.stringify(prefs)));
    });
  };

  const reset = () => {
    fetch("/api/preferences/defaults")
      .then((r) => r.json())
      .then((data) => setPrefs(data));
  };

  return { prefs, setPrefs, save, reset, isDirty, loading };
}
