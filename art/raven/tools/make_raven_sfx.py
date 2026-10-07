"""Build raven calls, hurt/death voices and original movement sounds.

Requires numpy and ffmpeg (PATH, --ffmpeg, or the imageio-ffmpeg package).
The original recording is kept in art/raven/audio/source for offline rebuilding.
See art/raven/audio/README.md and the packaged CREDITS.md for its licence.
"""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import shutil
import subprocess

import numpy as np

REPO = Path(__file__).resolve().parents[3]
SOURCE = REPO / "art/raven/audio/source/common-raven.ogg"
SOURCE_SHA1 = "5510722e428280528775b45c28f6f38c281fc761"
OUTPUT = REPO / "src/main/resources/assets/maledict/sounds/entity/raven"
RATE = 44100
# Complete, separated calls; avoid the noisier overlapping calls later in the take.
CALLS = ((0.30, 1.05), (2.05, 2.80), (12.15, 13.05))


def voice(samples: np.ndarray, start: float, end: float, pitch: float = 1.0) -> np.ndarray:
    clip = samples[round(start * RATE):round(end * RATE)].copy()
    if pitch == 1.0:
        return clip;
    return np.interp(np.arange(0, len(clip) - 1, pitch), np.arange(len(clip)), clip)


def movement(kind: str, variant: int) -> np.ndarray:
    # Original procedural foley; no extra recording or licence dependency.
    rng = np.random.default_rng(5100 + variant)
    duration = 0.16 if kind == "step" else 0.30
    t = np.arange(round(duration * RATE)) / RATE
    noise = rng.normal(0, 1, len(t))
    spectrum = np.fft.rfft(noise)
    hz = np.fft.rfftfreq(len(noise), 1 / RATE)
    spectrum *= (1 - np.exp(-(hz / 250) ** 2)) * np.exp(-(hz / (3500 if kind == "step" else 1800)) ** 2)
    rustle = np.fft.irfft(spectrum, n=len(t))
    if kind == "step":
        # A light claw tap with a brief scrape; keep it much quieter than the voice.
        return (rustle * np.exp(-t * 65)
                + 0.5 * np.sin(2 * np.pi * (650 + variant * 90) * t) * np.exp(-t * 95))
    # One feathered downstroke, with a soft trailing brush.
    envelope = np.sin(np.pi * t / duration) ** 2
    return rustle * envelope * (0.7 + 0.3 * np.cos(2 * np.pi * 7 * t))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ffmpeg", help="Path to an existing ffmpeg executable")
    args = parser.parse_args()
    ffmpeg = args.ffmpeg or shutil.which("ffmpeg")
    if not ffmpeg:
        import imageio_ffmpeg
        ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    if hashlib.sha1(SOURCE.read_bytes()).hexdigest() != SOURCE_SHA1:
        raise ValueError("Original recording does not match the documented source")

    decoded = subprocess.check_output([
        ffmpeg, "-v", "error", "-i", str(SOURCE), "-ac", "1", "-ar", str(RATE),
        "-af", "highpass=f=120", "-f", "f32le", "-",
    ])
    samples = np.frombuffer(decoded, dtype="<f4")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    clips = {f"call_{i}": (voice(samples, start, end), -3, 0.06)
             for i, (start, end) in enumerate(CALLS, 1)}
    # Short, sharp distress responses; death has a lower, longer trailing croak.
    clips.update({
        "hurt_1": (voice(samples, 0.30, 1.05, 1.12), -3, 0.045),
        "hurt_2": (voice(samples, 2.05, 2.80, 1.08), -3, 0.045),
        "death_1": (voice(samples, 0.30, 1.05, 0.78), -4, 0.22),
        "death_2": (voice(samples, 12.15, 13.05, 0.82), -4, 0.25),
    })
    for kind in ("step", "fly"):
        for variant in (1, 2):
            clips[f"{kind}_{variant}"] = (movement(kind, variant), -12 if kind == "step" else -9, 0.04)
    for name, (call, peak_db, fade_seconds) in clips.items():
        call *= (10 ** (peak_db / 20)) / np.max(np.abs(call))
        fade_in, fade_out = round(0.01 * RATE), round(fade_seconds * RATE)
        call[:fade_in] *= np.linspace(0, 1, fade_in)
        call[-fade_out:] *= np.linspace(1, 0, fade_out)
        path = OUTPUT / f"{name}.ogg"
        subprocess.run([
            ffmpeg, "-v", "error", "-y", "-f", "f32le", "-ar", str(RATE),
            "-ac", "1", "-i", "-", "-c:a", "libvorbis", "-q:a", "5",
            "-map_metadata", "-1", str(path),
        ], input=call.astype("<f4").tobytes(), check=True)
        # Decode the shipped file to check format, duration and clipping after encoding.
        audio = subprocess.check_output([
            ffmpeg, "-v", "error", "-i", str(path), "-f", "f32le", "-",
        ])
        peak = float(np.max(np.abs(np.frombuffer(audio, dtype="<f4"))))
        if peak >= 1:
            raise ValueError(f"Clipping in {path}")
        print(f"{path.relative_to(REPO)}: {len(call) / RATE:.2f}s, mono, 44100 Hz, peak {peak:.3f}")


if __name__ == "__main__":
    main()
