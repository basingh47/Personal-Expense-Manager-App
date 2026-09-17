import subprocess

svg_content = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108">
  <defs>
    <!-- Background Linear Gradient -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1B2A4A"/>
      <stop offset="100%" stop-color="#0B132B"/>
    </linearGradient>

    <!-- Radial Aura Glow -->
    <radialGradient id="auraGlow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#00E676" stop-opacity="0.35"/>
      <stop offset="100%" stop-color="#00E676" stop-opacity="0"/>
    </radialGradient>

    <!-- Shield Blue Gradient -->
    <linearGradient id="shieldGrad" x1="28%" y1="20%" x2="72%" y2="80%">
      <stop offset="0%" stop-color="#2196F3"/>
      <stop offset="100%" stop-color="#0D47A1"/>
    </linearGradient>

    <!-- Gold Coin 1 Gradient -->
    <linearGradient id="coinGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFD700"/>
      <stop offset="100%" stop-color="#FF8F00"/>
    </linearGradient>

    <!-- Gold Coin 2 Gradient -->
    <linearGradient id="coinGrad2" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFF9C4"/>
      <stop offset="30%" stop-color="#FFD700"/>
      <stop offset="100%" stop-color="#FF8F00"/>
    </linearGradient>
  </defs>

  <!-- 1. Background Rectangle -->
  <rect width="108" height="108" fill="url(#bgGrad)"/>

  <!-- 2. Grid lines -->
  <g stroke="#253A68" stroke-width="0.5" opacity="0.6">
    <line x1="9" y1="0" x2="9" y2="108"/>
    <line x1="19" y1="0" x2="19" y2="108"/>
    <line x1="29" y1="0" x2="29" y2="108"/>
    <line x1="39" y1="0" x2="39" y2="108"/>
    <line x1="49" y1="0" x2="49" y2="108"/>
    <line x1="59" y1="0" x2="59" y2="108"/>
    <line x1="69" y1="0" x2="69" y2="108"/>
    <line x1="79" y1="0" x2="79" y2="108"/>
    <line x1="89" y1="0" x2="89" y2="108"/>
    <line x1="99" y1="0" x2="99" y2="108"/>

    <line x1="0" y1="9" x2="108" y2="9"/>
    <line x1="0" y1="19" x2="108" y2="19"/>
    <line x1="0" y1="29" x2="108" y2="29"/>
    <line x1="0" y1="39" x2="108" y2="39"/>
    <line x1="0" y1="49" x2="108" y2="49"/>
    <line x1="0" y1="59" x2="108" y2="59"/>
    <line x1="0" y1="69" x2="108" y2="69"/>
    <line x1="0" y1="79" x2="108" y2="79"/>
    <line x1="0" y1="89" x2="108" y2="89"/>
    <line x1="0" y1="99" x2="108" y2="99"/>
  </g>

  <!-- 3. Glowing background aura -->
  <circle cx="54" cy="54" r="30" fill="url(#auraGlow)"/>

  <!-- 4. Main Wealth & Security Shield -->
  <path d="M54,22 L78,31 L78,54 C78,69.5 67.5,77 54,81 C40.5,77 30,69.5 30,54 L30,31 Z" fill="url(#shieldGrad)"/>

  <!-- 5. Inner shield gold trim border -->
  <path d="M54,25 L75,33 L75,54 C75,67 66,73.5 54,77 C42,73.5 33,67 33,54 L33,33 Z" fill="none" stroke="#FFD700" stroke-width="1.5"/>

  <!-- 6. Background Gold Coin -->
  <circle cx="46" cy="55" r="10" fill="url(#coinGrad1)"/>

  <!-- 7. Foreground Gold Coin -->
  <circle cx="58" cy="58" r="11" fill="url(#coinGrad2)"/>
  <!-- Dollar Symbol on foreground coin -->
  <path d="M58,52 L58,64 M55,54 C52,54 52,58 55,58 C58,58 58,62 61,62 C64,62 64,58 61,58" fill="none" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>

  <!-- 8. Trend Upward Arrow (Neon Emerald) -->
  <path d="M34,66 C42,60 48,64 56,52 C62,43 70,36 76,34" fill="none" stroke="#00E676" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M68,34 L76,34 L76,42" fill="none" stroke="#00E676" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"/>

  <!-- White highlight line inside arrow -->
  <path d="M34,66 C42,60 48,64 56,52 C62,43 70,36 76,34" fill="none" stroke="#FFFFFF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M68,34 L76,34 L76,42" fill="none" stroke="#FFFFFF" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>

  <!-- 9. Accent Stars / Sparkles -->
  <path d="M78,24 L80,26 L82,24 L80,22 Z" fill="#FFFFFF"/>
  <path d="M40,36 L41,37 L42,36 L41,35 Z" fill="#FFFFFF"/>
</svg>
"""

with open("app_store_icon_512.svg", "w") as f:
    f.write(svg_content)

# Convert SVG to 512x512 PNG using FFmpeg (with librsvg)
cmd = [
    "ffmpeg",
    "-y",
    "-i", "app_store_icon_512.svg",
    "-vf", "scale=512:512",
    "app_store_icon_512.png"
]
subprocess.run(cmd, check=True)

# Also copy as play_store_icon_512.png and to output directories
subprocess.run(["cp", "app_store_icon_512.png", "play_store_icon_512.png"], check=True)
subprocess.run(["cp", "app_store_icon_512.png", ".build-outputs/app_store_icon_512.png"], check=True)
subprocess.run(["cp", "app_store_icon_512.png", ".build-outputs/play_store_icon_512.png"], check=True)
subprocess.run(["cp", "app_store_icon_512.png", "app/src/main/res/drawable/app_store_icon_512.png"], check=True)
print("Successfully generated 512x512 app store PNG icons!")
