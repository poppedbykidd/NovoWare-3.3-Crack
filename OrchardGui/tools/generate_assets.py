"""
Generates every texture the Orchard click GUI uses.

  * glyph atlases + metrics (with kerning) for each text style, rendered from Red Hat Display
  * sidebar / search icons, rasterised from Material icons at their exact on-screen size
  * anti-aliased circle textures used for rounded-rectangle corners
  * the red glow behind the "Self Destruct" button

The generated files are already committed under src/main/resources/assets/orchardgui/gui,
so you only need to run this if you change a font size, icon or colour.

Requirements:  pip install pillow playwright   (and a Chromium for playwright)
Run from this folder:  python generate_assets.py
"""
import asyncio
import base64
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "orchardgui", "gui")
CHROMIUM = os.environ.get("CHROMIUM_PATH")  # optional, playwright finds its own otherwise

# name -> (weight, pixel size, vertical stretch). Font, weights, sizes and stretch were fitted
# against the reference screenshot (the original client draws its text slightly taller than wide).
FONTS = {
    "title": ("Bold", 32.25, 1.01),
    "version": ("Bold", 17.75, 0.875),
    "category": ("Bold", 18.25, 1.11),
    "search": ("SemiBold", 22.75, 0.945),
    "tab": ("SemiBold", 22.25, 0.99),
    "name": ("Bold", 20.4, 0.985),
    "desc": ("Medium", 13.25, 1.0),
    "button": ("Bold", 15.75, 1.08),
    "button_alt": ("Medium", 15.0, 1.1),
}

# name -> (svg file or None for hand-drawn, width, height). Sizes measured from the screenshot.
ICONS = {
    "combat": ("combat.svg", 23, 22),
    "movement": ("movement.svg", 23, 28),
    "player": ("player.svg", 25, 28),
    "render": ("render.svg", 25, 19),
    "hud": ("laptop.svg", 28, 22),
    "configs": ("configs.svg", 24, 20),
    "binds": ("binds.svg", 25, 18),
    "themes": (None, 25, 24),
    "search": ("search.svg", 24, 24),
}

CIRCLE_RADII = list(range(1, 17))
CHARS = [chr(c) for c in range(32, 127)]


def font_path(weight):
    return os.path.join(HERE, "fonts", "RedHatDisplay-%s.otf" % weight)


SUBPIXELS = 4  # each glyph is baked at 4 horizontal sub-pixel offsets for even spacing


def build_font(name, weight, size, stretch):
    ss = 8  # glyphs are drawn 8x larger (and `stretch` taller) and box-filtered down
    big = ImageFont.truetype(font_path(weight), size * stretch * ss, layout_engine=ImageFont.Layout.RAQM)
    font = ImageFont.truetype(font_path(weight), size, layout_engine=ImageFont.Layout.RAQM)
    hx = stretch * ss  # big-font units per design pixel, horizontally
    glyphs = []
    for ch in CHARS:
        adv = font.getlength(ch)
        box = big.getbbox(ch, anchor="ls")
        for sub in range(SUBPIXELS):
            if ch == " " or box[2] <= box[0]:
                glyphs.append((ch, sub, None, 0, 0, adv))
                continue
            x0, x1 = math.floor(box[0] / hx) - 1, math.ceil(box[2] / hx) + 2
            y0, y1 = math.floor(box[1] / ss) - 1, math.ceil(box[3] / ss) + 1
            canvas = Image.new("L", (round((x1 - x0) * hx), (y1 - y0) * ss), 0)
            pen = (-x0 + sub / SUBPIXELS) * hx
            ImageDraw.Draw(canvas).text((pen, -y0 * ss), ch, font=big, fill=255, anchor="ls")
            img = canvas.resize((x1 - x0, y1 - y0), Image.BOX)
            glyphs.append((ch, sub, img, x0, y0, adv))

    # shelf-pack into a 512 wide atlas
    width, x, y, row_h, placed = 512, 1, 1, 0, []
    for ch, sub, img, xo, yo, adv in glyphs:
        if img is None:
            placed.append((ch, sub, 0, 0, 0, 0, 0, 0, adv, None))
            continue
        w, h = img.size
        if x + w + 1 > width:
            x, y, row_h = 1, y + row_h + 1, 0
        placed.append((ch, sub, x, y, w, h, xo, yo, adv, img))
        x += w + 1
        row_h = max(row_h, h)
    height = 1
    while height < y + row_h + 1:
        height *= 2

    atlas = Image.new("RGBA", (width, height), (255, 255, 255, 0))
    for p in placed:
        if p[9] is not None:
            atlas.paste(Image.new("RGBA", p[9].size, (255, 255, 255, 255)), (p[2], p[3]), p[9])
    atlas.save(os.path.join(OUT, "font_%s.png" % name))

    ascent, descent = font.getmetrics()
    lines = ["size %s" % size, "atlas %d %d" % (width, height), "metrics %d %d" % (ascent, descent),
             "subpixels %d" % SUBPIXELS]
    for ch, sub, ax, ay, w, h, xo, yo, adv, _ in placed:
        lines.append("g %d %d %d %d %d %d %d %d %.4f" % (ord(ch), sub, ax, ay, w, h, xo, yo, adv))
    single = {c: font.getlength(c) for c in CHARS}
    for a in CHARS:
        for b in CHARS:
            k = font.getlength(a + b) - single[a] - single[b]
            if abs(k) >= 0.05:
                lines.append("k %d %d %.4f" % (ord(a), ord(b), k))
    with open(os.path.join(OUT, "font_%s.txt" % name), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def build_circles():
    for r in CIRCLE_RADII:
        ss = 16
        big = Image.new("L", (2 * r * ss, 2 * r * ss), 0)
        ImageDraw.Draw(big).ellipse((0, 0, 2 * r * ss - 1, 2 * r * ss - 1), fill=255)
        mask = big.resize((2 * r, 2 * r), Image.LANCZOS)
        img = Image.new("RGBA", mask.size, (255, 255, 255, 0))
        img.putalpha(mask)
        img.save(os.path.join(OUT, "circle_%d.png" % r))


def rounded_mask(w, h, r, ss=8):
    big = Image.new("L", (w * ss, h * ss), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, w * ss - 1, h * ss - 1), r * ss, fill=255)
    return big.resize((w, h), Image.LANCZOS)


def build_glow():
    # button is 200x40 with radius 10; the glow texture adds a 14px margin on every side
    m, w, h = 14, 200, 40
    base = Image.new("L", (w + 2 * m, h + 2 * m), 0)
    base.paste(rounded_mask(w, h, 10), (m, m))
    glow = base.filter(ImageFilter.GaussianBlur(5))
    glow = glow.point(lambda v: int(v * 0.85))
    img = Image.new("RGBA", glow.size, (255, 255, 255, 0))
    img.putalpha(glow)
    img.save(os.path.join(OUT, "glow_button.png"))


def draw_themes_icon(w, h):
    ss = 16
    big = Image.new("L", (w * ss, h * ss), 0)
    d = ImageDraw.Draw(big)
    stroke = 2.1 * ss
    r = 7.4 * ss
    cx, cy = w * ss / 2, h * ss / 2
    for dx, dy in ((0, -4.3), (-4.6, 3.6), (4.6, 3.6)):
        x, y = cx + dx * ss, cy + dy * ss
        d.ellipse((x - r, y - r, x + r, y + r), outline=255, width=int(stroke))
    return big.resize((w, h), Image.LANCZOS)


async def render_svgs(jobs):
    from playwright.async_api import async_playwright

    results = {}
    async with async_playwright() as p:
        kwargs = {"executable_path": CHROMIUM} if CHROMIUM else {}
        browser = await p.chromium.launch(**kwargs)
        page = await browser.new_page(viewport={"width": 512, "height": 512})
        for name, svg in jobs.items():
            data = base64.b64encode(open(os.path.join(HERE, "icons", svg), "rb").read()).decode()
            await page.set_content(
                '<html><body style="margin:0;background:transparent">'
                '<div style="width:512px;height:512px;background:#fff;'
                '-webkit-mask:url(data:image/svg+xml;base64,%s) center/contain no-repeat"></div>'
                "</body></html>" % data
            )
            path = os.path.join(OUT, "_tmp_%s.png" % name)
            await page.screenshot(path=path, omit_background=True)
            results[name] = Image.open(path).convert("RGBA").split()[3]
            os.remove(path)
        await browser.close()
    return results


def build_icons():
    rendered = asyncio.run(render_svgs({n: s for n, (s, _, _) in ICONS.items() if s}))
    for name, (svg, w, h) in ICONS.items():
        if svg is None:
            mask = draw_themes_icon(w, h)
        else:
            alpha = rendered[name]
            alpha = alpha.crop(alpha.getbbox())
            mask = alpha.resize((w, h), Image.LANCZOS)
        img = Image.new("RGBA", (w, h), (255, 255, 255, 0))
        img.putalpha(mask)
        img.save(os.path.join(OUT, "icon_%s.png" % name))


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, (weight, size, stretch) in FONTS.items():
        build_font(name, weight, size, stretch)
    build_circles()
    build_glow()
    build_icons()
    print("assets written to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()
