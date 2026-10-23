# -*- coding: utf-8 -*-
"""Chroma-key the AI planet sprite (v2, component-based).

v1 failed: the sphere's pale mint top is within JPEG tolerance of the cream bg,
so floodfill + a blanket pocket sweep ate the sphere. v2 strategy:
  1. border floodfill with TIGHT threshold (bg is flat cream, thresh 12 is safe)
  2. label every remaining bg-colored pixel region (scipy)
  3. remove only SMALL regions (stars / moon / cream pockets between ring and
     sphere); the sphere's huge pale top region survives
  4. feather alpha, autocrop, measure sphere bbox, resize, save
ASCII only (project rule #9).
"""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy import ndimage

SRC = "tools/icon_source_planet_sprite.jpg"
DST = "app/src/main/res/drawable-nodpi/companion_planet_sprite.png"
MAGENTA = (255, 0, 255)
SMALL_REGION_CAP = 60000  # px; pockets/moon/stars are smaller, sphere top is way larger

img = Image.open(SRC).convert("RGB")
w, h = img.size
px = img.load()

def avg(points):
    rs = gs = bs = 0
    for x, y in points:
        r, g, b = px[x, y]
        rs += r; gs += g; bs += b
    n = len(points)
    return (rs // n, gs // n, bs // n)

m = 6
seeds = [(m, m), (w - 1 - m, m), (m, h - 1 - m), (w - 1 - m, h - 1 - m),
         (w // 2, m), (w // 2, h - 1 - m), (m, h // 2), (w - 1 - m, h // 2)]
bg = avg(seeds)
print("bg color:", bg, "source:", (w, h))

work = img.copy()
def try_fill(x, y, thresh):
    try:
        ImageDraw.floodfill(work, (x, y), MAGENTA, thresh=thresh)
    except Exception:
        pass

for x in range(0, w, 10):
    try_fill(x, 0, 12); try_fill(x, h - 1, 12)
for y in range(0, h, 10):
    try_fill(0, y, 12); try_fill(w - 1, y, 12)

# label remaining bg-colored regions and remove the small ones
arr = np.asarray(work).astype(np.int16)
bgv = np.array(bg, dtype=np.int16)
dist = np.abs(arr - bgv).max(axis=2)
bgish = (dist <= 10) & ~((arr[:, :, 0] == 255) & (arr[:, :, 1] == 0) & (arr[:, :, 2] == 255))
labels, n = ndimage.label(bgish)
removed = 0
sizes = ndimage.sum(bgish, labels, range(1, n + 1))
for i, s in enumerate(sizes, start=1):
    if s < SMALL_REGION_CAP:
        bgish[labels == i] = False
        removed += 1
print("bg regions:", n, "removed small:", removed)
mask_arr = ~bgish  # True = keep

# border flood results must also stay removed (magenta areas)
mag = (arr[:, :, 0] == 255) & (arr[:, :, 1] == 0) & (arr[:, :, 2] == 255)
mask_arr = mask_arr & ~mag

mask = Image.fromarray((mask_arr * 255).astype(np.uint8), "L")

# keep only the largest opaque component (planet+ring+satellite are connected);
# floating moon/stars are separate islands and the forest scene draws its own
lab2, n2 = ndimage.label(mask_arr)
if n2 > 1:
    sizes2 = ndimage.sum(mask_arr, lab2, range(1, n2 + 1))
    main = int(sizes2.argmax()) + 1
    print("opaque components:", n2, "main size:", int(sizes2.max()),
          "dropped:", [int(s) for s in sizes2 if s != sizes2.max()])
    mask_arr = lab2 == main
    mask = Image.fromarray((mask_arr * 255).astype(np.uint8), "L")

mask = mask.filter(ImageFilter.GaussianBlur(1.2))

rgba = img.convert("RGBA")
rgba.putalpha(mask)
bbox = mask.getbbox()
if bbox is None:
    print("ERROR: empty mask"); sys.exit(1)
rgba = rgba.crop(bbox)
cw, ch = rgba.size
print("content bbox:", bbox, "size:", (cw, ch))

ap = rgba.getchannel("A").load()
col_run = []
for x in range(cw):
    run = best = 0
    for y in range(ch):
        if ap[x, y] > 128:
            run += 1; best = max(best, run)
        else:
            run = 0
    col_run.append(best)
max_run = max(col_run)
sx = [x for x, r in enumerate(col_run) if r >= max_run * 0.72]
sx0, sx1 = (sx[0], sx[-1]) if sx else (0, cw - 1)
row_run = []
for y in range(ch):
    run = best = 0
    for x in range(sx0, sx1 + 1):
        if ap[x, y] > 128:
            run += 1; best = max(best, run)
        else:
            run = 0
    row_run.append(best)
sy = [y for y, r in enumerate(row_run) if r >= max_run * 0.55]
sy0, sy1 = (sy[0], sy[-1]) if sy else (0, ch - 1)
print("sphere bbox in content: x %d..%d y %d..%d (of %dx%d)" % (sx0, sx1, sy0, sy1, cw, ch))
print("sphere ratios: cx=%.3f cy=%.3f rw=%.3f rh=%.3f" % (
    (sx0 + sx1) / 2.0 / cw, (sy0 + sy1) / 2.0 / ch,
    (sx1 - sx0) / float(cw), (sy1 - sy0) / float(ch)))

scale = 720.0 / max(cw, ch)
if scale < 1:
    rgba = rgba.resize((int(cw * scale), int(ch * scale)), Image.LANCZOS)
rgba.save(DST)
print("saved:", DST, rgba.size)
