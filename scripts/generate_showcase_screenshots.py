#!/usr/bin/env python3
"""
generate_showcase_screenshots.py
Generates Google Play Store / GitHub showcase mockups for Imava in Light Theme.
Combines raw screenshots with modern flagship device frames, soft ambient pastel glowing backdrops,
and commanding, large typography tailored for high conversion, readability, and premium presentation.
"""

import os
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# 1200 x 2400 (Exact 1:2 ratio, Google Play Store compliant & Retina crisp)
CANVAS_WIDTH = 1200
CANVAS_HEIGHT = 2400

# Windows System Fonts
FONT_BOLD_PATH = "C:/Windows/Fonts/segoeuib.ttf"
FONT_REGULAR_PATH = "C:/Windows/Fonts/segoeui.ttf"

# Showcase specifications for all 8 core screens in Light Theme
SCREENSHOT_SPECS = [
    {
        "filename": "01_photos.png",
        "badge": "120Hz SMOOTH • ZERO BLOAT",
        "title": "Blazing Fast. Beautifully Pure.",
        "subtitle": "Fluid pinch-to-zoom timeline with instant date grouping.",
        "glow_color": (99, 102, 241),      # Indigo
        "accent_text": (55, 48, 163),      # Indigo 800
        "pill_bg": (238, 242, 255),        # Indigo 50
        "pill_border": (165, 180, 252)     # Indigo 300
    },
    {
        "filename": "02_albums.png",
        "badge": "SMART ALBUMS & STORIES",
        "title": "Organize Without Limits.",
        "subtitle": "Pin favorite folders, browse locations, and relive stories.",
        "glow_color": (139, 92, 246),     # Violet
        "accent_text": (91, 33, 182),      # Violet 800
        "pill_bg": (245, 243, 255),        # Violet 50
        "pill_border": (196, 181, 253)     # Violet 300
    },
    {
        "filename": "03_selection.png",
        "badge": "POWERFUL BATCH TOOLS",
        "title": "Edit, Sanitize & Rename.",
        "subtitle": "EXIF scrubbing, mosaic redaction, and bulk operations.",
        "glow_color": (37, 99, 235),      # Blue
        "accent_text": (30, 64, 175),      # Blue 800
        "pill_bg": (239, 246, 255),        # Blue 50
        "pill_border": (147, 197, 253)     # Blue 300
    },
    {
        "filename": "04_tag_manager.png",
        "badge": "INSTANT SQLITE TAG FILTERING",
        "title": "Find Any Shot in Milliseconds.",
        "subtitle": "Custom tag ordering, drag-to-reorder, and fast search.",
        "glow_color": (217, 119, 6),      # Amber
        "accent_text": (146, 64, 14),      # Amber 800
        "pill_bg": (254, 243, 199),        # Amber 50
        "pill_border": (252, 211, 77)      # Amber 300
    },
    {
        "filename": "05_vault_setup.png",
        "badge": "AES-256-GCM HARDWARE VAULT",
        "title": "Fortress for Private Memories.",
        "subtitle": "StrongBox TEE encryption with Decoy PIN deniability.",
        "glow_color": (5, 150, 105),      # Emerald
        "accent_text": (6, 95, 70),        # Emerald 800
        "pill_bg": (236, 253, 245),        # Emerald 50
        "pill_border": (110, 231, 183)     # Emerald 300
    },
    {
        "filename": "06_privacy_shield.png",
        "badge": "RECENT APPS PRIVACY SHIELD",
        "title": "Immune to Shoulder Surfing.",
        "subtitle": "FLAG_SECURE shields app previews in Android task switcher.",
        "glow_color": (225, 29, 72),      # Rose
        "accent_text": (159, 18, 57),      # Rose 800
        "pill_bg": (255, 241, 242),        # Rose 50
        "pill_border": (253, 164, 175)     # Rose 300
    },
    {
        "filename": "07_about_legal.png",
        "badge": "100% OFFLINE • ZERO TRACKERS",
        "title": "Pure Privacy. Zero Compromises.",
        "subtitle": "Audited open-source code with complete transparency.",
        "glow_color": (8, 145, 178),      # Cyan
        "accent_text": (22, 78, 99),       # Cyan 900
        "pill_bg": (236, 254, 255),        # Cyan 50
        "pill_border": (103, 232, 249)     # Cyan 300
    },
    {
        "filename": "08_album_privacy.png",
        "badge": "PER-ALBUM BIOMETRIC LOCK",
        "title": "Lock Specific Folders Instantly.",
        "subtitle": "Fingerprint & PIN security with automatic stealth relock.",
        "glow_color": (124, 58, 237),     # Purple
        "accent_text": (88, 28, 135),      # Purple 800
        "pill_bg": (250, 245, 255),        # Purple 50
        "pill_border": (216, 180, 254)     # Purple 300
    }
]

def render_showcase(spec: dict, raw_path: Path, output_path: Path):
    canvas_w = CANVAS_WIDTH
    canvas_h = CANVAS_HEIGHT

    # Light Theme Canvas Background: Clean Slate 50 to Slate 100/200
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (248, 250, 252, 255))
    draw = ImageDraw.Draw(canvas)

    for y in range(canvas_h):
        factor = y / canvas_h
        r = int(248 * (1 - factor) + 236 * factor)
        g = int(250 * (1 - factor) + 240 * factor)
        b = int(252 * (1 - factor) + 245 * factor)
        draw.line([(0, y), (canvas_w, y)], fill=(r, g, b, 255))

    glow_rgb = spec["glow_color"]
    accent_text = spec["accent_text"]
    pill_bg = spec["pill_bg"]
    pill_border = spec["pill_border"]

    # Soft ambient pastel glow aura behind the device
    glow_img = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_img)
    glow_center_x = canvas_w // 2
    glow_center_y = 800
    glow_radius = 620

    for r in range(glow_radius, 0, -14):
        alpha = int(48 * (1 - r / glow_radius) ** 1.8)
        glow_draw.ellipse(
            [glow_center_x - r, glow_center_y - r, glow_center_x + r, glow_center_y + r],
            fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], alpha)
        )

    # Subtle decorative circular light halo ring
    ring_r = 420
    glow_draw.ellipse(
        [glow_center_x - ring_r, glow_center_y - ring_r, glow_center_x + ring_r, glow_center_y + ring_r],
        outline=(glow_rgb[0], glow_rgb[1], glow_rgb[2], 32),
        width=2
    )

    canvas = Image.alpha_composite(canvas, glow_img)
    draw = ImageDraw.Draw(canvas)

    # Increased Font Sizes for high legibility
    font_pill = ImageFont.truetype(FONT_BOLD_PATH, 28)
    font_title = ImageFont.truetype(FONT_BOLD_PATH, 66)
    font_sub = ImageFont.truetype(FONT_REGULAR_PATH, 34)

    # 1. Badge Pill
    pill_text = spec["badge"]
    pill_bbox = font_pill.getbbox(pill_text)
    pill_text_w = pill_bbox[2] - pill_bbox[0]
    pill_w = pill_text_w + 68
    pill_h = 58
    pill_x = (canvas_w - pill_w) // 2
    pill_y = 90

    # Draw rounded pill capsule
    draw.rounded_rectangle(
        [pill_x, pill_y, pill_x + pill_w, pill_y + pill_h],
        radius=29,
        fill=(pill_bg[0], pill_bg[1], pill_bg[2], 255),
        outline=(pill_border[0], pill_border[1], pill_border[2], 255),
        width=2
    )

    # Vibrant glowing dot inside pill
    dot_r = 6
    dot_x = pill_x + 28
    dot_y = pill_y + pill_h // 2
    draw.ellipse(
        [dot_x - dot_r, dot_y - dot_r, dot_x + dot_r, dot_y + dot_r],
        fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], 255)
    )
    draw.text(
        (pill_x + 46, pill_y + 11),
        pill_text,
        font=font_pill,
        fill=(accent_text[0], accent_text[1], accent_text[2], 255)
    )

    # 2. Main Title (Bolder, 66px, Deep Charcoal #0F172A)
    title_text = spec["title"]
    title_bbox = font_title.getbbox(title_text)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = (canvas_w - title_w) // 2
    title_y = 175
    draw.text((title_x, title_y), title_text, font=font_title, fill=(15, 23, 42, 255))

    # 3. Subtitle (Bolder, 34px, Slate 600 #475569)
    sub_text = spec["subtitle"]
    sub_bbox = font_sub.getbbox(sub_text)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sub_x = (canvas_w - sub_w) // 2
    sub_y = 270
    draw.text((sub_x, sub_y), sub_text, font=font_sub, fill=(71, 85, 105, 255))

    # 4. Device Mockup Chassis
    screen_raw = Image.open(raw_path).convert("RGBA")

    screen_w = 900
    screen_h = int(screen_w * (screen_raw.height / screen_raw.width))
    screen_resized = screen_raw.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    # Screen corner radius
    screen_radius = 50
    mask = Image.new("L", (screen_w, screen_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, screen_w, screen_h], radius=screen_radius, fill=255)

    # Frame specs
    bezel = 16
    frame_w = screen_w + (bezel * 2)
    frame_h = screen_h + (bezel * 2)
    frame_radius = screen_radius + bezel

    phone_img = Image.new("RGBA", (frame_w, frame_h), (0, 0, 0, 0))
    phone_draw = ImageDraw.Draw(phone_img)

    # Outer metallic chassis (Titanium Dark Slate finish)
    phone_draw.rounded_rectangle(
        [0, 0, frame_w, frame_h],
        radius=frame_radius,
        fill=(24, 30, 44, 255),
        outline=(51, 65, 85, 255),
        width=3
    )
    # Inner bezel trim
    phone_draw.rounded_rectangle(
        [3, 3, frame_w - 3, frame_h - 3],
        radius=frame_radius - 2,
        fill=None,
        outline=(15, 23, 42, 255),
        width=bezel - 4
    )

    # Paste screen inside frame
    phone_img.paste(screen_resized, (bezel, bezel), mask)

    # Camera punch hole
    cam_d = 26
    cam_x = frame_w // 2 - cam_d // 2
    cam_y = bezel + 22
    phone_draw.ellipse([cam_x, cam_y, cam_x + cam_d, cam_y + cam_d], fill=(8, 10, 15, 255))
    phone_draw.ellipse([cam_x + 5, cam_y + 5, cam_x + cam_d - 5, cam_y + cam_d - 5], fill=(22, 27, 46, 255))
    phone_draw.ellipse([cam_x + 9, cam_y + 9, cam_x + 13, cam_y + 13], fill=(130, 180, 255, 140))

    # Subtle screen glass glare reflection in upper right
    glare_img = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    glare_draw = ImageDraw.Draw(glare_img)
    glare_draw.polygon([
        (screen_w // 2, 0),
        (screen_w, 0),
        (screen_w, int(screen_h * 0.35)),
        (int(screen_w * 0.3), 0)
    ], fill=(255, 255, 255, 14))
    glare_masked = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    glare_masked.paste(glare_img, (0, 0), mask)
    phone_img.alpha_composite(glare_masked, (bezel, bezel))

    # Multi-tier drop shadow calibrated for light canvas
    shadow_pad = 120
    shadow_img = Image.new("RGBA", (frame_w + shadow_pad * 2, frame_h + shadow_pad * 2), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow_img)

    # Colored ambient bounce shadow
    shadow_draw.rounded_rectangle(
        [shadow_pad - 12, shadow_pad + 25, shadow_pad + frame_w + 12, shadow_pad + frame_h + 35],
        radius=frame_radius + 10,
        fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], 45)
    )
    # Deep black contact shadow
    shadow_draw.rounded_rectangle(
        [shadow_pad, shadow_pad + 18, shadow_pad + frame_w, shadow_pad + frame_h + 22],
        radius=frame_radius,
        fill=(15, 23, 42, 115)
    )
    shadow_blurred = shadow_img.filter(ImageFilter.GaussianBlur(38))

    # Position phone on canvas (rising gracefully from bottom)
    phone_x = (canvas_w - frame_w) // 2
    phone_y = 410

    # Composite shadow then phone
    canvas.alpha_composite(shadow_blurred, (phone_x - shadow_pad, phone_y - shadow_pad))
    canvas.alpha_composite(phone_img, (phone_x, phone_y))

    # Save high-quality RGB PNG
    output_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(output_path, format="PNG", optimize=True)
    print(f"  [OK] Rendered {output_path.name}")

def main():
    repo_root = Path(__file__).resolve().parent.parent
    raw_dir = repo_root / "screenshots" / "raw"
    dest_dir = repo_root / "screenshots"

    if not raw_dir.exists():
        print(f"Error: {raw_dir} does not exist.")
        sys.exit(1)

    print("[*] Generating Play Store & GitHub Light-Theme Showcase Mockups...")
    for spec in SCREENSHOT_SPECS:
        raw_file = raw_dir / spec["filename"]
        if not raw_file.exists():
            print(f"  [!] Missing raw file: {raw_file}")
            continue
        out_file = dest_dir / spec["filename"]
        render_showcase(spec, raw_file, out_file)

    print("[SUCCESS] All 8 light-theme showcase screenshots generated successfully!")

if __name__ == "__main__":
    main()
