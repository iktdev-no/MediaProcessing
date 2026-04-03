import { useEffect, useState } from "react";

export function PendingIcon({
  size = 32,
  color = "currentColor",
  duration = 2000,

  sandAmount = 1.0,
  topSandMaxHeight = 12,
  bottomSandMaxHeight = 12,

  dripYOffset = 11.5,
  dripHeight = 8,
}) {
  const [progress, setProgress] = useState(0);

  // running → rotating → resetting
  const [phase, setPhase] = useState<"running" | "rotating" | "resetting">(
    "running",
  );

  // -----------------------------
  // RUNNING (sand flows)
  // -----------------------------
  useEffect(() => {
    if (phase !== "running") return;

    const id = setInterval(() => {
      setProgress((p) => {
        if (p >= 1) {
          setPhase("rotating");
          return p;
        }
        return p + 0.02;
      });
    }, duration / 50);

    return () => clearInterval(id);
  }, [duration, phase]);

  // -----------------------------
  // ROTATING (freeze sand)
  // -----------------------------
  useEffect(() => {
    if (phase !== "rotating") return;

    // wait for rotation to finish
    const t = setTimeout(() => {
      setPhase("resetting");
    }, 300);

    return () => clearTimeout(t);
  }, [phase]);

  // -----------------------------
  // RESETTING (snap rotation + reset sand)
  // -----------------------------
  useEffect(() => {
    if (phase !== "resetting") return;

    // reset sand instantly
    setProgress(0);

    // next frame → running
    requestAnimationFrame(() => {
      setPhase("running");
    });
  }, [phase]);

  // -----------------------------
  // SAND VALUES
  // -----------------------------
  const topSand = sandAmount * (1 - progress);
  const bottomSand = sandAmount * progress;

  const isRotating = phase === "rotating";
  const animateSand = phase === "running"; // ONLY animate in running

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      style={{
        transform: isRotating ? "rotate(180deg)" : "rotate(0deg)",
        transition: isRotating ? "transform 0.3s ease-in-out" : "none",
      }}
    >
      <defs>
        <mask id="topMask">
          <rect x="8" y="2" width="8" height="6" fill="white" />
          <path d="M8 8 L12 12 L16 8 Z" fill="white" />
        </mask>

        <mask id="bottomMask">
          <path d="M8 16 L12 12 L16 16 Z" fill="white" />
          <rect x="8" y="16" width="8" height="6" fill="white" />
        </mask>
      </defs>

      {/* OUTLINE */}
      <path
        d="M6 2v6h.01L6 8.01 10 12l-4 4 .01.01H6V22h12v-5.99h-.01L18 16l-4-4 4-3.99-.01-.01H18V2zm10 14.5V20H8v-3.5l4-4zm-4-5-4-4V4h8v3.5z"
        fill={color}
      />

      {/* TOP SAND */}
      <rect
        x="8"
        y={2 + (1 - topSand) * topSandMaxHeight}
        width="8"
        height={topSand * topSandMaxHeight}
        fill={color}
        mask="url(#topMask)"
        style={{
          transition: animateSand ? "all 0.12s linear" : "none",
        }}
      />

      {/* BOTTOM SAND */}
      <rect
        x="8"
        y={22 - bottomSand * bottomSandMaxHeight}
        width="8"
        height={bottomSand * bottomSandMaxHeight}
        fill={color}
        mask="url(#bottomMask)"
        style={{
          transition: animateSand ? "all 0.12s linear" : "none",
        }}
      />

      {/* DRIP */}
      {phase === "running" && progress < 1 && (
        <rect
          x="11.5"
          y={dripYOffset}
          width="1"
          height={dripHeight}
          fill={color}
          opacity={0.7}
        />
      )}
    </svg>
  );
}
