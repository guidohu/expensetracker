"""
Builds the Play Store listing graphics (captioned phone screenshots + feature graphic) from the
full-resolution renders StoreAssetsTest writes to app/src/test/snapshots/images/.

    python3 play-store-assets/make_listing_graphics.py

Needs Pillow and macOS's Avenir Next font. Output goes to play-store-assets/listing/ as 24-bit PNGs
(no alpha channel, which Play Console requires).
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RENDERS = ROOT / "app/src/test/snapshots/images"
OUT = ROOT / "play-store-assets/listing"
FONT = "/System/Library/Fonts/Avenir Next.ttc"
BOLD, DEMI, MEDIUM = 0, 2, 5

TOP_COLOR = (27, 184, 207)    # icon gradient, light end
BOTTOM_COLOR = (11, 100, 121)  # icon gradient, dark end (#0B6479)
SUBTEXT = (224, 247, 250)

# (output name, StoreAssetsTest render, headline, subline) — Play shows these in order, and most
# people only see the first two or three, so lead with the strongest benefits.
SHOTS = [
    ("1-insights", "shot_stats", "See where your money goes",
     "Charts by category, mood and need vs. want"),
    ("2-expenses", "shot_expenses", "Track every expense in seconds",
     "Simple daily log, grouped by day"),
    ("3-mood", "shot_addExpense", "How did that purchase feel?",
     "Mood tags reveal emotional spending"),
    ("4-wishlist", "shot_wishlist", "Think before you buy",
     "A wishlist that separates needs from wants"),
    ("5-budget", "shot_settings", "Stay on budget",
     "Monthly budget, reminders and no-spend streaks"),
    ("6-private", "shot_onboarding", "Private by design",
     "No account, no ads. Your data stays on your phone."),
]


def font(index: int, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT, size, index=index)


def gradient(size: tuple[int, int]) -> Image.Image:
    w, h = size
    column = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / (h - 1)
        column.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(TOP_COLOR, BOTTOM_COLOR)))
    return column.resize(size)


def wrap(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    lines, line = [], ""
    for word in text.split():
        candidate = f"{line} {word}".strip()
        if draw.textlength(candidate, font=fnt) <= max_width or not line:
            line = candidate
        else:
            lines.append(line)
            line = word
    return lines + [line]


def draw_centered(draw, lines, fnt, y, width, fill, line_gap) -> int:
    for line in lines:
        draw.text(((width - draw.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
        y += fnt.size + line_gap
    return y


def phone(screen: Image.Image, width: int) -> Image.Image:
    """The screen inside a simple dark device frame, with transparent rounded corners."""
    bezel = round(width * 0.022)
    screen_w = width - 2 * bezel
    screen = screen.convert("RGB").resize((screen_w, round(screen.height * screen_w / screen.width)), Image.LANCZOS)
    height = screen.height + 2 * bezel
    outer_r, inner_r = round(width * 0.085), round(width * 0.065)

    frame = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, width - 1, height - 1), outer_r, fill=(20, 24, 28, 255))
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, screen.width - 1, screen.height - 1), inner_r, fill=255)
    frame.paste(screen, (bezel, bezel), mask)
    return frame


def paste_with_shadow(canvas: Image.Image, item: Image.Image, pos: tuple[int, int], blur: int) -> None:
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    alpha = item.split()[3].point(lambda a: a * 0.45)
    shadow.paste((0, 0, 0, 255), (pos[0], pos[1] + blur // 2), alpha)
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(blur)))
    canvas.alpha_composite(item, pos)


def render(name: str) -> Image.Image:
    return Image.open(RENDERS / f"com.github.guidohu.expensetracker_StoreAssetsTest_{name}.png")


def screenshot(render_name: str, headline: str, subline: str) -> Image.Image:
    w, h = 1080, 1920
    canvas = gradient((w, h)).convert("RGBA")
    draw = ImageDraw.Draw(canvas)
    y = draw_centered(draw, wrap(draw, headline, font(BOLD, 84), 940), font(BOLD, 84), 120, w, "white", 8)
    draw_centered(draw, wrap(draw, subline, font(MEDIUM, 42), 900), font(MEDIUM, 42), y + 24, w, SUBTEXT, 10)

    device = phone(render(render_name), 760)
    paste_with_shadow(canvas, device, ((w - device.width) // 2, 500), 40)  # runs off the bottom edge
    return canvas.convert("RGB")


def feature_graphic() -> Image.Image:
    w, h = 1024, 500
    canvas = gradient((w, h)).convert("RGBA")
    draw = ImageDraw.Draw(canvas)

    icon = Image.open(ROOT / "play-store-assets/icon-rounded.png").convert("RGBA").resize((112, 112), Image.LANCZOS)
    paste_with_shadow(canvas, icon, (64, 92), 14)
    draw.text((64, 228), "Expense Tracker", font=font(BOLD, 58), fill="white")
    y = 312
    for line in ["Budget, track and understand", "your spending"]:
        draw.text((64, y), line, font=font(DEMI, 32), fill=SUBTEXT)
        y += 42
    draw.text((64, y + 22), "No ads  ·  No account  ·  Private", font=font(MEDIUM, 24), fill=SUBTEXT)

    device = phone(render("shot_stats"), 300)
    paste_with_shadow(canvas, device, (w - device.width - 80, 56), 24)
    return canvas.convert("RGB")


def main() -> None:
    OUT.mkdir(exist_ok=True)
    for out_name, render_name, headline, subline in SHOTS:
        screenshot(render_name, headline, subline).save(OUT / f"screenshot-{out_name}.png", optimize=True)
    feature_graphic().save(OUT / "feature-graphic.png", optimize=True)
    print(f"Wrote {len(SHOTS)} screenshots + feature graphic to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
