"""
Master Google Play Store Feature Graphic Generator (1024 x 500 px) for CountUp.
Synthesizes:
- Pure authentic Android screenshots:
  1. Real 4×1 Zen Horizon Widget in Dark Mode (SOBRIETY STREAK 100 DAYS).
  2. Real 1×1 Zen Pebble Widget in Dark Mode (100 d • SOBRIETY STREAK).
  3. Real In-App 3D Tactile Card (MEDITATION & ZEN BREATHING 170 days).
- 100% real product reality (zero generative / artificial mockups).
- Washi paper foundation (#F5E6D3), Chinese Shanshui ink wash contours, and Solitary Sail.
- Marcellus Roman serif typography and dynamic 24 Solar Terms capsule.
- Uncluttered serene negative space ("Ma") adhering strictly to Kenya Hara / Dieter Rams philosophy.
"""

import math
import os
import random
import shutil
from PIL import Image, ImageDraw, ImageFilter, ImageFont

random.seed(108)  # Harmonious seed

WIDTH = 1024
HEIGHT = 500

# Color Tokens strictly derived from ZenTheme.kt and WidgetThemeTokens.kt
COLOR_PAPER_BG = (245, 230, 211)       # ZenPaperBackground #F5E6D3
COLOR_PAPER_SURFACE = (235, 220, 195)  # ZenPaperSurface #EBDCC3
COLOR_CARD_WHITE = (255, 255, 255)     # ZenPaperCard #FFFFFF
COLOR_INK_BLACK = (44, 36, 22)         # ZenInkBlack #2C2416
COLOR_INK_MUTED = (107, 93, 79)        # ZenInkMuted #6B5D4F
COLOR_HAIRLINE = (227, 211, 184)       # ZenHairlineRule #E3D3B8
COLOR_HAIRLINE_VAR = (217, 198, 166)   # ZenHairlineRuleVariant #D9C6A6
COLOR_VERMILION = (217, 118, 66)       # ZenVermilion #D97642 (Terracotta 朱砂)
COLOR_SAGE = (74, 124, 89)             # ZenSage #4A7C59 (Willow Sage 柳绿)
COLOR_OCHRE = (212, 165, 116)          # ZenOchre #D4A574 (Ochre Gold 赭石)
COLOR_INDIGO = (125, 155, 168)         # ZenIndigo #7D9BA8 (Dusty Indigo 黛蓝)
COLOR_SEAL_INK = (90, 77, 65)          # ZenSealInk #5A4D41

FONT_MARCELLUS = "app/src/main/res/font/marcellus_regular.ttf"
FONT_NOTO_ITALIC = "app/src/main/res/font/noto_serif_italic.ttf"
FONT_SANS_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FONT_SANS_REG = "C:/Windows/Fonts/segoeui.ttf"
FONT_NOTO_SERIF_SC = "C:/Windows/Fonts/NotoSerifSC-VF.ttf"


def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()


def draw_centered_text(draw, box, text, font, fill):
    """Draw text strictly centered within a (x1, y1, x2, y2) bounding box."""
    x1, y1, x2, y2 = box
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = x1 + (x2 - x1 - tw) / 2 - bbox[0]
    ty = y1 + (y2 - y1 - th) / 2 - bbox[1]
    draw.text((tx, ty), text, font=font, fill=fill)


def create_washi_paper_canvas():
    """Build a multi-layered warm paper canvas with organic washi fiber noise."""
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), COLOR_PAPER_BG + (255,))
    
    noise = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    n_draw = ImageDraw.Draw(noise)
    for _ in range(16000):
        x = random.randint(0, WIDTH - 1)
        y = random.randint(0, HEIGHT - 1)
        tone = random.choice([255, 235, 215, 195])
        alpha = random.randint(4, 11)
        n_draw.point((x, y), fill=(tone, tone, tone, alpha))
        
        if random.random() < 0.008:
            fx2 = x + random.randint(-8, 8)
            fy2 = y + random.randint(-8, 8)
            n_draw.line([(x, y), (fx2, fy2)], fill=(tone, tone, tone, random.randint(5, 12)), width=1)
            
    canvas = Image.alpha_composite(canvas, noise)
    return canvas


def draw_shanshui_landscape(canvas):
    """Draw authentic Chinese Literati Shanshui landscape contours anchored to bottom-right."""
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    # 1. Warm celestial rising sun / enso halo in upper-right
    sun_x, sun_y, max_r = 860, 110, 110
    for r in range(max_r, 0, -3):
        norm = 1.0 - (r / max_r)
        alpha = int(20 * norm)
        draw.ellipse([sun_x - r, sun_y - r, sun_x + r, sun_y + r],
                     fill=COLOR_VERMILION + (alpha,))

    def bezier_curve(p0, p1, p2, p3, steps=35):
        pts = []
        for i in range(steps + 1):
            t = i / steps
            x = (1-t)**3 * p0[0] + 3*(1-t)**2*t * p1[0] + 3*(1-t)*t**2 * p2[0] + t**3 * p3[0]
            y = (1-t)**3 * p0[1] + 3*(1-t)**2*t * p1[1] + 3*(1-t)*t**2 * p2[1] + t**3 * p3[1]
            pts.append((x, y))
        return pts

    # Ridge 1: Distant indigo peaks
    r1_curve = bezier_curve((380, 500), (520, 390), (740, 260), (1024, 270))
    poly_r1 = [(380, 500)] + r1_curve + [(1024, 500)]
    draw.polygon(poly_r1, fill=COLOR_INDIGO + (24,))

    # Ridge 2: Midground willow sage ridge
    r2_curve = bezier_curve((480, 500), (660, 420), (840, 320), (1024, 340))
    poly_r2 = [(480, 500)] + r2_curve + [(1024, 500)]
    draw.polygon(poly_r2, fill=COLOR_SAGE + (28,))

    # Ridge 3: Warm foothill ochre dune
    r3_curve = bezier_curve((590, 500), (750, 440), (890, 390), (1024, 420))
    poly_r3 = [(590, 500)] + r3_curve + [(1024, 500)]
    draw.polygon(poly_r3, fill=COLOR_OCHRE + (35,))

    # Solitary Sail (孤帆) on the mist horizon
    sail_x, sail_y = 805, 335
    draw.polygon([(sail_x, sail_y), (sail_x - 7, sail_y + 16), (sail_x + 6, sail_y + 16)],
                 fill=COLOR_INK_MUTED + (75,))
    draw.line([(sail_x, sail_y - 2), (sail_x, sail_y + 19)], fill=COLOR_INK_BLACK + (85,), width=1)
    draw.line([(sail_x - 10, sail_y + 19), (sail_x + 9, sail_y + 19)], fill=COLOR_INK_BLACK + (85,), width=1)

    # Water ripple hairlines
    for rip_y in [440, 455, 470, 482]:
        draw.line([(680, rip_y), (980, rip_y)], fill=COLOR_INDIGO + (18,), width=1)

    blurred = overlay.filter(ImageFilter.GaussianBlur(radius=1.2))
    return Image.alpha_composite(canvas, blurred)


def draw_material_elevation_shadow(canvas, x, y, w, h, radius, blur=18, dy=6, alpha=36):
    """Render realistic double-pass Material elevation drop shadow."""
    pad = blur * 2 + 10
    sw = w + pad * 2
    sh = h + pad * 2
    
    # Ambient shadow
    amb_img = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    amb_draw = ImageDraw.Draw(amb_img)
    amb_draw.rounded_rectangle(
        [pad, pad + dy, pad + w, pad + h + dy],
        radius=radius,
        fill=(44, 36, 22, alpha)
    )
    amb_blur = amb_img.filter(ImageFilter.GaussianBlur(radius=blur))

    # Key shadow
    key_img = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    key_draw = ImageDraw.Draw(key_img)
    key_draw.rounded_rectangle(
        [pad, pad + 2, pad + w, pad + h + 2],
        radius=radius,
        fill=(44, 36, 22, int(alpha * 0.75))
    )
    key_blur = key_img.filter(ImageFilter.GaussianBlur(radius=blur // 2.5))

    combined = Image.alpha_composite(amb_blur, key_blur)
    canvas.paste(combined, (x - pad, y - pad), combined)


def draw_enso_symbol(canvas, x, y, size=60):
    """Draw Mid-Century Modern Zen Ensō mark matching ic_zen_enso.xml."""
    enso_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(enso_layer)
    
    scale = size / 64.0
    stroke_w = int(7 * scale)
    ring_box = [int(8 * scale), int(8 * scale), int(56 * scale), int(56 * scale)]
    draw.ellipse(ring_box, outline=COLOR_VERMILION + (255,), width=stroke_w)
    
    dot_cx = int(32 * scale)
    dot_cy = int(20 * scale)
    dot_r = int(5.5 * scale)
    draw.ellipse([dot_cx - dot_r, dot_cy - dot_r, dot_cx + dot_r, dot_cy + dot_r], fill=COLOR_VERMILION + (255,))

    canvas.paste(enso_layer, (x, y), enso_layer)


def get_masked_widget(src_path, box, radius=63):
    """Crop and mask with 4x supersampled anti-aliased rounded rectangle."""
    im = Image.open(src_path).convert("RGBA")
    crop = im.crop(box)
    w, h = crop.size
    scale = 4
    mask = Image.new("L", (w * scale, h * scale), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, w * scale - 1, h * scale - 1], radius=radius * scale, fill=255)
    mask = mask.resize((w, h), Image.Resampling.LANCZOS)
    crop.putalpha(mask)
    return crop


def get_masked_4x1_dark_widget():
    """Load authentic 4x1 Dark Mode widget from live emulator capture."""
    if os.path.exists("artifacts/masked_4x1_dark.png"):
        return Image.open("artifacts/masked_4x1_dark.png")
    return get_masked_widget("artifacts/emulator_hero_2x1_live.png", (67, 231, 1013, 504), radius=63)


def get_masked_1x1_dark_widget():
    """Load authentic 1x1 Zen Pebble Dark Mode widget from live emulator capture."""
    if os.path.exists("artifacts/masked_1x1_dark.png"):
        return Image.open("artifacts/masked_1x1_dark.png")
    return get_masked_widget("artifacts/emulator_hero_1x1_live.png", (67, 231, 272, 504), radius=63)


def get_masked_real_card():
    """Load and mask real card from screenshot_1_main_list.png."""
    im = Image.open("playstore_package/screenshot_1_main_list.png")
    # Card 1 bounds: 68, 516, 1012, 874 (size: 944 x 358)
    c_crop = im.crop((68, 516, 1012, 874)).convert("RGBA")
    w, h = c_crop.size
    scale = 4
    mask = Image.new("L", (w * scale, h * scale), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, w * scale - 1, h * scale - 1], radius=60 * scale, fill=255)
    mask = mask.resize((w, h), Image.Resampling.LANCZOS)
    c_crop.putalpha(mask)
    return c_crop


def generate_feature_graphic():
    canvas = create_washi_paper_canvas()
    canvas = draw_shanshui_landscape(canvas)
    draw = ImageDraw.Draw(canvas)

    # =========================================================================
    # LEFT COLUMN: BRAND SOUL, SACRED EMPTINESS & VALUE PILLARS
    # =========================================================================
    left_x = 64
    top_y = 66

    # 1. Enso Emblem
    enso_size = 58
    draw_enso_symbol(canvas, left_x, top_y, size=enso_size)

    # 2. Main Title: "CountUp" in Marcellus Regular
    font_brand = get_font(FONT_MARCELLUS, 54)
    draw.text((left_x + enso_size + 14, top_y - 4), "CountUp", font=font_brand, fill=COLOR_INK_BLACK)

    # 3. Subtitle: "Mindful Days & Zen Widgets" in Noto Serif Italic
    font_sub = get_font(FONT_NOTO_ITALIC, 21)
    draw.text((left_x + enso_size + 16, top_y + 56), "Mindful Days & Zen Widgets", font=font_sub, fill=COLOR_INK_MUTED)

    # 4. Solar Term Capsule (24 Solar Terms Signature Feature)
    cap_y = top_y + 112
    cap_w, cap_h = 330, 34
    draw.rounded_rectangle([left_x, cap_y, left_x + cap_w, cap_y + cap_h],
                           radius=17, fill=COLOR_PAPER_SURFACE + (210,), outline=COLOR_HAIRLINE_VAR, width=1)
    
    font_sc_term = get_font(FONT_NOTO_SERIF_SC, 13)
    font_cap_en = get_font(FONT_MARCELLUS, 13)
    draw_centered_text(draw, (left_x + 12, cap_y, left_x + 44, cap_y + cap_h), "白露", font=font_sc_term, fill=COLOR_VERMILION)
    draw_centered_text(draw, (left_x + 46, cap_y, left_x + 138, cap_y + cap_h), "· WHITE DEW", font=font_cap_en, fill=COLOR_VERMILION)
    draw_centered_text(draw, (left_x + 138, cap_y, left_x + 150, cap_y + cap_h), "|", font=font_cap_en, fill=COLOR_HAIRLINE_VAR)
    draw_centered_text(draw, (left_x + 152, cap_y, left_x + 280, cap_y + cap_h), "24 SOLAR TERMS", font=font_cap_en, fill=COLOR_SEAL_INK)
    draw.ellipse([left_x + 292 - 4, cap_y + 17 - 4, left_x + 292 + 4, cap_y + 17 + 4], fill=COLOR_VERMILION)

    # 5. Core Value Seals (Uncluttered, calm vertical rhythm; bottom text removed)
    seal_start_y = cap_y + 60
    font_seal_title = get_font(FONT_SANS_BOLD, 14.5)
    font_seal_desc = get_font(FONT_SANS_REG, 12.5)

    value_props = [
        (COLOR_VERMILION, "100% Private & Offline", "Zero device permissions · No accounts · Offline sandbox"),
        (COLOR_SAGE, "5 Calm Zen Home Widgets", "Live home-screen counts · In-place reset · OEM hardened"),
        (COLOR_INDIGO, "30 Poetic Ink Landscapes", "Classical Chinese Shanshui themes rotate with seasons"),
    ]

    for i, (color_dot, title, desc) in enumerate(value_props):
        sy = seal_start_y + i * 50
        draw.ellipse([left_x + 2, sy + 4, left_x + 12, sy + 14], fill=color_dot)
        draw.text((left_x + 22, sy), title, font=font_seal_title, fill=COLOR_INK_BLACK)
        draw.text((left_x + 22, sy + 21), desc, font=font_seal_desc, fill=COLOR_INK_MUTED)

    # Bottom Editorial text ("No ads. No subscriptions. No cloud tracking.") removed per user request,
    # preserving pure negative space ("Ma" 間) over the subtle washi paper texture.

    # =========================================================================
    # RIGHT COLUMN: 100% REAL ANDROID UI SHOWCASE (ZERO GENERATIVE RESIDUE)
    # =========================================================================
    font_tag = get_font(FONT_SANS_BOLD, 10)

    # 1. Real 4×1 Zen Horizon Dark Mode Widget (Top Full Width)
    w4_img = get_masked_4x1_dark_widget()
    w4_w = 486
    w4_h = int(w4_w * (w4_img.size[1] / w4_img.size[0]))
    w4_res = w4_img.resize((w4_w, w4_h), Image.Resampling.LANCZOS)
    x4, y4 = 472, 44
    draw_material_elevation_shadow(canvas, x4, y4, w4_w, w4_h, radius=24, blur=18, dy=6, alpha=42)
    canvas.paste(w4_res, (x4, y4), w4_res)

    tag_text1 = "4×1 ZEN HORIZON · DARK MODE"
    draw.rounded_rectangle([x4 + 14, y4 - 12, x4 + 195, y4 + 8],
                           radius=7, fill=COLOR_INK_BLACK, outline=COLOR_OCHRE, width=1)
    draw_centered_text(draw, (x4 + 14, y4 - 12, x4 + 195, y4 + 8), tag_text1, font=font_tag, fill=COLOR_OCHRE)

    # 2. Real 1×1 Zen Pebble Dark Mode Widget (Bottom-Left)
    w1_img = get_masked_1x1_dark_widget()
    w1_h = 212
    w1_w = int(w1_h * (w1_img.size[0] / w1_img.size[1]))
    w1_res = w1_img.resize((w1_w, w1_h), Image.Resampling.LANCZOS)
    x1, y1 = 472, 230
    draw_material_elevation_shadow(canvas, x1, y1, w1_w, w1_h, radius=24, blur=16, dy=5, alpha=40)
    canvas.paste(w1_res, (x1, y1), w1_res)

    tag_text2 = "1×1 ZEN PEBBLE"
    draw.rounded_rectangle([x1 + 10, y1 - 12, x1 + 120, y1 + 8],
                           radius=7, fill=COLOR_INK_BLACK, outline=COLOR_OCHRE, width=1)
    draw_centered_text(draw, (x1 + 10, y1 - 12, x1 + 120, y1 + 8), tag_text2, font=font_tag, fill=COLOR_OCHRE)

    # 3. Real In-App 3D Tactile Card (Bottom-Right)
    card_img = get_masked_real_card()
    card_w = 312
    card_h = int(card_w * (card_img.size[1] / card_img.size[0]))
    card_res = card_img.resize((card_w, card_h), Image.Resampling.LANCZOS)
    xc = 472 + w1_w + 15
    yc = 230 + (w1_h - card_h) // 2
    draw_material_elevation_shadow(canvas, xc, yc, card_w, card_h, radius=18, blur=16, dy=5, alpha=36)
    canvas.paste(card_res, (xc, yc), card_res)

    tag_text3 = "IN-APP 3D CARD"
    draw.rounded_rectangle([xc + 12, yc - 12, xc + 122, yc + 8],
                           radius=7, fill=COLOR_PAPER_SURFACE, outline=COLOR_HAIRLINE_VAR, width=1)
    draw_centered_text(draw, (xc + 12, yc - 12, xc + 122, yc + 8), tag_text3, font=font_tag, fill=COLOR_INK_MUTED)

    # Convert final to 24-bit RGB (strict Google Play specification)
    final_rgb = canvas.convert("RGB")
    return final_rgb


if __name__ == "__main__":
    out_dir = "playstore_package"
    os.makedirs(out_dir, exist_ok=True)
    
    img = generate_feature_graphic()
    
    png_path = os.path.join(out_dir, "feature_graphic_1024x500.png")
    jpg_path = os.path.join(out_dir, "feature_graphic_1024x500.jpg")
    
    img.save(png_path, "PNG", optimize=True)
    img.save(jpg_path, "JPEG", quality=96)
    
    # Also mirror directly to conversation artifacts directory for instant presentation
    artifact_dir = r"C:\Users\kaipi\.gemini\antigravity\brain\d9ca751a-8747-4bd1-9cf1-415b3da539aa"
    if os.path.isdir(artifact_dir):
        artifact_png = os.path.join(artifact_dir, "feature_graphic_1024x500.png")
        shutil.copyfile(png_path, artifact_png)
        print(f"Mirrored to artifact directory: {artifact_png}")
    
    print(f"Generated feature graphic successfully: {png_path} ({img.size})")
    print(f"Generated feature graphic successfully: {jpg_path} ({img.size})")
