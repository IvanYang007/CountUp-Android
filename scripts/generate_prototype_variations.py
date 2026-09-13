"""
Multi-Variant 1024 x 500 Feature Graphic Prototype Generator for CountUp.
Renders 4 distinct architectural layout variants using 100% real widget screenshots.
"""

import math
import os
import random
import shutil
from PIL import Image, ImageDraw, ImageFilter, ImageFont

from generate_feature_graphic import (
    WIDTH, HEIGHT, COLOR_PAPER_BG, COLOR_PAPER_SURFACE, COLOR_CARD_WHITE,
    COLOR_INK_BLACK, COLOR_INK_MUTED, COLOR_HAIRLINE, COLOR_HAIRLINE_VAR,
    COLOR_VERMILION, COLOR_SAGE, COLOR_OCHRE, COLOR_INDIGO, COLOR_SEAL_INK,
    FONT_MARCELLUS, FONT_NOTO_ITALIC, FONT_SANS_BOLD, FONT_SANS_REG, FONT_NOTO_SERIF_SC,
    get_font, draw_centered_text, create_washi_paper_canvas, draw_shanshui_landscape,
    draw_material_elevation_shadow, draw_enso_symbol, get_masked_real_card,
    get_masked_4x1_dark_widget, get_masked_1x1_dark_widget
)

OUT_DIR = "artifacts"
os.makedirs(OUT_DIR, exist_ok=True)

w4_img = get_masked_4x1_dark_widget()
w1_img = get_masked_1x1_dark_widget()
card_img = get_masked_real_card()

font_tag = get_font(FONT_SANS_BOLD, 10)


def build_left_brand_column(canvas, draw):
    """Render consistent left brand column with sacred negative space."""
    left_x = 64
    top_y = 66

    # 1. Enso Emblem
    enso_size = 58
    draw_enso_symbol(canvas, left_x, top_y, size=enso_size)

    # 2. Main Title: "CountUp"
    font_brand = get_font(FONT_MARCELLUS, 54)
    draw.text((left_x + enso_size + 14, top_y - 4), "CountUp", font=font_brand, fill=COLOR_INK_BLACK)

    # 3. Subtitle: "Mindful Days & Zen Widgets"
    font_sub = get_font(FONT_NOTO_ITALIC, 21)
    draw.text((left_x + enso_size + 16, top_y + 56), "Mindful Days & Zen Widgets", font=font_sub, fill=COLOR_INK_MUTED)

    # 4. Solar Term Capsule (24 Solar Terms)
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

    # 5. Core Value Seals (Clean vertical rhythm; bottom text removed)
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


# =============================================================================
# VARIANT A: "The Zen Dyad & Companion Card" (Dark Widgets + In-App 3D Card)
# =============================================================================
def generate_variant_a():
    canvas = create_washi_paper_canvas()
    canvas = draw_shanshui_landscape(canvas)
    draw = ImageDraw.Draw(canvas)
    build_left_brand_column(canvas, draw)

    # 1. 4x1 Zen Horizon Dark Widget (Top)
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

    # 2. 1x1 Zen Pebble Dark Widget (Bottom-Left)
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

    # 3. In-App 3D Tactile Card (Bottom-Right)
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

    return canvas.convert("RGB")


# =============================================================================
# VARIANT B: "The Zen Widget Trinity" (100% Home Screen Widgets)
# =============================================================================
def generate_variant_b():
    canvas = create_washi_paper_canvas()
    canvas = draw_shanshui_landscape(canvas)
    draw = ImageDraw.Draw(canvas)
    build_left_brand_column(canvas, draw)

    # 1. 4x1 Zen Horizon Dark Widget (Top)
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

    # 2. 1x1 Zen Pebble Dark Widget (Bottom-Left)
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

    # 3. Real Multi-Item Zen Widget (Bottom-Right, cropped and masked)
    im_w_full = Image.open("artifacts/playstore_screenshot_4_widget_final.png")
    crop_w = im_w_full.crop((60, 230, 1020, 685)).convert("RGBA")
    w_mw, h_mw = crop_w.size
    scale = 4
    mask = Image.new("L", (w_mw * scale, h_mw * scale), 0)
    dm = ImageDraw.Draw(mask)
    dm.rounded_rectangle([0, 0, w_mw * scale - 1, h_mw * scale - 1], radius=50 * scale, fill=255)
    mask = mask.resize((w_mw, h_mw), Image.Resampling.LANCZOS)
    crop_w.putalpha(mask)

    mw_w = 312
    mw_h = int(mw_w * (crop_w.size[1] / crop_w.size[0]))
    mw_res = crop_w.resize((mw_w, mw_h), Image.Resampling.LANCZOS)
    xmw = 472 + w1_w + 15
    ymw = 230 + (w1_h - mw_h) // 2
    draw_material_elevation_shadow(canvas, xmw, ymw, mw_w, mw_h, radius=18, blur=16, dy=5, alpha=36)
    canvas.paste(mw_res, (xmw, ymw), mw_res)

    tag_text3 = "MULTI-ITEM ZEN WIDGET"
    draw.rounded_rectangle([xmw + 12, ymw - 12, xmw + 160, ymw + 8],
                           radius=7, fill=(250, 238, 230), outline=COLOR_VERMILION, width=1)
    draw_centered_text(draw, (xmw + 12, ymw - 12, xmw + 160, ymw + 8), tag_text3, font=font_tag, fill=COLOR_VERMILION)

    return canvas.convert("RGB")


# =============================================================================
# VARIANT C: "The In-Situ Mobile Stage" (Contextual Handset + Floating Pebble)
# =============================================================================
def generate_variant_c():
    canvas = create_washi_paper_canvas()
    canvas = draw_shanshui_landscape(canvas)
    draw = ImageDraw.Draw(canvas)
    build_left_brand_column(canvas, draw)

    # 1. Android Phone Mockup (Center-Right)
    im_phone = Image.open("artifacts/emulator_hero_2x1_live.png")
    ph_h = 428
    ph_w = int(ph_h * (im_phone.size[0] / im_phone.size[1]))
    phone_res = im_phone.resize((ph_w, ph_h), Image.Resampling.LANCZOS)

    bezel_pad = 6
    total_w = ph_w + bezel_pad * 2
    total_h = ph_h + bezel_pad * 2
    phone_frame = Image.new("RGBA", (total_w, total_h), (0, 0, 0, 0))
    df = ImageDraw.Draw(phone_frame)
    df.rounded_rectangle([0, 0, total_w - 1, total_h - 1], radius=24, fill=(30, 27, 24, 255), outline=(75, 68, 60, 255), width=2)

    mask = Image.new("L", (ph_w, ph_h), 0)
    dm = ImageDraw.Draw(mask)
    dm.rounded_rectangle([0, 0, ph_w - 1, ph_h - 1], radius=18, fill=255)
    phone_res.putalpha(mask)
    phone_frame.paste(phone_res, (bezel_pad, bezel_pad), phone_res)

    px, py = 475, 36
    draw_material_elevation_shadow(canvas, px, py, total_w, total_h, radius=24, blur=22, dy=8, alpha=50)
    canvas.paste(phone_frame, (px, py), phone_frame)

    tag_phone = "LIVE HOME SCREEN"
    draw.rounded_rectangle([px + 12, py - 12, px + 140, py + 8],
                           radius=7, fill=COLOR_INK_BLACK, outline=COLOR_OCHRE, width=1)
    draw_centered_text(draw, (px + 12, py - 12, px + 140, py + 8), tag_phone, font=font_tag, fill=COLOR_OCHRE)

    # 2. High-Resolution 1x1 Zen Pebble Tile (Floating to the right of the phone)
    w1_h = 220
    w1_w = int(w1_h * (w1_img.size[0] / w1_img.size[1]))
    w1_res = w1_img.resize((w1_w, w1_h), Image.Resampling.LANCZOS)
    x1, y1 = px + total_w + 32, 54
    draw_material_elevation_shadow(canvas, x1, y1, w1_w, w1_h, radius=24, blur=18, dy=6, alpha=44)
    canvas.paste(w1_res, (x1, y1), w1_res)

    tag_p = "1×1 ZEN PEBBLE"
    draw.rounded_rectangle([x1 + 10, y1 - 12, x1 + 120, y1 + 8],
                           radius=7, fill=COLOR_INK_BLACK, outline=COLOR_OCHRE, width=1)
    draw_centered_text(draw, (x1 + 10, y1 - 12, x1 + 120, y1 + 8), tag_p, font=font_tag, fill=COLOR_OCHRE)

    # 3. Habit Milestone Note / In-App callout chip below the pebble
    chip_w, chip_h = 260, 130
    chip_x, chip_y = px + total_w + 20, 310
    draw_material_elevation_shadow(canvas, chip_x, chip_y, chip_w, chip_h, radius=18, blur=16, dy=5, alpha=36)
    
    # Load and crop in-app card content
    card_sub = card_img.crop((0, 0, card_img.size[0], int(card_img.size[1]*0.9)))
    card_sub_res = card_sub.resize((chip_w, chip_h), Image.Resampling.LANCZOS)
    canvas.paste(card_sub_res, (chip_x, chip_y), card_sub_res)

    tag_sub = "TACTILE IN-APP CARD"
    draw.rounded_rectangle([chip_x + 10, chip_y - 12, chip_x + 150, chip_y + 8],
                           radius=7, fill=COLOR_PAPER_SURFACE, outline=COLOR_HAIRLINE_VAR, width=1)
    draw_centered_text(draw, (chip_x + 10, chip_y - 12, chip_x + 150, chip_y + 8), tag_sub, font=font_tag, fill=COLOR_INK_MUTED)

    return canvas.convert("RGB")


# =============================================================================
# VARIANT D: "The Nocturnal Ink Stage" (Deep Stone Gallery Showcase)
# =============================================================================
def generate_variant_d():
    canvas = create_washi_paper_canvas()
    canvas = draw_shanshui_landscape(canvas)
    draw = ImageDraw.Draw(canvas)
    build_left_brand_column(canvas, draw)

    # Draw Deep Ink-Stone Stage on the right
    stage_x, stage_y, stage_w, stage_h = 458, 30, 526, 440
    draw_material_elevation_shadow(canvas, stage_x, stage_y, stage_w, stage_h, radius=24, blur=22, dy=7, alpha=48)
    
    # Fill stage with warm charcoal paper tone
    stage_layer = Image.new("RGBA", (stage_w, stage_h), (0, 0, 0, 0))
    ds = ImageDraw.Draw(stage_layer)
    ds.rounded_rectangle([0, 0, stage_w - 1, stage_h - 1], radius=24, fill=(34, 30, 24, 255), outline=(78, 69, 58, 255), width=1)
    
    # Stage header label
    font_stage = get_font(FONT_MARCELLUS, 12)
    ds.text((22, 16), "ZEN WIDGET SUITE · DARK THEME", font=font_stage, fill=COLOR_OCHRE)
    ds.line([(22, 34), (stage_w - 22, 34)], fill=(62, 54, 44, 255), width=1)
    canvas.paste(stage_layer, (stage_x, stage_y), stage_layer)

    # 1. 4x1 Dark Widget inside the stage
    w4_w = 482
    w4_h = int(w4_w * (w4_img.size[1] / w4_img.size[0]))
    w4_res = w4_img.resize((w4_w, w4_h), Image.Resampling.LANCZOS)
    x4 = stage_x + (stage_w - w4_w) // 2
    y4 = stage_y + 48
    draw_material_elevation_shadow(canvas, x4, y4, w4_w, w4_h, radius=20, blur=14, dy=4, alpha=55)
    canvas.paste(w4_res, (x4, y4), w4_res)

    # 2. Bottom row: 1x1 Dark Pebble + 1x1 Detail Card
    w1_h = 210
    w1_w = int(w1_h * (w1_img.size[0] / w1_img.size[1]))
    w1_res = w1_img.resize((w1_w, w1_h), Image.Resampling.LANCZOS)
    x1 = stage_x + 22
    y1 = stage_y + 58 + w4_h
    draw_material_elevation_shadow(canvas, x1, y1, w1_w, w1_h, radius=22, blur=14, dy=4, alpha=50)
    canvas.paste(w1_res, (x1, y1), w1_res)

    # 3. Companion Card on the right of 1x1
    cw = stage_w - w1_w - 60
    ch = int(cw * (card_img.size[1] / card_img.size[0]))
    c_res = card_img.resize((cw, ch), Image.Resampling.LANCZOS)
    xc = x1 + w1_w + 16
    yc = y1 + (w1_h - ch) // 2
    draw_material_elevation_shadow(canvas, xc, yc, cw, ch, radius=16, blur=14, dy=4, alpha=40)
    canvas.paste(c_res, (xc, yc), c_res)

    return canvas.convert("RGB")


if __name__ == "__main__":
    variants = [
        ("A", "prototype_variant_a.png", generate_variant_a),
        ("B", "prototype_variant_b.png", generate_variant_b),
        ("C", "prototype_variant_c.png", generate_variant_c),
        ("D", "prototype_variant_d.png", generate_variant_d),
    ]

    artifact_dir = r"C:\Users\kaipi\.gemini\antigravity\brain\d9ca751a-8747-4bd1-9cf1-415b3da539aa"

    for key, filename, generator in variants:
        img = generator()
        out_path = os.path.join(OUT_DIR, filename)
        img.save(out_path, "PNG", optimize=True)
        print(f"Generated Variant {key}: {out_path} ({img.size})")

        # Mirror to conversation artifact directory for preview
        if os.path.isdir(artifact_dir):
            mirror_path = os.path.join(artifact_dir, filename)
            shutil.copyfile(out_path, mirror_path)
            print(f"  -> Mirrored to {mirror_path}")
