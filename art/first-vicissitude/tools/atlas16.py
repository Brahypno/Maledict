"""Original part-oriented pixel art, packed in a 16px grid; no external image inputs."""
import re
import zlib
from relic_surfaces import paint_relic


class PartAtlas:
    def __init__(self, palette):
        self.palette = palette
        self.used = set()
        self.regions = {}
        self.base = [0.0] * (256 * 256 * 4)
        self.emission = [0.0] * (256 * 256 * 4)

    def allocate(self, name, material):
        # Only mirrored instances share a patch, not every object of a material.
        key = re.sub(r'\b(left|right)\b', 'paired', name)
        key = f'{material}:{key}'
        if key in self.regions:
            return self.regions[key]
        height = 4 if material == 8 else 2 if 9 <= material <= 12 or 'Vestment pennant' in name else 1
        for y in range(17 - height):
            for x in range(16):
                cells = {(x, y + row) for row in range(height)}
                if not cells & self.used:
                    self.used.update(cells)
                    region = dict(name=key, material=material, x=x*16, y=y*16,
                                  width=16, height=height*16)
                    self.regions[key] = region
                    self.paint(region)
                    return region
        raise ValueError(f'256px atlas exhausted while allocating {name}')

    def paint(self, region):
        if paint_relic(self, region): return
        name, material = region['name'], region['material']
        seed = zlib.crc32(name.encode('utf-8'))
        color = self.palette[material][1]
        base = tuple(int(color[k:k+2], 16)/255 for k in (0, 2, 4))
        glow = self.palette[material][2]
        height = region['height']
        bone = (.77, .80, .85)
        recess = (.23, .20, .29)
        for y in range(height):
            for x in range(16):
                rgb, alpha = base, 1
                shade = .95 if 3 <= x <= 9 else .70 if x > 11 else .82
                # Readable single-pixel edges, paired with a recessed shadow.
                if x == 2: shade = 1.10
                if x == 12: shade = .52
                if y < 2: shade *= .72
                if material == 8:
                    # Four authored 16x16 zones: socket, shaft, outer vane, tip.
                    zone = y // 16
                    ridge = 4 + (1 if zone == 2 else 0)
                    shade = .94 if x < 10 else .65
                    if x <= ridge:
                        rgb = bone
                        shade = (.66, .87, 1.06, .94, .79, .72)[min(x, 5)]
                    if x == ridge + 1:
                        rgb, shade = recess, 1
                    if x == ridge + 2: shade = 1.08
                    # The same bone-over-violet layering as shoulders and forearms.
                    if zone == 0 and y < 6: shade *= .68 + y*.045
                    if x == 12 and (y+seed) % 19 in (4, 5, 6): shade = .42
                    if x == 11 and (y+seed) % 19 in (4, 5): shade = 1.16
                    if x in (ridge-1, ridge) and (y+seed) % 23 in (8, 9):
                        rgb, shade = base, .72
                    if zone == 2 and y % 16 in (7, 8) and 8 <= x <= 11:
                        shade = .59 if y % 16 == 7 else 1.08
                elif 9 <= material <= 12:
                    # Each feather's shaft and barb rhythm are drawn at native pixels.
                    shaft = 7 + (1 if y > 20 else 0)
                    band = (y + abs(x-shaft)//2 + seed % 3) % 5
                    shade = (.78 if x < shaft else .61) + (.13 if band == 0 else 0)
                    if x == shaft: shade = 1.18
                    if x == shaft+1: shade = .43
                    if x in (1, 14): shade = 1.0
                    if x in (0, 15) and (y+seed) % 9 < 2: alpha = 0
                    if material == 11 and y > 23 and x > 12 and y % 6 < 3: alpha = 0
                elif material in (4, 5, 6, 15):
                    # Tracks and crystal faces keep their own restrained specular marks.
                    shade = 1.05 if 4 <= x <= 7 else .74
                    if x in (3, 8): shade = .49
                    if y in (4, 11) and 5 <= x <= 6: shade = 1.16
                else:
                    lower = name.lower()
                    if any(word in lower for word in ('knuckle', 'fingertip', 'palm', 'thumb')):
                        # Knuckle top, inset finger face, curled underside.
                        if y in (3, 4) and 3 <= x <= 11: shade = 1.11
                        if y == 5 and 4 <= x <= 11: shade = .65
                        if y > 11: shade *= .72
                        if x in (4, 10) and 7 <= y <= 10: shade = .75
                    elif any(word in lower for word in ('cuff', 'elbow', 'wrist')):
                        if y in (3, 12): shade = 1.08
                        if y in (4, 11): shade = .48
                        if y in (7, 8) and x == 8: shade = 1.13
                    elif any(word in lower for word in ('pectoral', 'clavicular', 'shoulder', 'thoracic')):
                        # Overlapping broad faces with a stepped lower contact shadow.
                        edge = 10 + (x//4 + seed % 2) % 3
                        if y == edge: shade = .52
                        if y == edge-1: shade = 1.06
                        if y > edge: shade *= .85
                    elif any(word in lower for word in ('crest', 'belly', 'humerus', 'strut')):
                        if x == 4: shade = 1.08
                        if x in (9, 10) and 5 < y < 12: shade = .65
                        if y > 12: shade *= .76
                    else:
                        if y == 11 and 4 <= x <= 11: shade = .51
                        if y == 12 and 4 <= x <= 10: shade = 1.02
                    # Part-specific chipped pixels, never a tiled noise overlay.
                    cy = 5 + seed % 5
                    cx = 6 + (seed // 7) % 4
                    if y in (cy, cy+1) and x == cx: shade *= .65
                    if y == cy and x == cx-1: shade = 1.10
                    if material in (2, 3, 14): shade = .18 + .82*shade
                offset = ((region['y']+y)*256 + region['x']+x)*4
                for channel in range(3):
                    value = min(1, rgb[channel]*shade)
                    self.base[offset+channel] = value
                    self.emission[offset+channel] = value*glow
                self.base[offset+3] = alpha
                self.emission[offset+3] = alpha if glow else 0

    def report(self):
        return dict(size=getattr(self,'size',256), prototype_cell_size=16,
                    allocated_pixels=sum(r['width']*r['height'] for r in self.regions.values()),
                    regions=list(self.regions.values()))
