#!/usr/bin/env python3
"""Generates Mehran FarsiPlex branding assets.

Outputs (all committed):
  app/src/main/res/drawable-nodpi/ic_launcher.png   512x512 launcher icon
  app/src/main/res/drawable-nodpi/tv_banner.png     320x180 Android TV banner
  app/src/main/res/drawable-nodpi/logo.png          600x600 splash logo (transparent)
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/res/drawable-nodpi"

BRAND = (11, 126, 244)      # #0B7EF4, lifted from farsiplex.com's player accent
BRAND_DARK = (5, 58, 130)
NIGHT = (7, 8, 12)
WHITE = (245, 247, 255)
MUTED = (150, 162, 190)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def font(size, bold=True):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)


def vertical_gradient(size, top, bottom):
    w, h = size
    img = Image.new("RGB", size, top)
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        row = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = row
    return img


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius, fill=255)
    return mask


def play_triangle(draw, box, color=WHITE):
    x0, y0, x1, y1 = box
    draw.polygon([(x0, y0), (x0, y1), (x1, (y0 + y1) / 2)], fill=color)


def make_icon(size=512):
    img = vertical_gradient((size, size), BRAND, BRAND_DARK).convert("RGBA")
    d = ImageDraw.Draw(img)
    # Subtle ring so the mark reads on both light and dark launcher rows.
    d.ellipse([size * 0.08, size * 0.08, size * 0.92, size * 0.92],
              outline=(255, 255, 255, 38), width=max(2, size // 90))
    # Film strip holes down the left edge.
    hole_w, hole_h = size * 0.07, size * 0.055
    for i in range(5):
        y = size * 0.22 + i * size * 0.14
        d.rounded_rectangle([size * 0.17, y, size * 0.17 + hole_w, y + hole_h],
                            radius=hole_h / 2, fill=(255, 255, 255, 90))
    play_triangle(d, (size * 0.4, size * 0.33, size * 0.78, size * 0.67))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), rounded_mask((size, size), int(size * 0.22)))
    return out


def make_banner(w=320, h=180):
    img = vertical_gradient((w, h), (13, 24, 45), NIGHT).convert("RGBA")
    d = ImageDraw.Draw(img)
    # Soft brand glow behind the wordmark instead of a hard graphic shape.
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([w * 0.55, -h * 0.5, w * 1.25, h * 0.75],
                                 fill=(*BRAND, 34))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    icon = make_icon(64)
    img.paste(icon, (16, 16), icon)
    d.text((92, 20), "MEHRAN", font=font(24), fill=WHITE)
    d.text((92, 46), "FARSIPLEX", font=font(24), fill=BRAND)
    d.text((18, h - 34), "Persian movies & series", font=font(13, bold=False), fill=MUTED)
    return img


def make_logo(size=600):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Glow disc.
    for r, alpha in ((size * 0.47, 26), (size * 0.38, 34), (size * 0.3, 44)):
        d.ellipse([size / 2 - r, size / 2 - r, size / 2 + r, size / 2 + r],
                  fill=(*BRAND, alpha))
    d.ellipse([size * 0.18, size * 0.18, size * 0.82, size * 0.82],
              fill=(*BRAND, 235))
    d.ellipse([size * 0.21, size * 0.21, size * 0.79, size * 0.79],
              outline=(255, 255, 255, 60), width=max(2, size // 140))
    play_triangle(d, (size * 0.4, size * 0.34, size * 0.72, size * 0.66))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    make_icon().save(OUT / "ic_launcher.png")
    make_banner().save(OUT / "tv_banner.png")
    make_logo().save(OUT / "logo.png")
    print("wrote", *(str(p.relative_to(ROOT)) for p in sorted(OUT.iterdir())))


if __name__ == "__main__":
    main()
