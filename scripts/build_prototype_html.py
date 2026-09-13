"""
Builder script for prototype_feature_graphic_layout.html.
Generates an interactive, self-contained single-page prototype that allows exploring
4 radically different 1024x500 Feature Graphic layouts built with authentic widget screenshots.
"""

import base64
import os

def b64(path):
    with open(path, 'rb') as f:
        return 'data:image/png;base64,' + base64.b64encode(f.read()).decode('utf-8')

print("Encoding images...")
b64_a = b64('artifacts/prototype_variant_a.png')
b64_b = b64('artifacts/prototype_variant_b.png')
b64_c = b64('artifacts/prototype_variant_c.png')
b64_d = b64('artifacts/prototype_variant_d.png')

html_content = f'''<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CountUp — 1024x500 Feature Graphic Layout Prototype</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@400;600&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  :root {{
    --bg-main: #0F1117;
    --bg-panel: #181B24;
    --bg-card: #202430;
    --border-color: #2D3345;
    --text-primary: #F3F4F6;
    --text-muted: #9CA3AF;
    --accent-gold: #D4A574;
    --accent-vermilion: #D97642;
    --accent-sage: #4A7C59;
    --accent-indigo: #7D9BA8;
    --accent-cyan: #38BDF8;
  }}

  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    font-family: 'Plus Jakarta Sans', -apple-system, sans-serif;
    background: var(--bg-main);
    color: var(--text-primary);
    min-height: 100vh;
    padding-bottom: 110px;
    overflow-x: hidden;
  }}

  /* Top Navigation Bar */
  .topbar {{
    position: sticky;
    top: 0;
    z-index: 100;
    background: rgba(15, 17, 23, 0.92);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-color);
    padding: 14px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }}

  .brand-group {{
    display: flex;
    align-items: center;
    gap: 12px;
  }}
  .brand-badge {{
    background: rgba(212, 165, 116, 0.15);
    border: 1px solid var(--accent-gold);
    color: var(--accent-gold);
    padding: 3px 10px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    text-transform: uppercase;
  }}
  .app-title {{
    font-size: 17px;
    font-weight: 700;
    letter-spacing: -0.3px;
  }}

  .controls-group {{
    display: flex;
    align-items: center;
    gap: 12px;
  }}

  .segmented-ctrl {{
    display: flex;
    background: var(--bg-panel);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 2px;
  }}
  .seg-btn {{
    background: transparent;
    border: none;
    color: var(--text-muted);
    padding: 6px 14px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.15s ease;
  }}
  .seg-btn.active {{
    background: var(--accent-gold);
    color: #1A130B;
  }}

  .btn-toggle {{
    background: var(--bg-panel);
    border: 1px solid var(--border-color);
    color: var(--text-primary);
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }}
  .btn-toggle.active {{
    border-color: var(--accent-cyan);
    background: rgba(56, 189, 248, 0.12);
    color: var(--accent-cyan);
  }}

  /* Main Container */
  .container {{
    max-width: 1280px;
    margin: 0 auto;
    padding: 28px 24px 0;
  }}

  .view-single {{
    display: flex;
    flex-direction: column;
    align-items: center;
  }}

  /* Canvas Stage */
  .stage-wrapper {{
    position: relative;
    box-shadow: 0 24px 60px rgba(0, 0, 0, 0.7), 0 0 0 1px rgba(255, 255, 255, 0.08);
    border-radius: 12px;
    overflow: hidden;
    background: #000;
    transition: transform 0.25s ease;
    transform-origin: top center;
  }}

  .stage-img {{
    display: block;
    width: 1024px;
    height: 500px;
  }}

  /* Safe Area Overlay */
  .safe-overlay {{
    position: absolute;
    inset: 0;
    pointer-events: none;
    display: none;
    z-index: 10;
  }}
  .safe-overlay.visible {{
    display: block;
  }}
  .safe-inner {{
    position: absolute;
    top: 48px;
    left: 48px;
    right: 48px;
    bottom: 48px;
    border: 2px dashed rgba(56, 189, 248, 0.85);
    background: rgba(56, 189, 248, 0.03);
    border-radius: 8px;
  }}
  .safe-label {{
    position: absolute;
    top: 54px;
    left: 56px;
    background: rgba(56, 189, 248, 0.9);
    color: #032030;
    font-family: 'JetBrains Mono', monospace;
    font-size: 10px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    letter-spacing: 0.5px;
  }}
  .cutout-pill {{
    position: absolute;
    bottom: 54px;
    right: 56px;
    background: rgba(217, 118, 66, 0.9);
    color: #FFFFFF;
    font-family: 'JetBrains Mono', monospace;
    font-size: 10px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
  }}

  /* Variant Details Card */
  .meta-card {{
    max-width: 1024px;
    width: 100%;
    margin-top: 24px;
    background: var(--bg-panel);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 24px 28px;
  }}
  .meta-header {{
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--border-color);
    padding-bottom: 16px;
  }}
  .meta-title {{
    font-size: 20px;
    font-weight: 700;
    color: var(--accent-gold);
    display: flex;
    align-items: center;
    gap: 10px;
  }}
  .meta-badge {{
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    background: rgba(74, 124, 89, 0.2);
    border: 1px solid var(--accent-sage);
    color: #86EFAC;
    font-weight: 600;
  }}
  .meta-desc {{
    font-size: 14px;
    line-height: 1.6;
    color: #D1D5DB;
    margin-bottom: 18px;
  }}
  .feature-grid {{
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 14px;
  }}
  .feat-box {{
    background: var(--bg-card);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    padding: 12px 14px;
  }}
  .feat-box strong {{
    display: block;
    font-size: 11px;
    color: var(--accent-gold);
    text-transform: uppercase;
    letter-spacing: 0.8px;
    margin-bottom: 4px;
  }}
  .feat-box p {{
    font-size: 13px;
    color: var(--text-muted);
    line-height: 1.4;
  }}

  /* Compare Mode Grid */
  .view-compare {{
    display: none;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }}
  .compare-item {{
    background: var(--bg-panel);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }}
  .compare-head {{
    padding: 12px 16px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }}
  .compare-title {{
    font-size: 14px;
    font-weight: 700;
    color: var(--accent-gold);
  }}
  .compare-body {{
    padding: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }}
  .compare-img {{
    width: 100%;
    max-width: 512px;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
    margin-bottom: 12px;
  }}
  .compare-notes {{
    width: 100%;
    font-size: 12.5px;
    color: var(--text-muted);
    line-height: 1.5;
  }}

  /* Mobile Simulated Context */
  .view-mobile {{
    display: none;
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }}
  .phone-sim {{
    width: 360px;
    background: #1F2430;
    border: 8px solid #2D3345;
    border-radius: 36px;
    overflow: hidden;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
  }}
  .phone-header {{
    background: #11141D;
    padding: 12px 16px;
    font-size: 12px;
    font-weight: 600;
    display: flex;
    justify-content: space-between;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }}
  .phone-thumb-wrap {{
    width: 100%;
    height: 168px;
    overflow: hidden;
    background: #000;
  }}
  .phone-thumb {{
    width: 100%;
    height: auto;
    display: block;
  }}
  .phone-app-info {{
    padding: 16px;
  }}
  .phone-app-row {{
    display: flex;
    gap: 12px;
    align-items: center;
  }}
  .app-icon-squircle {{
    width: 52px;
    height: 52px;
    border-radius: 12px;
    background: #F5E6D3;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--accent-vermilion);
    font-size: 24px;
    font-weight: 700;
    border: 1px solid var(--border-color);
  }}
  .app-meta-col {{
    flex: 1;
  }}
  .app-meta-name {{
    font-size: 15px;
    font-weight: 700;
    color: #fff;
  }}
  .app-meta-cat {{
    font-size: 12px;
    color: var(--accent-sage);
    margin-top: 2px;
  }}
  .app-meta-stars {{
    font-size: 11px;
    color: var(--accent-gold);
    margin-top: 2px;
  }}

  /* Floating Bottom Switcher (Prototype Skill Standard) */
  .floating-switcher {{
    position: fixed;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(24, 27, 36, 0.95);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(212, 165, 116, 0.4);
    box-shadow: 0 12px 36px rgba(0, 0, 0, 0.6), 0 0 20px rgba(212, 165, 116, 0.15);
    border-radius: 999px;
    padding: 6px 12px;
    display: flex;
    align-items: center;
    gap: 14px;
    z-index: 1000;
  }}
  .switcher-arrow {{
    background: rgba(255, 255, 255, 0.08);
    border: none;
    color: var(--text-primary);
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 15px;
    font-weight: 700;
    transition: all 0.15s ease;
  }}
  .switcher-arrow:hover {{
    background: var(--accent-gold);
    color: #1A130B;
  }}
  .switcher-label-wrap {{
    display: flex;
    align-items: center;
    gap: 10px;
    user-select: none;
  }}
  .variant-pill {{
    background: var(--accent-gold);
    color: #1A130B;
    font-size: 11px;
    font-weight: 800;
    padding: 3px 8px;
    border-radius: 999px;
    letter-spacing: 0.5px;
  }}
  .variant-name {{
    font-size: 13.5px;
    font-weight: 700;
    color: #FFFFFF;
    white-space: nowrap;
  }}
  .variant-hint {{
    font-size: 11px;
    color: var(--text-muted);
    font-family: 'JetBrains Mono', monospace;
  }}
</style>
</head>
<body>

<!-- Sticky Header -->
<header class="topbar">
  <div class="brand-group">
    <span class="brand-badge">Play Store ASO Studio</span>
    <span class="app-title">CountUp · 1024×500 Feature Graphic Prototype</span>
  </div>

  <div class="controls-group">
    <!-- View Switcher -->
    <div class="segmented-ctrl">
      <button class="seg-btn active" onclick="setViewMode('single')">Single Focus</button>
      <button class="seg-btn" onclick="setViewMode('compare')">Compare All (4)</button>
      <button class="seg-btn" onclick="setViewMode('mobile')">Mobile Preview (320px)</button>
    </div>

    <!-- Zoom Switcher -->
    <div class="segmented-ctrl" id="zoomCtrl">
      <button class="seg-btn" onclick="setZoom(0.65)">65%</button>
      <button class="seg-btn" onclick="setZoom(0.85)">85%</button>
      <button class="seg-btn active" onclick="setZoom(1.0)">100%</button>
    </div>

    <!-- Safe Area Toggle -->
    <button class="btn-toggle" id="safeAreaToggle" onclick="toggleSafeArea()">
      <span>🛡️</span> Safe Margins (48px)
    </button>
  </div>
</header>

<main class="container">

  <!-- 1. Single Focus View -->
  <section class="view-single" id="viewSingle">
    <div class="stage-wrapper" id="stageWrapper">
      <img id="stageImg" class="stage-img" src="{b64_a}" alt="Feature Graphic 1024x500">
      <div class="safe-overlay" id="safeOverlay">
        <div class="safe-inner"></div>
        <div class="safe-label">GOOGLE PLAY SAFE ZONE (928 × 404 px)</div>
        <div class="cutout-pill">48px EDGE PADDING</div>
      </div>
    </div>

    <!-- Detailed Metadata Card -->
    <article class="meta-card" id="metaCard">
      <div class="meta-header">
        <div>
          <h2 class="meta-title" id="metaTitle">Variant A: The Zen Dyad & Companion Card</h2>
          <span style="font-size: 12px; color: var(--text-muted); margin-top: 4px; display: block;" id="metaSubtitle">
            Dark Mode Zen Horizon (4×1) + Zen Pebble (1×1) + In-App 3D Tactile Card
          </span>
        </div>
        <span class="meta-badge" id="metaRating">ASO Recommended · Grade A+</span>
      </div>

      <p class="meta-desc" id="metaDesc">
        A balanced Yin-Yang composition combining real Dark Mode home-screen widgets with a light washi paper in-app card. 
        Shows shoppers the complete app experience: home screen glanceability alongside the tactile in-app detail card.
      </p>

      <div class="feature-grid" id="metaGrid">
        <div class="feat-box">
          <strong>Hero Element</strong>
          <p id="featHero">4×1 Zen Horizon Dark Mode (Sobriety 100 Days)</p>
        </div>
        <div class="feat-box">
          <strong>Glanceable Token</strong>
          <p id="featToken">1×1 Zen Pebble Dark Mode (100 d •)</p>
        </div>
        <div class="feat-box">
          <strong>Context Surface</strong>
          <p id="featSurface">In-App 3D Tactile Card (Meditation 170 days)</p>
        </div>
        <div class="feat-box">
          <strong>Philosophy</strong>
          <p id="featPhil">Kenya Hara "Ma" (Negative Space) + Dieter Rams Honesty</p>
        </div>
      </div>
    </article>
  </section>

  <!-- 2. Compare All View -->
  <section class="view-compare" id="viewCompare">
    <div class="compare-item">
      <div class="compare-head">
        <span class="compare-title">Variant A: Zen Dyad & Companion Card</span>
        <button class="seg-btn active" onclick="selectVariant('A')">Select</button>
      </div>
      <div class="compare-body">
        <img class="compare-img" src="{b64_a}" alt="Variant A">
        <div class="compare-notes">
          <strong>Composition:</strong> 4×1 Dark Horizon (top) + 1×1 Dark Pebble (left) + In-App 3D Card (right).<br>
          <strong>Verdict:</strong> Maximum conversion balance — establishes widget mastery while revealing in-app depth.
        </div>
      </div>
    </div>

    <div class="compare-item">
      <div class="compare-head">
        <span class="compare-title">Variant B: The Zen Widget Trinity</span>
        <button class="seg-btn" onclick="selectVariant('B')">Select</button>
      </div>
      <div class="compare-body">
        <img class="compare-img" src="{b64_b}" alt="Variant B">
        <div class="compare-notes">
          <strong>Composition:</strong> 100% Home Widgets: 4×1 Dark + 1×1 Dark + 3×2 Multi-Item Grid.<br>
          <strong>Verdict:</strong> Pure launcher powerhouse. Ideal if the marketing message is exclusively "the ultimate widget app".
        </div>
      </div>
    </div>

    <div class="compare-item">
      <div class="compare-head">
        <span class="compare-title">Variant C: In-Situ Mobile Stage</span>
        <button class="seg-btn" onclick="selectVariant('C')">Select</button>
      </div>
      <div class="compare-body">
        <img class="compare-img" src="{b64_c}" alt="Variant C">
        <div class="compare-notes">
          <strong>Composition:</strong> Android handset mockup in center-right + enlarged 1×1 tile + card snippet.<br>
          <strong>Verdict:</strong> Visceral real-phone context; immediately shows how widgets sit on actual launcher wallpaper.
        </div>
      </div>
    </div>

    <div class="compare-item">
      <div class="compare-head">
        <span class="compare-title">Variant D: Nocturnal Ink Stage</span>
        <button class="seg-btn" onclick="selectVariant('D')">Select</button>
      </div>
      <div class="compare-body">
        <img class="compare-img" src="{b64_d}" alt="Variant D">
        <div class="compare-notes">
          <strong>Composition:</strong> Deep Sumi ink-stone pedestal framing the dark widgets with gold hairlines.<br>
          <strong>Verdict:</strong> High-contrast museum aesthetic; emphasizes craftsmanship and dark mode luxury.
        </div>
      </div>
    </div>
  </section>

  <!-- 3. Mobile Simulated View -->
  <section class="view-mobile" id="viewMobile">
    <div class="phone-sim">
      <div class="phone-header">
        <span>Google Play Store</span>
        <span>9:41 AM</span>
      </div>
      <div class="phone-thumb-wrap">
        <img id="mobileThumb" class="phone-thumb" src="{b64_a}" alt="Mobile Preview">
      </div>
      <div class="phone-app-info">
        <div class="phone-app-row">
          <div class="app-icon-squircle">〇</div>
          <div class="app-meta-col">
            <div class="app-meta-name">CountUp — Mindful Days</div>
            <div class="app-meta-cat">Productivity · Widgets · Offline</div>
            <div class="app-meta-stars">★ 4.9 · 100% Private</div>
          </div>
        </div>
      </div>
    </div>
    <p style="font-size: 13px; color: var(--text-muted); text-align: center; max-width: 480px;">
      Simulating the 320px mobile Play Store product listing. Notice how the Enso emblem, the bold "100 DAYS" odometer numeral, and the 24 Solar Terms capsule remain razor-sharp and legible at small scale.
    </p>
  </section>

</main>

<!-- Floating Switcher Bar (Prototype Skill Standard) -->
<nav class="floating-switcher" id="floatingSwitcher">
  <button class="switcher-arrow" onclick="cycleVariant(-1)" title="Previous Variant (←)">←</button>
  <div class="switcher-label-wrap">
    <span class="variant-pill" id="switcherPill">VARIANT A</span>
    <span class="variant-name" id="switcherName">Zen Dyad & Companion Card</span>
    <span class="variant-hint">[Press ← / →]</span>
  </div>
  <button class="switcher-arrow" onclick="cycleVariant(1)" title="Next Variant (→)">→</button>
</nav>

<script>
  const VARIANTS = {{
    'A': {{
      name: 'Zen Dyad & Companion Card',
      title: 'Variant A: The Zen Dyad & Companion Card',
      subtitle: '4×1 Zen Horizon Dark Mode + 1×1 Zen Pebble Dark Mode + In-App 3D Card',
      rating: 'ASO Recommended · Grade A+',
      desc: 'A balanced Yin-Yang composition combining real Dark Mode home-screen widgets with a light washi paper in-app card. Shows shoppers the complete app experience: home screen glanceability alongside the tactile in-app detail card.',
      hero: '4×1 Zen Horizon Dark Mode (Sobriety 100 Days)',
      token: '1×1 Zen Pebble Dark Mode (100 d •)',
      surface: 'In-App 3D Tactile Card (Meditation 170 days)',
      phil: 'Kenya Hara "Ma" (Negative Space) + Dieter Rams Honesty',
      img: '{b64_a}'
    }},
    'B': {{
      name: 'The Zen Widget Trinity',
      title: 'Variant B: The Zen Widget Trinity',
      subtitle: '100% Home Screen Focus: 4×1 Dark Horizon + 1×1 Dark Pebble + 3×2 Multi-Item Grid',
      rating: 'Widget Specialist · Grade A',
      desc: 'Every single element on the right is an authentic home-screen widget. Demonstrates all three core CountUp widget form-factors (4×1 single milestone, 1×1 compact pebble, 3×2 multi-item grid) across dark and light modes.',
      hero: '4×1 Zen Horizon Dark Mode (Sobriety 100 Days)',
      token: '1×1 Zen Pebble Dark Mode (100 d •)',
      surface: '3×2 Multi-Item Zen Widget (Haircut, Coffee, Gym...)',
      phil: 'Matias Duarte Android Widget Suite Supremacy',
      img: '{b64_b}'
    }},
    'C': {{
      name: 'In-Situ Mobile Stage',
      title: 'Variant C: The In-Situ Mobile Stage',
      subtitle: 'Elevated Android Pixel Handset + Enlarged 1×1 Pebble Tile + In-App Chip',
      rating: 'Contextual Reality · Grade A',
      desc: 'Places an authentic Android handset on the right to show how the widgets live in-situ on a real smartphone launcher, paired with prominent enlarged detail tokens.',
      hero: 'Full Android Pixel Handset with live launcher wallpaper',
      token: 'High-Res 1×1 Zen Pebble Tile (100 d •)',
      surface: 'Floating In-App Milestone Note Chip',
      phil: 'Tangible Context & Mobile Device Elevation',
      img: '{b64_c}'
    }},
    'D': {{
      name: 'Nocturnal Ink Stage',
      title: 'Variant D: The Nocturnal Ink Stage',
      subtitle: 'Deep Sumi Ink-Stone Pedestal Framing Dark Mode Widgets & Gold Hairlines',
      rating: 'Luxury Minimalist · Grade A-',
      desc: 'Draws a dark ink-stone slate stage on the right, providing an architectural nocturnal gallery pedestal that makes the dark widgets and gold milestones glow.',
      hero: 'Dark Mode 4×1 Widget on Charcoal Slate',
      token: '1×1 Zen Pebble with Gold Accent Dot',
      surface: 'In-App Card on Deep Ink Surface',
      phil: 'Classical Literati Ink Stone (端砚) Aesthetic',
      img: '{b64_d}'
    }}
  }};

  const KEYS = ['A', 'B', 'C', 'D'];
  let currentKey = 'A';
  let isSafeArea = false;

  // Initialize from URL search param if present (?variant=)
  function initFromUrl() {{
    const params = new URLSearchParams(window.location.search);
    const v = params.get('variant');
    if (v && KEYS.includes(v.toUpperCase())) {{
      currentKey = v.toUpperCase();
    }}
    applyVariant(currentKey, false);
  }}

  function selectVariant(key) {{
    currentKey = key;
    applyVariant(key, true);
    setViewMode('single');
  }}

  function cycleVariant(delta) {{
    let idx = KEYS.indexOf(currentKey);
    idx = (idx + delta + KEYS.length) % KEYS.length;
    currentKey = KEYS[idx];
    applyVariant(currentKey, true);
  }}

  function applyVariant(key, updateUrl) {{
    const data = VARIANTS[key];
    if (!data) return;

    // Update Images
    document.getElementById('stageImg').src = data.img;
    document.getElementById('mobileThumb').src = data.img;

    // Update Switcher
    document.getElementById('switcherPill').textContent = 'VARIANT ' + key;
    document.getElementById('switcherName').textContent = data.name;

    // Update Metadata Card
    document.getElementById('metaTitle').textContent = data.title;
    document.getElementById('metaSubtitle').textContent = data.subtitle;
    document.getElementById('metaRating').textContent = data.rating;
    document.getElementById('metaDesc').textContent = data.desc;
    document.getElementById('featHero').textContent = data.hero;
    document.getElementById('featToken').textContent = data.token;
    document.getElementById('featSurface').textContent = data.surface;
    document.getElementById('featPhil').textContent = data.phil;

    if (updateUrl) {{
      const url = new URL(window.location);
      url.searchParams.set('variant', key);
      window.history.replaceState({{}}, '', url);
    }}
  }}

  function setViewMode(mode) {{
    const single = document.getElementById('viewSingle');
    const compare = document.getElementById('viewCompare');
    const mobile = document.getElementById('viewMobile');
    const zoomCtrl = document.getElementById('zoomCtrl');

    document.querySelectorAll('.segmented-ctrl .seg-btn').forEach(btn => {{
      if (btn.textContent.toLowerCase().includes(mode.substring(0, 4))) {{
        btn.classList.add('active');
      }} else {{
        btn.classList.remove('active');
      }}
    }});

    single.style.display = 'none';
    compare.style.display = 'none';
    mobile.style.display = 'none';

    if (mode === 'single') {{
      single.style.display = 'flex';
      zoomCtrl.style.display = 'flex';
    }} else if (mode === 'compare') {{
      compare.style.display = 'grid';
      zoomCtrl.style.display = 'none';
    }} else if (mode === 'mobile') {{
      mobile.style.display = 'flex';
      zoomCtrl.style.display = 'none';
    }}
  }}

  function setZoom(scale) {{
    const wrapper = document.getElementById('stageWrapper');
    wrapper.style.transform = `scale(${{scale}})`;
    document.querySelectorAll('#zoomCtrl .seg-btn').forEach(btn => {{
      btn.classList.toggle('active', btn.textContent.includes(Math.round(scale * 100) + '%'));
    }});
  }}

  function toggleSafeArea() {{
    isSafeArea = !isSafeArea;
    document.getElementById('safeOverlay').classList.toggle('visible', isSafeArea);
    document.getElementById('safeAreaToggle').classList.toggle('active', isSafeArea);
  }}

  // Keyboard navigation (← / → arrow keys per Prototype skill)
  window.addEventListener('keydown', (e) => {{
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) return;
    if (e.key === 'ArrowLeft') cycleVariant(-1);
    if (e.key === 'ArrowRight') cycleVariant(1);
    if (e.key === 's' || e.key === 'S') toggleSafeArea();
  }});

  initFromUrl();
</script>
</body>
</html>
'''

out_file = "prototype_feature_graphic_layout.html"
with open(out_file, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"Successfully created self-contained prototype: {out_file} ({os.path.getsize(out_file)} bytes)")
