"""Native 96px crystal drawings with single-texel flowing filigree.

Eight shell patches have dedicated UV regions; geometry and other UVs stay put.
No source image is resized or pasted into the atlas.
"""
import math

SIZE=96
GOLD=('9e623f','d99d63','f2c98b','ffe9bd')
ROSE=('78259d','a72ac3','dc54db','f49be9')
IVORY=('ccb2dd','eed1ec','fff0ef')

# Each shell is independently composed: fan, long flow, crescent, veil,
# cascade, braid, fern and quiet trailing curls. Coordinates are native texels.
PANELS=(
    dict(name='crest_fan',shade=(.18,.48,.20),
         gold=(((20,7),(7,31),(61,37),(37,65)),
               ((37,65),(23,78),(35,90),(56,86)),
               ((28,31),(34,16),(65,20),(64,40))),
         rose=(((33,5),(26,26),(78,23),(60,64)),
               ((60,64),(50,77),(74,83),(80,68))),
         ivory=(((9,23),(13,39),(27,43),(34,45)),),
         curls=((0,64,40,10,0.0,1.35),(0,55,19,7,2.7,1.05),
                (1,77,55,8,.3,1.3),(1,17,59,6,2.4,1.1))),
    dict(name='long_rising_flow',shade=(.42,-.24,.32),
         gold=(((69,7),(91,36),(14,41),(28,71)),
               ((28,71),(37,92),(71,78),(68,62))),
         rose=(((80,11),(95,39),(34,49),(40,72)),
               ((57,7),(72,33),(7,42),(15,68)),
               ((15,68),(18,87),(35,87),(43,82))),
         ivory=(((66,17),(75,41),(24,53),(30,66)),),
         curls=((0,68,62,9,1.6,1.2),(1,27,78,6,2.0,1.15),
                (1,73,28,5,.2,1.15),(0,48,50,4,2.8,1.0))),
    dict(name='open_crescent',shade=(.58,-.36,.15),
         gold=(((73,15),(20,6),(10,69),(58,83)),),
         rose=(((67,24),(34,14),(27,60),(56,71)),
               ((84,25),(61,30),(58,49),(78,56))),
         ivory=(((33,27),(20,45),(25,66),(39,73)),),
         curls=((0,58,83,8,.4,1.0),(1,78,56,7,-1.0,1.3),
                (0,72,16,5,2.0,1.1))),
    dict(name='dark_branch_veil',shade=(.26,.20,.34),
         gold=(((14,83),(22,60),(61,59),(71,21)),),
         rose=(((8,78),(6,41),(40,48),(44,12)),
               ((31,85),(37,65),(83,47),(82,15)),
               ((16,59),(38,31),(11,31),(20,15))),
         ivory=(((68,47),(57,35),(58,21),(66,10)),),
         curls=((1,20,15,6,1.2,1.25),(0,69,24,5,.0,1.0))),
    dict(name='small_scroll_cascade',shade=(.15,.34,.38),
         gold=(((18,13),(71,9),(21,62),(65,81)),),
         rose=(((11,25),(48,27),(14,55),(40,80)),
               ((35,8),(88,25),(43,53),(80,73))),
         ivory=(((30,29),(54,43),(27,67),(48,84)),),
         curls=((0,29,20,7,1.0,1.2),(0,50,46,6,.8,1.3),
                (0,65,81,8,.5,1.25),(1,76,28,5,2.0,1.1),
                (1,79,72,6,1.4,1.15))),
    dict(name='diagonal_braid',shade=(.65,-.46,.20),
         gold=(((10,73),(34,48),(69,54),(82,12)),
               ((21,84),(47,58),(78,64),(88,39))),
         rose=(((10,59),(42,38),(59,48),(71,13)),
               ((28,89),(37,74),(65,78),(76,64))),
         ivory=(((25,59),(44,44),(66,50),(73,35)),),
         curls=((0,80,18,7,.1,1.2),(1,21,39,8,2.1,1.2),
                (1,67,78,5,1.1,1.1))),
    dict(name='fine_fern',shade=(.34,.16,.12),
         gold=(((43,7),(11,30),(62,53),(46,87)),
               ((28,34),(7,43),(11,61),(25,63)),
               ((47,48),(75,36),(81,54),(68,62))),
         rose=(((55,6),(28,27),(80,57),(57,90)),
               ((24,46),(36,40),(22,32),(13,28))),
         ivory=(((35,14),(27,24),(43,34),(49,40)),),
         curls=((0,25,63,6,1.8,1.25),(0,68,62,6,.4,1.25),
                (1,13,28,5,2.5,1.1),(1,72,77,5,1.1,1.0))),
    dict(name='quiet_comet_trails',shade=(.62,-.30,.40),
         gold=(((13,75),(14,37),(75,52),(77,18)),),
         rose=(((21,91),(24,45),(79,65),(88,38)),
               ((10,51),(30,29),(61,47),(64,23))),
         ivory=(((30,73),(35,57),(58,61),(67,49)),),
         curls=((0,78,20,5,2.3,1.3),(1,64,23,7,-.6,1.4),
                (1,32,75,4,2.0,1.0))),
)

# Layered original sigils: long interlaced orbits, short seals and branching
# curls enrich each existing composition. Gold, rose and ivory share no fill.
# Orbit tuples: color, center x/y, radii, rotation, starting angle, open arc.
SIGILS=(
    dict(orbits=((0,47,27,24,10,-.30,.2,5.7),(1,47,29,16,16,.0,.7,5.5),
                 (2,47,29,7,11,.15,.0,5.6),(1,45,48,29,9,.28,.0,5.6),
                 (0,46,64,18,15,-.18,.5,5.6),(1,46,66,10,9,.0,.0,5.7)),
         seals=((0,((47,7),(28,29),(47,42),(65,24),(47,7))),
                (1,((26,43),(49,37),(71,47),(47,55),(26,43))),
                (2,((46,73),(40,81),(46,90),(52,80),(46,73)))),
         branches=((1,((28,26),(7,12),(6,42),(22,45))),
                   (0,((62,34),(89,18),(84,52),(72,55))),
                   (1,((31,61),(7,61),(10,83),(24,80)))),
         curls=((0,18,20,6,1.0,1.1),(1,78,39,5,2.1,1.2),
                (2,25,71,5,.0,1.1),(1,61,77,6,1.5,1.1))),
    dict(orbits=((1,55,19,21,9,.1,.3,5.6),(0,53,34,15,19,-.3,.0,5.9),
                 (2,51,36,7,13,-.3,.2,5.5),(1,41,55,28,11,-.30,.5,5.4),
                 (0,40,70,24,9,.22,.0,5.9),(1,39,82,12,8,-.1,.3,5.7)),
         seals=((0,((25,27),(54,21),(77,32),(49,44),(25,27))),
                (1,((21,63),(43,48),(66,61),(40,73),(21,63))),
                (2,((41,82),(35,89),(43,93),(48,87),(41,82)))),
         branches=((1,((31,23),(8,18),(9,44),(23,46))),
                   (0,((67,46),(91,47),(87,70),(75,73))),
                   (2,((31,62),(10,73),(22,90),(32,85)))),
         curls=((0,19,33,5,2.0,1.1),(1,78,64,6,.0,1.2),
                (0,27,80,5,.8,1.1),(1,71,20,4,1.4,1.15))),
    dict(orbits=((0,50,44,29,33,-.2,.5,5.2),(1,51,45,22,28,-.2,.5,5.2),
                 (2,50,43,14,19,-.2,.4,5.0),(1,51,43,6,12,-.2,.2,5.7),
                 (0,43,69,25,8,.2,.0,5.6),(1,53,24,26,7,-.20,.2,5.7)),
         seals=((1,((24,51),(54,18),(79,49))),
                (0,((27,57),(49,80),(75,55))),
                (2,((40,44),(49,35),(58,44),(49,52),(40,44)))),
         branches=((0,((30,28),(12,10),(8,39),(21,42))),
                   (1,((70,60),(93,66),(86,87),(72,86))),
                   (1,((34,72),(16,68),(15,88),(30,89)))),
         curls=((1,20,26,5,.2,1.3),(0,81,77,5,2.4,1.2),
                (2,27,80,4,1.0,1.0),(0,70,23,4,1.4,1.15))),
    dict(orbits=((1,44,17,18,10,.25,.0,5.8),(0,49,32,12,16,-.4,.3,5.5),
                 (1,46,49,26,10,-.3,.0,5.8),(2,48,50,8,14,.15,.1,5.4),
                 (1,42,68,23,14,.3,.4,5.6),(0,40,80,12,9,-.1,.0,5.8)),
         seals=((1,((27,20),(46,9),(64,23),(43,37),(27,20))),
                (0,((22,48),(48,39),(73,51),(45,59),(22,48))),
                (1,((23,67),(42,56),(64,72),(40,85),(23,67)))),
         branches=((1,((30,36),(8,29),(11,59),(27,60))),
                   (2,((65,40),(84,27),(92,51),(80,56))),
                   (0,((26,76),(10,86),(22,94),(34,87)))),
         curls=((1,17,47,6,.4,1.3),(1,78,42,5,2.0,1.25),
                (0,28,83,4,.2,1.1),(2,62,68,5,1.8,1.0))),
    dict(orbits=((0,46,20,24,9,.12,.2,5.8),(1,47,32,19,15,-.1,.1,5.9),
                 (2,47,33,10,8,.0,.0,5.7),(0,44,50,29,10,.10,.2,5.7),
                 (1,43,65,20,14,-.2,.5,5.6),(0,43,81,10,9,.1,.0,5.8)),
         seals=((1,((20,26),(49,16),(74,31),(44,43),(20,26))),
                (0,((19,49),(46,39),(73,53),(44,64),(19,49))),
                (2,((43,65),(33,76),(43,89),(52,76),(43,65)))),
         branches=((0,((27,36),(8,29),(9,59),(25,62))),
                   (1,((68,37),(90,27),(91,55),(77,58))),
                   (1,((28,68),(12,72),(17,92),(31,87)))),
         curls=((0,18,47,5,1.0,1.2),(1,81,47,6,2.0,1.25),
                (1,25,82,5,.2,1.2),(2,66,70,5,.6,1.1))),
    dict(orbits=((0,47,30,25,12,-.55,.0,5.8),(1,46,35,16,21,.35,.2,5.5),
                 (2,45,37,8,14,-.2,.0,5.8),(0,50,55,26,10,-.40,.0,5.8),
                 (1,41,70,19,13,.3,.2,5.7),(0,37,83,10,6,-.3,.0,5.8)),
         seals=((1,((16,23),(59,15),(76,49),(16,23))),
                (0,((22,55),(72,30),(66,70),(22,55))),
                (2,((27,65),(56,53),(57,78),(27,65)))),
         branches=((0,((31,25),(11,17),(10,47),(24,50))),
                   (1,((71,51),(93,53),(85,74),(76,74))),
                   (1,((25,68),(7,73),(14,92),(28,88)))),
         curls=((1,18,34,5,.2,1.2),(0,80,66,5,1.4,1.2),
                (2,24,81,5,1.0,1.15),(1,69,20,5,2.2,1.1))),
    dict(orbits=((0,46,20,19,11,-.2,.2,5.7),(1,44,34,12,16,.4,.0,5.7),
                 (2,45,35,6,10,.2,.0,5.8),(0,46,51,26,12,.1,.1,5.7),
                 (1,43,67,20,13,-.3,.5,5.8),(0,43,82,10,8,.0,.0,5.7)),
         seals=((1,((23,24),(47,13),(71,28))),
                (0,((22,48),(45,38),(72,54),(43,67),(22,48))),
                (2,((29,73),(44,59),(59,77),(42,91),(29,73)))),
         branches=((1,((30,28),(7,22),(8,48),(25,49))),
                   (0,((62,32),(83,17),(94,40),(78,45))),
                   (1,((30,65),(9,65),(12,86),(29,85))),
                   (1,((59,65),(82,64),(86,85),(68,84)))),
         curls=((0,17,37,5,.8,1.3),(1,82,32,6,2.0,1.2),
                (0,20,77,4,.2,1.2),(1,75,76,5,1.2,1.1))),
    dict(orbits=((1,53,22,24,10,.25,.1,5.7),(0,52,37,18,18,-.2,.0,5.8),
                 (2,53,37,9,11,.1,.0,5.6),(1,49,54,29,9,-.20,.2,5.8),
                 (0,45,69,20,13,.25,.4,5.5),(1,42,82,12,8,-.1,.0,5.8)),
         seals=((0,((25,27),(54,16),(78,33),(49,44),(25,27))),
                (1,((23,54),(52,43),(74,59),(43,71),(23,54))),
                (2,((31,78),(44,68),(56,83),(41,92),(31,78)))),
         branches=((0,((35,31),(12,24),(13,50),(30,52))),
                   (1,((69,43),(91,38),(92,66),(76,68))),
                   (0,((29,68),(10,73),(17,91),(31,85)))),
         curls=((1,21,39,6,.0,1.3),(0,82,55,5,1.8,1.15),
                (1,25,80,5,.4,1.2),(1,72,20,4,2.0,1.25))),
)


def rgb(value):
    return [int(value[k:k+2],16)/255 for k in (0,2,4)]


def line(a,b):
    x,y=map(round,a); bx,by=map(round,b)
    dx=abs(bx-x); dy=-abs(by-y)
    sx=1 if x<bx else -1; sy=1 if y<by else -1
    error=dx+dy
    result=set()
    while True:
        result.add((x,y))
        if (x,y)==(bx,by): return result
        twice=2*error
        if twice>=dy: error+=dy; x+=sx
        if twice<=dx: error+=dx; y+=sy


def bezier(points):
    samples=[]
    for i in range(129):
        t=i/128; s=1-t
        samples.append(tuple(s**3*points[0][k]+3*s*s*t*points[1][k]
                             +3*s*t*t*points[2][k]+t**3*points[3][k] for k in (0,1)))
    return set().union(*(line(a,b) for a,b in zip(samples,samples[1:])))


def thin(pixels):
    """Remove raster corner thickness while keeping eight-connected curves."""
    pixels=set(pixels)
    changed=True
    while changed:
        changed=False
        for phase in (0,1):
            remove=set()
            for x,y in pixels:
                n=[(x+dx,y+dy) in pixels for dx,dy in
                   ((0,1),(1,1),(1,0),(1,-1),(0,-1),(-1,-1),(-1,0),(-1,1))]
                transitions=sum(not n[i] and n[(i+1)%8] for i in range(8))
                if not (2<=sum(n)<=6 and transitions==1): continue
                if phase==0:
                    allowed=not(n[0] and n[2] and n[4]) and not(n[2] and n[4] and n[6])
                else:
                    allowed=not(n[0] and n[2] and n[6]) and not(n[0] and n[4] and n[6])
                if allowed: remove.add((x,y))
            pixels-=remove
            changed|=bool(remove)
    # Skeleton intersections can retain a solid four-texel knot. Open one
    # corner there, favoring the most connected point, to retain hairline width.
    for x,y in sorted(pixels):
        square={(x,y),(x+1,y),(x,y+1),(x+1,y+1)}
        if square<=pixels:
            corner=max(sorted(square),key=lambda p:sum(
                (p[0]+dx,p[1]+dy) in pixels
                for dx in (-1,0,1) for dy in (-1,0,1) if dx or dy))
            pixels.remove(corner)
    return pixels


def drawing(variant):
    plan=PANELS[variant]
    raw=[set().union(*(bezier(points) for points in plan[color]))
         for color in ('gold','rose','ivory')]
    sigil=SIGILS[variant]
    for color,cx,cy,rx,ry,rotation,start,arc in sigil['orbits']:
        points=[]
        for i in range(193):
            a=start+arc*i/192
            x=rx*math.cos(a); y=ry*math.sin(a)
            points.append((cx+x*math.cos(rotation)-y*math.sin(rotation),
                           cy+x*math.sin(rotation)+y*math.cos(rotation)))
        raw[color].update(set().union(*(line(a,b) for a,b in zip(points,points[1:]))))
    for color,points in sigil['seals']:
        raw[color].update(set().union(*(line(a,b) for a,b in zip(points,points[1:]))))
    for color,points in sigil['branches']:
        raw[color].update(bezier(points))
    for color,cx,cy,radius,angle,turns in plan['curls']+sigil['curls']:
        points=[]
        for i in range(97):
            t=i/96; a=angle+math.tau*turns*t
            r=radius*(1-.78*t)
            points.append((cx+r*math.cos(a),cy+r*math.sin(a)))
        raw[color].update(set().union(*(line(a,b) for a,b in zip(points,points[1:]))))
    # No widening, outline, antialias fringe or terminal pads.
    masks=[thin({(x,y) for x,y in mask if 2<=x<SIZE-2 and 2<=y<SIZE-2})
           for mask in raw]
    # Fine colored trails pass behind gold, with a one-texel interruption at
    # crossings instead of a filled junction or a broad multicolor outline.
    occupied=set()
    for i,mask in enumerate(masks):
        clearance={(x+dx,y+dy) for x,y in occupied
                   for dx in (-1,0,1) for dy in (-1,0,1)}
        masks[i]=mask-clearance
        occupied.update(masks[i])
    combined=set().union(*masks)
    for mask in masks+[combined]:
        assert not any({(x+1,y),(x,y+1),(x+1,y+1)}<=mask for x,y in mask), 'Filigree exceeds one texel'
    return masks


def paint(atlas,region,variant):
    gold,rose,ivory=drawing(variant)
    dark=rgb('170821'); middle=rgb('5e147e'); bright=rgb('a33fc0')
    origin,slope,spread=PANELS[variant]['shade']
    for y in range(SIZE):
        for x in range(SIZE):
            u=(x+.5)/SIZE; v=(y+.5)/SIZE
            seam=origin+slope*v
            # Draw the crystal planes directly at native resolution, in stepped
            # mineral tones; the fine ornaments are independent pixel paths.
            plane=.22+.34*math.sin(math.pi*u)+.16*(1-v)
            if u>seam: plane*=.42
            plane=round(plane*16)/16
            color=[a*(1-plane)+b*plane for a,b in zip(dark,middle)]
            if u<seam:
                light=round(max(0,1-abs(u-seam+.09)/spread)*.50*8)/8
                color=[a*(1-light)+b*light for a,b in zip(color,bright)]
            point=(x,y)
            if point in rose: color=rgb(ROSE[(y//12+variant)%len(ROSE)])
            if point in ivory: color=rgb(IVORY[(y//14+variant)%len(IVORY)])
            if point in gold: color=rgb(GOLD[(y//10+variant)%len(GOLD)])
            i=((region['y']+y)*atlas.size+region['x']+x)*4
            atlas.base[i:i+4]=color+[1]
    region['line_width_texels']=1
    region['gold_texels']=len(gold)
    region['rose_texels']=len(rose)
    region['motif']=PANELS[variant]['name']


def refine_head_surfaces(atlas,meshes):
    rows=[0]*atlas.size
    for r in atlas.regions.values():
        mask=((1<<r['width'])-1)<<r['x']
        for y in range(r['y'],r['y']+r['height']): rows[y]|=mask
    for obj in meshes:
        if not obj.name.startswith('Fractured crystal shell '): continue
        old=atlas.regions[obj['atlas_region']]
        variant=int(obj.name.rsplit(' ',1)[-1])
        slot=None
        for y in range(256,atlas.size-SIZE-1):
            for x in range(atlas.size-SIZE-1):
                mask=((1<<(SIZE+2))-1)<<x
                if all(not rows[yy]&mask for yy in range(y,y+SIZE+2)):
                    slot=(x,y,mask); break
            if slot is not None: break
        if slot is None: raise ValueError('Head detail exceeds the atlas')
        x,y,mask=slot
        for yy in range(y,y+SIZE+2): rows[yy]|=mask
        region=dict(name='head:'+old['name'],material=old['material'],
                    x=x+1,y=y+1,width=SIZE,height=SIZE,
                    source='head_surface_detail',surface='shell')
        atlas.regions[region['name']]=region
        paint(atlas,region,variant)
        for loop in obj.data.uv_layers.active.data:
            uv=loop.uv
            u=(uv.x*atlas.size-old['x']-.5)/(old['width']-1)
            v=(uv.y*atlas.size-old['y']-.5)/(old['height']-1)
            uv.x=(region['x']+.5+u*(SIZE-1))/atlas.size
            uv.y=(region['y']+.5+v*(SIZE-1))/atlas.size
        obj['atlas_region']=region['name']
