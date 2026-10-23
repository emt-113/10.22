"""Generate Pausia (息律) launcher icons: pearl liquid-glass open ring.

Design matches res/drawable/ic_launcher_foreground.xml: a thick ring band
(Router 26 / Rinner 17.5 in 108-unit space) open at the upper right (gap
centered at -45deg, half-width 30deg). Pearl white wraps into mint toward the
lower-left; a mint droplet sits near the upper tip. Legacy mipmap PNGs serve
Android 7.x/8.x (minSdk 24); the adaptive vector layers serve API 26+.
"""
from PIL import Image, ImageDraw
import math
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "res")

MASTER = 432  # 108 units * 4 px/unit
CX = CY = 54.0
R_OUT, R_IN = 26.0, 14.0
GAP_CENTER, GAP_HALF = -45.0, 30.0  # degrees; gap spans [-75, -15]

BG_STOPS = [(0.0, (253, 251, 247)), (0.55, (234, 231, 248)), (1.0, (220, 240, 248))]
PEARL = (252, 251, 246)
MINT = (176, 216, 192)


def lerp_stops(stops, t):
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        if t <= p1:
            f = 0.0 if p1 == p0 else (t - p0) / (p1 - p0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1))
    return stops[-1][1]


def diagonal_gradient(size, stops):
    img = Image.new("RGB", (size, size))
    dr = ImageDraw.Draw(img)
    for y in range(size):
        c = lerp_stops(stops, min(1.0, (y + size / 2.0) / (2.0 * size)))
        dr.line([(0, y), (size, y)], fill=c)
    overlay = Image.new("L", (size, size), 0)
    od = ImageDraw.Draw(overlay)
    for x in range(size):
        od.line([(x, 0), (x, size)], fill=int(110 * x / size))
    img2 = Image.new("RGB", (size, size))
    dr2 = ImageDraw.Draw(img2)
    for x in range(size):
        c = lerp_stops(stops, min(1.0, x / size))
        dr2.line([(x, 0), (x, size)], fill=c)
    return Image.composite(img2, img, overlay)


def in_gap(angle_deg):
    """True when the angle lies inside the open gap (angles wrap at 360)."""
    a = (angle_deg - (GAP_CENTER - GAP_HALF)) % 360.0
    return a < 2 * GAP_HALF


def ring_pixel_color(angle_deg, radial_t):
    """Pearl->mint by angle, subtle glass shading by radial position."""
    w = 0.24 + 0.58 * max(0.0, math.cos(math.radians(angle_deg - 135.0))) ** 1.5
    base = tuple(round(p + (m - p) * w) for p, m in zip(PEARL, MINT))
    # glass shading: bright outer edge, soft shadow hugging the inner edge
    shade = 1.0
    if radial_t > 0.86:
        shade = 1.0 + 0.10 * (radial_t - 0.86) / 0.14
    elif radial_t < 0.30:
        shade = 1.0 - 0.16 * (0.30 - radial_t) / 0.30
    return tuple(max(0, min(255, round(c * shade))) for c in base)


def draw_ring(img, white_silhouette=False):
    S = MASTER / 108.0
    cx, cy = CX * S, CY * S
    ro, ri = R_OUT * S, R_IN * S
    dr = ImageDraw.Draw(img, "RGBA")

    if white_silhouette:
        # alpha-only C-ring + droplet (status bar / monochrome use)
        width = ro - ri
        mid = (ro + ri) / 2.0
        pts = []
        for deg in range(-15, 286, 2):
            th = math.radians(deg)
            pts.append((cx + mid * math.cos(th), cy + mid * math.sin(th)))
        dr.line(pts, fill=(255, 255, 255, 255), width=int(round(width)))
        d = 3.4 * S
        dr.ellipse([61.0 * S - d, 29.8 * S - d, 61.6 * S + d, 25.6 * S + d],
                   fill=(255, 255, 255, 255))
        return

    # per-pixel ring band with pearl->mint angular coloring + glass shading
    x0, y0 = int(cx - ro - 2), int(cy - ro - 2)
    x1, y1 = int(cx + ro + 2), int(cy + ro + 2)
    region = img.crop((x0, y0, x1, y1)).convert("RGBA")
    px = region.load()
    for py in range(region.height):
        dy = (y0 + py) - cy
        for px_x in range(region.width):
            dx = (x0 + px_x) - cx
            dist = math.hypot(dx, dy)
            if dist < ri - 0.5 or dist > ro + 0.5:
                continue
            angle = math.degrees(math.atan2(dy, dx)) % 360.0
            if in_gap(angle):
                continue
            radial_t = (dist - ri) / (ro - ri)
            c = ring_pixel_color(angle, radial_t)
            a = 255
            # anti-aliased edges
            if dist > ro - 0.5:
                a = int(255 * (ro + 0.5 - dist))
            elif dist < ri + 0.5:
                a = int(255 * (dist - (ri - 0.5)))
            px[px_x, py] = c + (a,)
    img.paste(region, (x0, y0))

    # glassy specular arcs + soft mint pool
    dr = ImageDraw.Draw(img, "RGBA")
    mid = (ro + ri) / 2.0
    arc_pts_a = [(cx + 21.5 * S * math.cos(math.radians(t)),
                  cy + 21.5 * S * math.sin(math.radians(t)))
                 for t in range(200, 301, 2)]
    dr.line(arc_pts_a, fill=(255, 255, 255, 170), width=max(2, int(2.2 * S)))
    arc_pts_b = [(cx + 21.5 * S * math.cos(math.radians(t)),
                  cy + 21.5 * S * math.sin(math.radians(t)))
                 for t in range(10, 116, 2)]
    dr.line(arc_pts_b, fill=(255, 255, 255, 89), width=max(2, int(1.6 * S)))
    pool_r = 8.5 * S
    pool = radial_tile(pool_r, 102)
    img.alpha_composite(pool, (int(37.5 * S - pool_r), int(63.5 * S - pool_r)))

    # mint droplet at the upper tip
    d = 3.4 * S
    tile = radial_tile(d, 255)
    tint = Image.new("RGBA", tile.size, (156, 207, 175, 255))
    drop = Image.composite(tint, Image.new("RGBA", tile.size, (234, 247, 238, 255)),
                           tile.split()[3].point(lambda v: v))
    mask = Image.new("L", tile.size, 0)
    md = ImageDraw.Draw(mask)
    md.ellipse([0, 0, tile.size[0] - 1, tile.size[1] - 1], fill=255)
    img.paste(drop, (int(61.0 * S - d), int(29.5 * S - d)), mask)
    dr.ellipse([61.0 * S - d, 29.5 * S - d, 61.0 * S + d, 29.5 * S + d],
               outline=(255, 255, 255, 200), width=max(1, int(0.5 * S)))


def radial_tile(radius, max_alpha):
    d = max(1, int(radius * 2))
    tile = Image.new("RGBA", (d, d), (0, 0, 0, 0))
    px = tile.load()
    for y in range(d):
        for x in range(d):
            dist = ((x - d / 2 + 0.5) ** 2 + (y - d / 2 + 0.5) ** 2) ** 0.5 / radius
            a = int(max_alpha * max(0.0, 1.0 - dist) ** 1.6)
            px[x, y] = (255, 255, 255, a)
    return tile


def make_launcher(round_mask=False):
    img = diagonal_gradient(MASTER, BG_STOPS).convert("RGBA")
    draw_ring(img)
    mask = Image.new("L", (MASTER, MASTER), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([0, 0, MASTER, MASTER], fill=255)
    else:
        md.rounded_rectangle([0, 0, MASTER, MASTER], radius=int(MASTER * 0.21), fill=255)
    img.putalpha(mask)
    return img


def make_notification():
    img = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    draw_ring(img, white_silhouette=True)
    return img


def main():
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    launcher = make_launcher(round_mask=False)
    rnd = make_launcher(round_mask=True)
    for dpi, side in densities.items():
        out = os.path.join(RES, "mipmap-" + dpi)
        os.makedirs(out, exist_ok=True)
        launcher.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher.png"))
        rnd.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_round.png"))
        launcher.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_launcher_square.png"))

    notif_sizes = {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}
    small = make_notification()
    for dpi, side in notif_sizes.items():
        out = os.path.join(RES, "drawable-" + dpi)
        os.makedirs(out, exist_ok=True)
        small.resize((side, side), Image.LANCZOS).save(os.path.join(out, "ic_notification.png"))
        small.resize((side * 2, side * 2), Image.LANCZOS).save(os.path.join(out, "ic_notification_large.png"))

    print("icons generated")


if __name__ == "__main__":
    main()
