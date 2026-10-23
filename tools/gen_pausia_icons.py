"""Generate Pausia launcher assets directly from the AI-rendered ring image.

Source: tools/icon_source_pausia_ring.jpg (pearl liquid-glass open ring on a
uniform cream background). The background is chroma-keyed to transparency with
a feathered threshold, then reused as:
  - adaptive foreground layers (keyed ring, all densities)
  - adaptive monochrome layer (white silhouette)
  - legacy mipmap launcher PNGs (ring over the sampled solid cream, masked)
  - notification silhouettes (white C-ring)
"""
from PIL import Image, ImageDraw
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "res")
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon_source_pausia_ring.jpg")
MASTER = 432  # 108-unit adaptive canvas at 4x

BG_SOLID = (253, 252, 248)
KEY_T0, KEY_T1 = 16.0, 52.0  # feathered chroma-key thresholds


def keyed_master():
    src = Image.open(SRC).convert("RGB")
    w, h = src.size
    bg = src.getpixel((8, 8))
    rgba = src.convert("RGBA")
    px = rgba.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y][:3]
            dist = ((r - bg[0]) ** 2 + (g - bg[1]) ** 2 + (b - bg[2]) ** 2) ** 0.5
            if dist <= KEY_T0:
                a = 0
            elif dist >= KEY_T1:
                a = 255
            else:
                a = int(255 * (dist - KEY_T0) / (KEY_T1 - KEY_T0))
            px[x, y] = (r, g, b, a)
    bbox = rgba.getbbox()
    pad = int(max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * 0.03)
    bbox = (max(0, bbox[0] - pad), max(0, bbox[1] - pad),
            min(w, bbox[2] + pad), min(h, bbox[3] + pad))
    return rgba.crop(bbox), bg


def fit_center(layer, content, box):
    """Paste `content` scaled to fit `box` centered on a transparent `layer`."""
    scale = min(box / content.width, box / content.height)
    new = content.resize((max(1, int(content.width * scale)),
                          max(1, int(content.height * scale))), Image.LANCZOS)
    layer.alpha_composite(new, ((layer.width - new.width) // 2,
                                (layer.height - new.height) // 2))
    return layer


def white_silhouette(content):
    out = Image.new("RGBA", content.size, (0, 0, 0, 0))
    px, po = content.load(), out.load()
    for y in range(content.height):
        for x in range(content.width):
            a = px[x, y][3]
            if a:
                po[x, y] = (255, 255, 255, a)
    return out


def main():
    ring, bg = keyed_master()

    # adaptive foreground: ring scaled into the 66-unit safe zone
    fg_sizes = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    fg_master = fit_center(Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0)), ring, int(66 / 108 * MASTER))
    mono_master = white_silhouette(fg_master)
    for dpi, side in fg_sizes.items():
        out = os.path.join(RES, "drawable-" + dpi)
        os.makedirs(out, exist_ok=True)
        fg_master.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_foreground.png"))
        mono_master.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_monochrome.png"))

    # legacy launcher: ring composited over the sampled solid cream, masked
    legacy_sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    full = Image.new("RGBA", (MASTER, MASTER), BG_SOLID + (255,))
    full = fit_center(full, ring, int(86 / 108 * MASTER))
    square_mask = Image.new("L", (MASTER, MASTER), 0)
    ImageDraw.Draw(square_mask).rounded_rectangle([0, 0, MASTER, MASTER],
                                                  radius=int(MASTER * 0.21), fill=255)
    round_mask = Image.new("L", (MASTER, MASTER), 0)
    ImageDraw.Draw(round_mask).ellipse([0, 0, MASTER, MASTER], fill=255)
    for dpi, side in legacy_sizes.items():
        out = os.path.join(RES, "mipmap-" + dpi)
        os.makedirs(out, exist_ok=True)
        sq = full.copy(); sq.putalpha(square_mask)
        rd = full.copy(); rd.putalpha(round_mask)
        sq.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))
        rd.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_round.png"))
        sq.save(os.path.join(out, "ic_launcher_square.png"))

    # notification silhouettes (white)
    notif_sizes = {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}
    notif_master = white_silhouette(fit_center(Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0)),
                                               ring, int(80 / 108 * MASTER)))
    for dpi, side in notif_sizes.items():
        out = os.path.join(RES, "drawable-" + dpi)
        os.makedirs(out, exist_ok=True)
        notif_master.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_notification.png"))
        notif_master.resize((side * 2, side * 2), Image.LANCZOS).save(os.path.join(out, "ic_notification_large.png"))

    print("pausia icons generated; bg =", bg)


if __name__ == "__main__":
    main()
