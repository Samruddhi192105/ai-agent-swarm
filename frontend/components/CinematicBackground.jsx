"use client";

import { useEffect, useRef } from "react";

const VIDEO_URL =
  "https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260729_102822_0e6c87e8-c141-4744-bf32-ad30db296371.mp4";

export default function CinematicBackground() {
  const videoRef = useRef(null);

  useEffect(() => {
    const video = videoRef.current;

    if (!video) return;

    video.muted = true;
    video.playsInline = true;
    video.loop = true;

    const startVideo = async () => {
      try {
        await video.play();
      } catch (error) {
        console.log("Autoplay was blocked:", error);
      }
    };

    if (video.readyState >= 2) {
      startVideo();
    } else {
      video.addEventListener("canplay", startVideo, { once: true });
    }

    return () => {
      video.removeEventListener("canplay", startVideo);
    };
  }, []);

  return (
    <div
      aria-hidden="true"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 0,
        background: "#0a0a0a",
        overflow: "hidden",
        pointerEvents: "none",
      }}
    >
      <video
        ref={videoRef}
        src={VIDEO_URL}
        autoPlay
        muted
        loop
        playsInline
        preload="auto"
        style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
            objectFit: "cover",
            display: "block",
            filter: "brightness(1.15) contrast(1.05)",
            }}
      />

      {/* Dark cinematic overlay */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
              "linear-gradient(to bottom, rgba(0,0,0,0.05), rgba(0,0,0,0.20))",
        }}
      />
    </div>
  );
}