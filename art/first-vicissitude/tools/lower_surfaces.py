"""Dense, original lower-body surfaces; preserve every upper-body texel.

The old 256-square atlas occupies the lower-left quadrant of a 512 atlas at
its original density. Only new lower-body islands receive four times the
linear drawing resolution. Shading and ornament are evaluated at that density.
"""
import json
import math

SIZE=512
PREFIXES={
    'Lower ossuary bridge':'bridge', 'Lumbar connection':'shaft',
    'Vertebral spindle':'shaft', 'Ventral ossuary segment':'plate',
    'Dorsal vertebral plate':'dorsal', 'Transverse ossuary flange':'flange',
    'Floating pelvic relic':'pelvic', 'Vestment pennant':'cloth',
}


def smooth(a,b,x):
    t=max(0,min(1,(x-a)/(b-a)))
    return t*t*(3-2*t)


def mix(a,b,t):
    t=max(0,min(1,t))
    return [x*(1-t)+y*t for x,y in zip(a,b)]


def stroke(u,v,path):
    nearest=10
    for (ax,ay),(bx,by) in zip(path,path[1:]):
        dx,dy=bx-ax,by-ay
        t=max(0,min(1,((u-ax)*dx+(v-ay)*dy)/(dx*dx+dy*dy)))
        nearest=min(nearest,math.hypot(u-ax-t*dx,v-ay-t*dy))
    return nearest


def paint(kind,face,material,u,v):
    if kind=='cloth':
        # Broad folds correspond to the added quarter-width mesh creases.
        fold=.35+.26*math.cos((u-.23)*math.tau*2)
        fold-=.22*(1-smooth(0,.055,v))
        fold-=.08*v
        rgb=mix((32,27,43),(111,92,126),fold)
        border=min(abs(u-.10),abs(u-.91))
        thread=(1-smooth(.006,.018,border))*smooth(.045,.07,v)*(1-smooth(.86,.94,v))
        shadow=1-smooth(.007,.021,min(abs(u-.12),abs(u-.93)))
        rgb=mix(rgb,(35,28,46),shadow*.42)
        rgb=mix(rgb,(158,139,175),thread*.7)
        emblem=((.51,.20),(.66,.29),(.51,.39),(.37,.29),(.51,.20))
        emblem2=((.51,.39),(.61,.47),(.51,.58),(.41,.47),(.51,.39),(.51,.66))
        d=min(stroke(u,v,emblem),stroke(u,v,emblem2))
        # Deliberate small gap in the upper ring, plus an inset dark thread.
        motif=(1-smooth(.004,.014,d))*(0 if u>.60 and .25<v<.28 else 1)
        rgb=mix(rgb,(156,137,175),motif*.72)
        rgb=mix(rgb,(27,23,37),(1-smooth(.001,.012,abs(v-.027)))*.4)
        hems=(24,27,29,26,30,31,27,23,26,29,25,28,30,26,23,25)
        p=u*15; k=min(14,int(p)); f=p-k
        hem=(hems[k]*(1-f)+hems[k+1]*f)/32
        alpha=float(v<hem)
        if (u<.07 and .38<v<.44) or (u>.94 and .53<v<.60): alpha=0
        if abs(u-(.53+(v-.64)*.5))<.014 and .64<v<.74: alpha=0
        return rgb,alpha

    # Convex bone shoulder with a darker embedded root and narrow edge lights.
    across=math.sin(math.pi*u)**.65
    body=.35+.34*across-.13*v-.22*(1-smooth(0,.11,v))
    if face in ('back','lane2','lane3'): body-=.10
    if face in ('inner','lane4','lane5','bottom','cap0'): body-=.20
    bone=material in (2,3,14)
    rgb=mix((49,43,62),(175,180,192) if bone else (128,114,145),body)
    if face in ('front','back') and kind in ('plate','bridge','dorsal','pelvic'):
        arch=.19+.22*(1-abs(2*u-1))
        arc=math.exp(-((v-arch)/.038)**2)
        root=math.exp(-((v-arch-.047)/.035)**2)
        # The main keel and paired swept arms are continuous forms, not tiles.
        keel=math.exp(-((u-.48)/(.055+.025*(1-v)))**2)*smooth(.04,.14,v)*(1-smooth(.83,.96,v))
        flank=math.exp(-((u-.565)/.029)**2)*smooth(.17,.3,v)*(1-smooth(.8,.94,v))
        rgb=mix(rgb,(41,34,54),root*.55+flank*.42)
        rgb=mix(rgb,(188,191,202),max(keel*.69,arc*.49))
        # A fine recessed chevron fits inside the lower part of the plate.
        engraving=stroke(u,v,((.28,.59),(.48,.72),(.72,.57)))
        rgb=mix(rgb,(54,43,68),(1-smooth(.003,.013,engraving))*.65)
    elif face.startswith('cap') or face in ('top','bottom'):
        rim=min(u,1-u,v,1-v)
        rgb=mix(rgb,(155,151,173),(1-smooth(.025,.095,rim))*.3)
    return rgb,1


def refine_lower_surfaces(atlas,meshes):
    old_regions=dict(atlas.regions)
    for field in ('base','emission'):
        old=getattr(atlas,field); expanded=[0.0]*(SIZE*SIZE*4)
        for y in range(256): expanded[y*SIZE*4:y*SIZE*4+1024]=old[y*1024:(y+1)*1024]
        setattr(atlas,field,expanded)
    atlas.size=SIZE
    rows=[(1<<256)-1 if y<256 else 0 for y in range(SIZE)]
    maps={}

    def region(kind,old):
        key='lower:'+kind+':'+old['name']
        if key in maps: return maps[key]
        w,h=old['width']*4,old['height']*4
        for y in range(SIZE-h-1):
            for x in range(SIZE-w-1):
                mask=((1<<(w+2))-1)<<x
                if all(not rows[yy]&mask for yy in range(y,y+h+2)):
                    for yy in range(y,y+h+2): rows[yy]|=mask
                    r=dict(name=key,x=x+1,y=y+1,width=w,height=h,
                           material=old['material'],surface=old.get('surface','front'),
                           source='lower_surface_detail')
                    maps[key]=r; atlas.regions[key]=r
                    for py in range(h):
                        for px in range(w):
                            rgb,alpha=paint(kind,r['surface'],r['material'],(px+.5)/w,(py+.5)/h)
                            offset=((r['y']+py)*SIZE+r['x']+px)*4
                            atlas.base[offset:offset+4]=[c/255 for c in rgb]+[alpha]
                    return r
        raise ValueError('Lower-body detail exceeds the atlas')

    for obj in meshes:
        kind=next((v for p,v in PREFIXES.items() if obj.name.startswith(p)),None)
        islands=json.loads(obj['surface_islands']) if 'surface_islands' in obj else None
        updated=[]
        for poly in obj.data.polygons:
            old=old_regions[islands[poly.index] if islands else obj['atlas_region']]
            new=region(kind,old) if kind else None
            for loop in poly.loop_indices:
                uv=obj.data.uv_layers.active.data[loop].uv
                if new:
                    u=(uv.x*256-old['x']-.5)/(old['width']-1)
                    v=(uv.y*256-old['y']-.5)/(old['height']-1)
                    uv.x=(new['x']+.5+u*(new['width']-1))/SIZE
                    uv.y=(new['y']+.5+v*(new['height']-1))/SIZE
                else: uv*=.5
            if new: updated.append(new['name'])
        if kind:
            obj['surface_islands']=json.dumps(updated)
            if 'atlas_region' in obj: del obj['atlas_region']
