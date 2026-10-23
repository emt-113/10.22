"""Generate ScreenRhythm pearl-planet launcher icons (legacy mipmaps + notification).

Design mirrors res/drawable/ic_launcher_{background,foreground}.xml:
- background: cream white -> mist lavender -> pale ice blue diagonal gradient
- planet: mint -> ice blue -> pearl pink -> lavender diagonal gradient sphere
- highlight, pearl rim, tilted glass orbit, satellite bead, moon dot, star dust
Legacy mipmap PNGs serve Android 7.x/8.x (minSdk 24); adaptive vector layers
serve API 26+. Notification icons are white alpha silhouettes.
"""
from PIL import Image, ImageDraw
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "res")

MASTER = 432  # 108 units * 4 px/unit

BG_STOPS = [(0.0, (253, 251, 247)), (0.55, (234, 231, 248)), (1.0, (220, 240, 248))]
SPHERE_STOPS = [(0.0, (143, 220, 190)), (0.34, (169, 221, 236)),
                (0.67, (247, 207, 224)), (1.0, (201, 180, 238))]


def lerp_stops(stops, t):
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        if t <= p1:
            f = 0.0 if p1 == p0 else (t - p0) / (p1 - p0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1))
    return stops[-1][1]


def diagonal_gradient(size, stops):
    img = Image.new("RGB", (size, size))
    dr = ImageDraw.Draw(img)
    max_t = 2.0  # diagonal coverage: t goes 0..2 across the square (x+y)/(2*size)
    for y in range(size):
        t = (y / (2.0 * size)) * max_t * 0.5
        # single-pass per row is enough visually; add x-blend by drawing rows then columns is overkill.
        c = lerp_stops(stops, min(1.0, (y + size / 2.0) / (2.0 * size) * 1.0))
        dr.line([(0, y), (size, y)], fill=c)
    # blend along x as well (cheap two-pass approximation of a diagonal ramp)
    overlay = Image.new("L", (size, size), 0)
    od = ImageDraw.Draw(overlay)
    for x in range(size):
        od.line([(x, 0), (x, size)], fill=int(110 * x / size))
    img2 = Image.new("RGB", (size, size))
    dr2 = ImageDraw.Draw(img2)
    for x in range(size):
        c = lerp_stops(stops, min(1.0, (x) / size))
        dr2.line([(x, 0), (x, size)], fill=c)
    img = Image.composite(img2, img, overlay.point(lambda v: v))
    return img


def radial_tile(radius, max_alpha):
    """White radial falloff tile (RGBA) with peak alpha at center."""
    d = max(1, int(radius * 2))
    tile = Image.new("RGBA", (d, d), (0, 0, 0, 0))
    px = tile.load()
    for y in range(d):
        for x in range(d):
            dist = ((x - d / 2 + 0.5) ** 2 + (y - d / 2 + 0.5) ** 2) ** 0.5 / radius
            a = int(max_alpha * max(0.0, 1.0 - dist) ** 1.6)
            px[x, y] = (255, 255, 255, a)
    return tile


def orbit_points(cx, cy, rx, ry):
    import math
    a = math.radians(-24)
    pts = []
    for deg in range(0, 360, 2):
        th = math.radians(deg)
        x = cx + rx * math.cos(th)
        y = cy + ry * math.sin(th)
        xr = cx + (x - cx) * math.cos(a) - (y - cy) * math.sin(a)
        yr = cy + (x - cx) * math.sin(a) + (y - cy) * math.cos(a)
        pts.append((xr, yr))
    return pts


def draw_scene(base, scale=1.0, offset=(0, 0)):
    """Draw planet + orbit + satellite + moon on an RGBA image (108-unit space).

    Layer order: orbit back half -> sphere -> highlight -> orbit front half ->
    satellite -> moon/dust, so the ring reads as wrapping around the sphere.
    """
    S = MASTER / 108.0
    import math
    dr = ImageDraw.Draw(base, "RGBA")
    cx, cy = 54 * S + offset[0], 54 * S + offset[1]
    r = 20 * S * scale
    rx, ry = 27 * S * scale, 9.5 * S * scale
    pts = orbit_points(cx, cy, rx, ry)
    # back half of the orbit (unrotated angles 180..360 -> upper arc)
    back = pts[90:180] + [pts[90]]
    dr.line(back, fill=(255, 255, 255, 120), width=max(2, int(1.6 * S)))

    # sphere with diagonal 4-stop gradient, clipped to circle
    gx0, gy0 = cx - r, cy - r
    span = max(1, int(round(2 * r)))
    grad = Image.new("RGBA", (span, span), (0, 0, 0, 0))
    gd = ImageDraw.Draw(grad)
    for y in range(span):
        t = y / span if span else 0
        c = lerp_stops(SPHERE_STOPS, t) + (255,)
        gd.line([(0, y), (span, y)], fill=c)
    mask = Image.new("L", (span, span), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([0, 0, span, span], fill=255)
    base.paste(grad, (int(gx0), int(gy0)), mask)

    # key light: soft radial sheen on the upper-left of the sphere
    hr = r * 0.62
    tile = radial_tile(hr, 105)
    base.alpha_composite(tile, (int(cx - r * 0.38 - hr), int(cy - r * 0.42 - hr)))

    dr = ImageDraw.Draw(base, "RGBA")
    # pearl rim
    dr.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(255, 255, 255, 175), width=max(2, int(0.9 * S)))

    # front half of the orbit (unrotated angles 0..180 -> lower arc)
    front = pts[0:91]
    dr.line(front, fill=(255, 255, 255, 150), width=max(2, int(1.6 * S)))
    # satellite at rightmost vertex, rotated
    satx, saty = pts[0]
    sr = 1.9 * S
    dr.ellipse([satx - sr, saty - sr, satx + sr, saty + sr], fill=(255, 255, 255, 255))

    # moon dot + dust
    mr = 2.2 * S
    dr.ellipse([76 * S - mr, 32 * S - mr, 76 * S + mr, 32 * S + mr], fill=(255, 255, 255, 230))
    for (dx, dy, drr, a) in [(33, 39, 1.1, 140), (72, 69, 0.9, 115)]:
        rr = drr * S
        dr.ellipse([dx * S - rr, dy * S - rr, dx * S + rr, dy * S + rr], fill=(255, 255, 255, a))


def make_launcher(square=True, round_mask=False):
    img = diagonal_gradient(MASTER, BG_STOPS).convert("RGBA")
    draw_scene(img)
    if round_mask:
        mask = Image.new("L", (MASTER, MASTER), 0)
        md = ImageDraw.Draw(mask)
        md.ellipse([0, 0, MASTER, MASTER], fill=255)
        img.putalpha(mask)
    elif square:
        mask = Image.new("L", (MASTER, MASTER), 0)
        md = ImageDraw.Draw(mask)
        rad = int(MASTER * 0.21)
        md.rounded_rectangle([0, 0, MASTER, MASTER], radius=rad, fill=255)
        img.putalpha(mask)
    return img


def make_notification(white=True, large=False):
    size = 432
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img, "RGBA")
    import math
    S = size / 108.0
    cx, cy, r = 54 * S, 54 * S, 20 * S
    col = (255, 255, 255, 255)
    # filled sphere (solid white body reads best at status-bar size)
    dr.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
    # orbit + satellite
    rx, ry = 27 * S, 9.5 * S
    pts = []
    a = math.radians(-24)
    for deg in range(0, 360, 2):
        th = math.radians(deg)
        x = cx + rx * math.cos(th)
        y = cy + ry * math.sin(th)
        xr = cx + (x - cx) * math.cos(a) - (y - cy) * math.sin(a)
        yr = cy + (x - cx) * math.sin(a) + (y - cy) * math.cos(a)
        pts.append((xr, yr))
    dr.line(pts + [pts[0]], fill=col, width=max(3, int(2.2 * S)))
    satx, saty = pts[0]
    sr = 1.9 * S
    dr.ellipse([satx - sr, saty - sr, satx + sr, saty + sr], fill=col)
    if large:
        # punch a transparent sphere core so the ring reads at large sizes
        core = r * 0.62
        dr.ellipse([cx - core, cy - core, cx + core, cy + core], fill=(0, 0, 0, 0))
    return img


def main():
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    launcher = make_launcher(square=True, round_mask=False)
    rnd = make_launcher(square=False, round_mask=True)
    sq = make_launcher(square=True, round_mask=False)
    for dpi, side in densities.items():
        out = os.path.join(RES, "mipmap-" + dpi)
        os.makedirs(out, exist_ok=True)
        launcher.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))
        rnd.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_round.png"))
        sq.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_square.png"))

    # notification icons (white silhouette)
    notif_sizes = {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}
    small = make_notification(large=False)
    big = make_notification(large=True)
    for dpi, side in notif_sizes.items():
        out = os.path.join(RES, "drawable-" + dpi)
        os.makedirs(out, exist_ok=True)
        small.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_notification.png"))
        big.resize((side * 2, side * 2), Image.LANCZOS).save(os.path.join(out, "ic_notification_large.png"))

    print("icons generated")


if __name__ == "__main__":
    main()
