"""Draws the Gem Case and Ender Gem Case art (our own, MIT like the rest of this port): block textures, block model,
item model and the Gem Case screen sheet (textures/gui/gem_case.png, laid out for GemCaseScreen). Re-run to rebuild.

The case is a cabinet with drawers, a velvet tray (where GemCaseTileRenderer draws the stored gems, about 14 px up)
and a glass lid, matching GemCaseBlock's shape (full cube with a recess at 13-15 px)."""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/apotheosis'

PALETTES = {
    'gem_case': {
        'wood': [(74, 47, 29), (86, 56, 35), (98, 64, 40)], 'wood_dark': (52, 32, 20), 'wood_light': (122, 82, 52),
        'velvet': [(84, 36, 92), (96, 44, 104), (72, 30, 80)], 'knob': (226, 182, 84), 'knob_dark': (150, 110, 40),
        'glass': (205, 232, 240),
    },
    'ender_gem_case': {
        'wood': [(36, 26, 52), (44, 32, 64), (52, 38, 74)], 'wood_dark': (22, 16, 32), 'wood_light': (92, 70, 128),
        'velvet': [(30, 90, 84), (38, 108, 100), (24, 72, 68)], 'knob': (176, 240, 226), 'knob_dark': (80, 170, 150),
        'glass': (215, 190, 240),
    },
}


def grain(px, x, y, cols, rnd):
    px[x, y] = cols[rnd.randrange(len(cols))] + (255,)


def side(p, rnd):
    """Rows 0-2: trim band (frame walls, lid edge); rows 3-15: planked cabinet side."""
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            grain(px, x, y, p['wood'], rnd)
    for x in range(16):
        for y in range(3):
            px[x, y] = (p['wood_light'] if y == 0 else p['wood'][2]) + (255,)
        px[x, 3] = p['wood_dark'] + (255,)
        px[x, 15] = p['wood_dark'] + (255,)
    for y in range(3, 16):
        px[0, y] = p['wood_dark'] + (255,)
        px[15, y] = p['wood_dark'] + (255,)
        for x in (5, 10):
            px[x, y] = p['wood_dark'] + (255,)
    return im


def front(p, rnd):
    """Rows 3-15 are the cabinet front: two drawers with knobs."""
    im = side(p, rnd)
    d = ImageDraw.Draw(im)
    for top in (4, 10):
        d.rectangle([2, top, 13, top + 4], fill=p['wood'][1] + (255,), outline=p['wood_dark'] + (255,))
        d.line([3, top + 1, 12, top + 1], fill=p['wood_light'] + (255,))
        d.rectangle([7, top + 2, 8, top + 3], fill=p['knob'] + (255,))
        im.putpixel((8, top + 3), p['knob_dark'] + (255,))
    return im


def velvet(p, rnd):
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            grain(px, x, y, p['velvet'], rnd)
    return im


def glass(p):
    """Lid: wood frame, see-through glass (partial alpha, so the block renders translucent) and two glints."""
    im = Image.new('RGBA', (16, 16), p['glass'] + (54,))
    px = im.load()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = p['wood_light'] + (255,)
    for i in range(4):
        px[3 + i, 6 - i] = (255, 255, 255, 150)
        px[4 + i, 7 - i] = (255, 255, 255, 90)
    for i in range(3):
        px[10 + i, 12 - i] = (255, 255, 255, 120)
    return im


def bottom(p, rnd):
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            grain(px, x, y, p['wood'], rnd)
    return im


def model(name):
    t = f'apotheosis:block/{name}'
    wall = {'side': '#side'}
    faces = lambda ups=True: {f: {'texture': '#side'} for f in ('north', 'east', 'south', 'west')}
    elements = [
        {'from': [0, 0, 0], 'to': [16, 13, 16], 'faces': {
            'north': {'texture': '#front', 'cullface': 'north'}, 'east': {'texture': '#side', 'cullface': 'east'},
            'south': {'texture': '#side', 'cullface': 'south'}, 'west': {'texture': '#side', 'cullface': 'west'},
            'up': {'texture': '#velvet'}, 'down': {'texture': '#bottom', 'cullface': 'down'}}},
        # Frame around the tray (13-15 px), 1 px walls.
        {'from': [0, 13, 0], 'to': [16, 15, 1], 'faces': faces()},
        {'from': [0, 13, 15], 'to': [16, 15, 16], 'faces': faces()},
        {'from': [0, 13, 1], 'to': [1, 15, 15], 'faces': faces()},
        {'from': [15, 13, 1], 'to': [16, 15, 15], 'faces': faces()},
        # Glass lid.
        {'from': [0, 15, 0], 'to': [16, 16, 16], 'faces': {
            'north': {'texture': '#side', 'cullface': 'north'}, 'east': {'texture': '#side', 'cullface': 'east'},
            'south': {'texture': '#side', 'cullface': 'south'}, 'west': {'texture': '#side', 'cullface': 'west'},
            'up': {'texture': '#glass', 'cullface': 'up'}}},
    ]
    return {'parent': 'minecraft:block/block', 'textures': {
        'particle': f'{t}_side', 'side': f'{t}_side', 'front': f'{t}_front', 'velvet': f'{t}_velvet',
        'glass': f'{t}_glass', 'bottom': f'{t}_bottom'}, 'elements': elements}


# ---- Screen sheet (307x256), regions used by GemCaseScreen ----
BG, HI, SH, OUT = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (0, 0, 0, 255)
SLOT_FILL, SLOT_DARK, SLOT_LIGHT = (139, 139, 139, 255), (55, 55, 55, 255), (255, 255, 255, 255)


def panel(d, x0, y0, w, h):
    x1, y1 = x0 + w - 1, y0 + h - 1
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=BG)
    d.line([x0 + 2, y0, x1 - 3, y0], fill=OUT); d.line([x0 + 2, y1, x1 - 2, y1], fill=OUT)
    d.line([x0, y0 + 2, x0, y1 - 3], fill=OUT); d.line([x1, y0 + 3, x1, y1 - 2], fill=OUT)
    d.point([(x0 + 1, y0 + 1), (x1 - 1, y1 - 1), (x1 - 2, y0 + 1), (x0 + 1, y1 - 2), (x1 - 1, y0 + 2)], fill=OUT)
    d.rectangle([x0 + 2, y0 + 1, x1 - 3, y0 + 2], fill=HI); d.rectangle([x0 + 1, y0 + 2, x0 + 2, y1 - 3], fill=HI)
    d.rectangle([x0 + 3, y1 - 2, x1 - 2, y1 - 1], fill=SH); d.rectangle([x1 - 2, y0 + 3, x1 - 1, y1 - 2], fill=SH)


def slot(d, x, y, w=18, h=18):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=SLOT_FILL)
    d.line([x, y, x + w - 2, y], fill=SLOT_DARK); d.line([x, y, x, y + h - 2], fill=SLOT_DARK)
    d.line([x + 1, y + h - 1, x + w - 1, y + h - 1], fill=SLOT_LIGHT); d.line([x + w - 1, y + 1, x + w - 1, y + h - 1], fill=SLOT_LIGHT)


def sheet(left_panel_height):
    im = Image.new('RGBA', (307, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    panel(d, 0, 0, 176, 230)
    # Search field (the EditBox draws at 16,16, 110x11; brown text, so a parchment field).
    d.rectangle([14, 14, 127, 28], fill=(232, 220, 192, 255), outline=SLOT_DARK)
    d.line([15, 28, 127, 28], fill=SLOT_LIGHT); d.line([127, 15, 127, 28], fill=SLOT_LIGHT)
    for r in range(3):
        for c in range(6):
            slot(d, 20 + 18 * c, 30 + 19 * r)
    slot(d, 12, 28, 6, 105)  # scrollbar track
    for p in range(6):
        slot(d, 20 + 18 * p, 90)  # purities, cracked to perfect
    # Little arrows between neighbouring purities, above the upgrade buttons.
    slot(d, 141, 17)  # filter slot (category item)
    slot(d, 141, 98)  # gem input
    # Funnel hint in the filter slot, gem hint in the input slot (faint).
    hint = (120, 120, 120, 255)
    d.polygon([(145, 22), (154, 22), (151, 27), (151, 31), (148, 31), (148, 27)], outline=hint)
    d.polygon([(149, 102), (153, 106), (149, 112), (145, 106)], outline=hint)
    for r in range(3):
        for c in range(9):
            slot(d, 7 + 18 * c, 147 + 18 * r)
    for c in range(9):
        slot(d, 7 + 18 * c, 205)
    # Left material panel at (198, 0), drawn 65 px left of the main panel.
    panel(d, 198, 0, 65, left_panel_height)
    for r in range(3):
        for c in range(2):
            slot(d, 198 + 19 + 18 * c, 20 + 18 * r)
    # Material panel header: three small gems.
    for i, col in enumerate([(120, 200, 255, 255), (200, 120, 255, 255), (255, 200, 90, 255)]):
        cx = 198 + 22 + 10 * i
        d.polygon([(cx, 8), (cx + 3, 11), (cx, 15), (cx - 3, 11)], fill=col, outline=SLOT_DARK)
    # Scroll handle at (303, 0): active, then inactive at (303, 12).
    for y0, fill in ((0, (220, 220, 220, 255)), (12, (150, 150, 150, 255))):
        d.rectangle([303, y0, 306, y0 + 11], fill=fill)
        d.line([303, y0, 305, y0], fill=HI); d.line([303, y0, 303, y0 + 10], fill=HI)
        d.line([304, y0 + 11, 306, y0 + 11], fill=SH); d.line([306, y0 + 1, 306, y0 + 11], fill=SH)
    # Upgrade button at (291, 29): normal, inactive (y 45), hover (y 61).
    for y0, body, arrow in ((29, (198, 198, 198, 255), (214, 160, 40, 255)),
                            (45, (160, 160, 160, 255), (110, 110, 110, 255)),
                            (61, (220, 220, 255, 255), (255, 210, 70, 255))):
        d.rectangle([291, y0, 306, y0 + 15], fill=body, outline=OUT)
        d.line([292, y0 + 1, 305, y0 + 1], fill=HI); d.line([292, y0 + 1, 292, y0 + 14], fill=HI)
        d.line([293, y0 + 14, 305, y0 + 14], fill=SH); d.line([305, y0 + 2, 305, y0 + 14], fill=SH)
        d.polygon([(298, y0 + 3), (302, y0 + 8), (300, y0 + 8), (300, y0 + 12), (297, y0 + 12), (297, y0 + 8), (295, y0 + 8)], fill=arrow)
    return im


def main():
    for name, p in PALETTES.items():
        rnd = random.Random(name)
        tex = ROOT / 'textures/block'
        side(p, rnd).save(tex / f'{name}_side.png')
        front(p, random.Random(name)).save(tex / f'{name}_front.png')
        velvet(p, rnd).save(tex / f'{name}_velvet.png')
        glass(p).save(tex / f'{name}_glass.png')
        bottom(p, rnd).save(tex / f'{name}_bottom.png')
        (ROOT / f'models/block/{name}.json').write_text(json.dumps(model(name), indent=2) + '\n')
        (ROOT / f'models/item/{name}.json').write_text(json.dumps({'parent': f'apotheosis:block/{name}'}, indent=2) + '\n')
    (ROOT / 'textures/gui').mkdir(exist_ok=True)
    sheet(81).save(ROOT / 'textures/gui/gem_case.png')
    print('drew gem case art')


if __name__ == '__main__':
    main()
