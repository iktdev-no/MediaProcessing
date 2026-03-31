import type { CoordinatorPreference } from "../../../types/transfer-model";

export function ProcessingTab({
  prefs,
  setPrefs,
}: {
  prefs: CoordinatorPreference;
  setPrefs: (p: CoordinatorPreference) => void;
}) {}
