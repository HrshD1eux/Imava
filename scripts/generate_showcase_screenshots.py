#!/usr/bin/env python3
"""
generate_showcase_screenshots.py
Generates Google Play Store / GitHub showcase mockups for Imava.
Combines raw screenshots with modern flagship device frames, ambient glowing backdrops,
and sleek typography tailored for high conversion and premium presentation.
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

# Showcase specifications for all 8 core screens
SCREENSHOT_SPECS = [
    {
        "filename": "01_photos.png",
        "badge": "120Hz SMOOTH • ZERO BLOAT",
        "title": "Blazing Fast. Beautifully Pure.",
        "subtitle": "Fluid pinch-to-zoom timeline with instant date grouping.",
        "glow_color": (99, 102, 241),       # Indigo
        "accent_color": (129, 140, 248),    # Light Indigo
        "badge_border": (165, 180, 252, 160)
    },
    {
        "filename": "02_albums.png",
        "badge": "SMART ALBUMS & STORIES",
        "title": "Organize Without Limits.",
        "subtitle": "Pin favorite folders, browse locations, and relive throwback stories.",
        "glow_color": (139, 92, 246),      # Violet
        "accent_color": (167, 139, 250),   # Light Violet
        "badge_border": (196, 181, 253, 160)
    },
    {
        "filename": "03_selection.png",
        "badge": "POWERFUL BATCH TOOLS",
        "title": "Edit, Sanitize & Rename.",
        "subtitle": "EXIF scrubbing, mosaic redaction, and multi-photo management in 1 tap.",
        "glow_color": (37, 99, 235),       # Azure Blue
        "accent_color": (96, 165, 250),    # Sky Blue
        "badge_border": (147, 197, 253, 160)
    },
    {
        "filename": "04_tag_manager.png",
        "badge": "INSTANT SQLITE TAG FILTERING",
        "title": "Find Any Shot in Milliseconds.",
        "subtitle": "Custom tag ordering, drag-to-reorder, and zero-disk search indexing.",
        "glow_color": (217, 119, 6),       # Amber
        "accent_color": (251, 191, 36),    # Gold Amber
        "badge_border": (253, 230, 138, 160)
    },
    {
        "filename": "05_vault_setup.png",
        "badge": "AES-256-GCM HARDWARE VAULT",
        "title": "Fortress for Private Memories.",
        "subtitle": "AndroidKeyStore StrongBox TEE encryption with Decoy PIN deniability.",
        "glow_color": (5, 150, 105),       # Emerald
        "accent_color": (52, 211, 153),    # Mint Emerald
        "badge_border": (110, 231, 183, 160)
    },
    {
        "filename": "06_privacy_shield.png",
        "badge": "RECENT APPS PRIVACY SHIELD",
        "title": "Immune to Shoulder Surfing.",
        "subtitle": "FLAG_SECURE shields app previews in Android's recent task switcher.",
        "glow_color": (225, 29, 72),       # Rose Crimson
        "accent_color": (251, 113, 133),   # Coral Pink
        "badge_border": (253, 164, 175, 160)
    },
    {
        "filename": "07_about_legal.png",
        "badge": "100% OFFLINE • ZERO TRACKERS",
        "title": "Pure Privacy. Zero Compromises.",
        "subtitle": "Audited open-source code with complete permission transparency.",
        "glow_color": (8, 145, 178),       # Cyan
        "accent_color": (56, 189, 248),    # Sky Cyan
        "badge_border": (125, 211, 252, 160)
    },
    {
        "filename": "08_album_privacy.png",
        "badge": "PER-ALBUM BIOMETRIC LOCK",
        "title": "Lock Specific Folders Instantly.",
        "subtitle": "Fingerprint & PIN security with automatic stealth relock.",
        "glow_color": (124, 58, 237),      # Purple
        "accent_color": (192, 132, 252),   # Lilac
        "badge_border": (216, 180, 254, 160)
    }
]

def render_showcase(spec: dict, raw_path: Path, output_path: Path):
    canvas_w = CANVAS_WIDTH
    canvas_h = CANVAS_HEIGHT

    # Base background gradient (Deep Obsidian Slate)
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (11, 15, 25, 255))
    draw = ImageDraw.Draw(canvas)

    # Gradient: Top #0A0E17 to Bottom #0F172A
    for y in range(canvas_h):
        factor = y / canvas_h
        r = int(10 * (1 - factor) + 15 * factor)
        g = int(14 * (1 - factor) + 23 * factor)
        b = int(23 * (1 - factor) + 42 * factor)
        draw.line([(0, y), (canvas_w, y)], fill=(r, g, b, 255))

    glow_rgb = spec["glow_color"]
    accent_rgb = spec["accent_color"]
    badge_border = spec["badge_border"]

    # Multi-ring ambient glowing aura behind the device
    glow_img = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_img)
    glow_center_x = canvas_w // 2
    glow_center_y = 780
    glow_radius = 580

    for r in range(glow_radius, 0, -12):
        alpha = int(48 * (1 - r / glow_radius) ** 1.85)
        glow_draw.ellipse(
            [glow_center_x - r, glow_center_y - r, glow_center_x + r, glow_center_y + r],
            fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], alpha)
        )

    # Subtle decorative geometric halo ring
    ring_r = 380
    glow_draw.ellipse(
        [glow_center_x - ring_r, glow_center_y - ring_r, glow_center_x + ring_r, glow_center_y + ring_r],
        outline=(accent_rgb[0], accent_rgb[1], accent_rgb[2], 28),
        width=2
    )

    canvas = Image.alpha_composite(canvas, glow_img)
    draw = ImageDraw.Draw(canvas)

    # Load fonts
    font_pill = ImageFont.truetype(FONT_BOLD_PATH, 24)
    font_title = ImageFont.truetype(FONT_BOLD_PATH, 58)
    font_sub = ImageFont.truetype(FONT_REGULAR_PATH, 28)

    # 1. Badge Pill
    pill_text = spec["badge"]
    pill_bbox = font_pill.getbbox(pill_text)
    pill_text_w = pill_bbox[2] - pill_bbox[0]
    pill_w = pill_text_w + 58
    pill_h = 50
    pill_x = (canvas_w - pill_w) // 2
    pill_y = 110

    # Pill background
    draw.rounded_rectangle(
        [pill_x, pill_y, pill_x + pill_w, pill_y + pill_h],
        radius=25,
        fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], 42),
        outline=badge_border,
        width=2
    )

    # Glowing dot in pill
    dot_r = 5
    dot_x = pill_x + 24
    dot_y = pill_y + pill_h // 2
    draw.ellipse(
        [dot_x - dot_r, dot_y - dot_r, dot_x + dot_r, dot_y + dot_r],
        fill=(accent_rgb[0], accent_rgb[1], accent_rgb[2], 255)
    )
    draw.text(
        (pill_x + 40, pill_y + 11),
        pill_text,
        font=font_pill,
        fill=(248, 250, 252, 255)
    )

    # 2. Main Title
    title_text = spec["title"]
    title_bbox = font_title.getbbox(title_text)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = (canvas_w - title_w) // 2
    title_y = 195
    draw.text((title_x, title_y), title_text, font=font_title, fill=(255, 255, 255, 255))

    # 3. Subtitle (supports optional wrap if long)
    sub_text = spec["subtitle"]
    sub_bbox = font_sub.getbbox(sub_text)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sub_x = (canvas_w - sub_w) // 2
    sub_y = 285
    draw.text((sub_x, sub_y), sub_text, font=font_sub, fill=(148, 163, 184, 255))

    # 4. Device Mockup
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

    # Outer metallic chassis
    phone_draw.rounded_rectangle(
        [0, 0, frame_w, frame_h],
        radius=frame_radius,
        fill=(24, 30, 44, 255),
        outline=(71, 85, 105, 255),
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
    phone_draw.ellipse([cam_x + 9, cam_y + 9, cam_x + 13, cam_y + 13], fill=(130, 180, 255, 140)) # lens glare

    # Subtle screen glass glare reflection in upper right
    glare_img = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    glare_draw = ImageDraw.Draw(glare_img)
    glare_draw.polygon([
        (screen_w // 2, 0),
        (screen_w, 0),
        (screen_w, int(screen_h * 0.35)),
        (int(screen_w * 0.3), 0)
    ], fill=(255, 255, 255, 12))
    glare_masked = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    glare_masked.paste(glare_img, (0, 0), mask)
    phone_img.alpha_composite(glare_masked, (bezel, bezel))

    # Multi-tier drop shadow
    shadow_pad = 120
    shadow_img = Image.new("RGBA", (frame_w + shadow_pad * 2, frame_h + shadow_pad * 2), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow_img)

    # Colored ambient bounce shadow
    shadow_draw.rounded_rectangle(
        [shadow_pad - 10, shadow_pad + 20, shadow_pad + frame_w + 10, shadow_pad + frame_h + 30],
        radius=frame_radius + 10,
        fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], 55)
    )
    # Deep black contact shadow
    shadow_draw.rounded_rectangle(
        [shadow_pad, shadow_pad + 15, shadow_pad + frame_w, shadow_pad + frame_h + 20],
        radius=frame_radius,
        fill=(0, 0, 0, 210)
    )
    shadow_blurred = shadow_img.filter(ImageFilter.GaussianBlur(40))

    # Position phone on canvas (rising from bottom)
    phone_x = (canvas_w - frame_w) // 2
    phone_y = 430

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

    print("[*] Generating Play Store & GitHub Showcase Mockups...")
    for spec in SCREENSHOT_SPECS:
        raw_file = raw_dir / spec["filename"]
        if not raw_file.exists():
            print(f"  [!] Missing raw file: {raw_file}")
            continue
        out_file = dest_dir / spec["filename"]
        render_showcase(spec, raw_file, out_file)

    print("[SUCCESS] All 8 showcase screenshots generated successfully!")

if __name__ == "__main__":
    main()
