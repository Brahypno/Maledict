"""Original plate and gauntlet pixel plans, read at each surface's UV resolution."""
FRONTS = {
'vertebra': '''011223322110
123343343321
234432234432
233210012332
122100001221
012234432210
123343343321
234332233432
233210012332
122100001221
012223322210
123343343321
234332233432
123210012321
011123321110
000012210000''',
'flank': '''011222222110
123333333221
234443333221
233333332221
233333322211
123333222110
012222211000
001111100000
123333322110
234443332221
233333332221
233333322211
123333222110
012333321100
001222210000
000111100000''',
'rib': '''011111111110
123333333321
234444443332
233333333332
233333333322
123333333221
012222222110
001111111100
011111111110
123333333321
234444333332
233333333322
123333333221
012222222110
001111111100
000000000000''',
'sternum': '''000122221000
001234432100
001234432100
001233332100
001223322100
000113311000
001234432100
001233332100
001223322100
000113311000
001234432100
001233332100
001223322100
000123321000
000012210000
000001100000''',
'shoulder': '''122233332211
233444443332
344444443332
344333333332
343333333332
333333333322
333333333322
333333333222
333333332222
333333322221
333333222211
333332222111
233332221111
123333321110
011222211000
000111100000''',
'upper': '''011222222110
123333333221
234444333221
234333333221
234333333221
233333333221
233333333221
233333333221
233333333221
233333333221
233333332221
233333332211
233333322211
123333322110
011222211000
000111100000''',
'forearm': '''011222222110
123333333221
234444433221
234333333221
234333333221
234333333221
234333332221
234333332221
234333332221
234333332221
234333332221
233333332211
233333322211
123444332110
011222211000
000111100000''',
'pectoral': '''122233332211
234444443322
344443333332
343333333332
333333333332
333333333332
333333333322
333333333322
333333333222
333333332222
333333322221
333333222211
233332222110
123444332100
011222211000
000111100000''',
'scapula': '''011222222110
123444443221
234333333221
233333333221
233333333221
233333333221
233333332221
233333322221
233333222221
233332222221
233322222221
233222222211
232222222211
123333322110
011222211000
000111100000''',
'shell': '''011222222110
123444443221
234333333221
233333333221
233333333221
233333332221
233333322221
233333222221
233332222221
233322222221
233222222221
232222222221
232222222211
123333322110
011222211000
000111100000''',
'palm': '''111111111111
233333333332
344444444443
333333333332
333333333332
333333333332
333333333332
233333333332
223333333332
122333333332
112233333322
111223333322
111122333322
111112333321
011111222210
001111111100''',
'finger': '''122222222221
234444444432
343333333332
333333333332
333333333332
333333333332
233333333332
233333333332
233333333322
233333333322
123333333221
112333332211
011222222110
000111111000
011222222110
001111111100''',
}
FRONTS={role:text.splitlines() for role,text in FRONTS.items()}
assert all(len(rows)==16 and all(len(row)==12 for row in rows) for rows in FRONTS.values())

def shade(role,face,x,y,width,height):
    xx=min(11,x*12//width); yy=min(15,y*16//height)
    value=int(FRONTS[role][yy][xx])
    # Tube lanes are genuinely separate surface strips, not repeating front maps.
    if face in ('back','lane2','lane3'):
        value=min(value,3)
        if xx>7: value=max(1,value-1)
        if yy>=14: value=0 if yy==15 else 1
        if role in ('scapula','shell') and 3<=xx<=4 and 3<=yy<=11:
            value=3 if xx==3 else 1
        if role=='vertebra':
            if 5<=xx<=6: value=1 if yy not in (2,7,12) else 2
            elif xx in (4,7) and 2<=yy<=13: value=3
    elif face in ('inner','lane4','lane5'):
        value=2 if 2<yy<13 else 1
        if xx<2 and 3<yy<12: value=3
        if yy>=14: value=0
    elif face=='outer':
        value=min(3,value)
        if xx==0 and 2<yy<12: value=4
    elif face.startswith('cap') or face in ('top','bottom'):
        rim=min(xx,11-xx,yy,15-yy)
        value=3 if rim<2 else 2
        if face in ('cap0','bottom'): value=max(0,value-1)
    return value


# Broad masses, rather than repeated horizontal stripes. One drawing represents
# one physical plate; the three lower segments already supply the repetition.
VOLUME = {
    'vertebra': (
        '112233332211', '234455554432', '345677766543', '456788776543',
        '456777665432', '345666554321', '234555443211', '223455432211',
        '223456543211', '234567654321', '234566654321', '223455543211',
        '122344432211', '112233322110', '011122211100', '001111110000'),
    'flank': (
        '112233332211', '234455554321', '456677665432', '567787765432',
        '566777654321', '455666543211', '344555432211', '233444332211',
        '234455443211', '345666543211', '456776543211', '455666543211',
        '344555432211', '233444322110', '122333221100', '011122110000'),
    'rib': (
        '112233332211', '234455554432', '456677776543', '567888776543',
        '567777665432', '456666554321', '345555443211', '234444332211',
        '233444332211', '223333322211', '222333322211', '222333322211',
        '122333222111', '112222221110', '011111111100', '000111110000'),
    'sternum': (
        '001122221100', '012344443210', '123566654321', '234677654321',
        '234676543211', '123565432211', '123565432211', '123566543211',
        '123566543211', '123565432211', '123454322110', '123454322110',
        '012343221100', '012232211000', '001122110000', '000111100000'),
}
BONE_TONES = ('343040','494354','605969','797383','92909e',
              'aaaab5','bfc2cb','d1d6dd','e0e5e9')
RELIC_TONES = ('292333','393042','4a3e52','5c4f64','706477',
               '867c90','9d95a7','b4afbe','cbc9d3')

# A branching axial relief, drawn as one connected motif rather than islands
# of highlights. The adjacent recessed edge supplies thickness at native UVs.
LOWER_RELIEF = (
    ((5.2,2),(5.2,12.8)),
    ((1.5,3),(2.3,4.5),(3.8,5.8),(5.2,6.2),(7.2,5.5),(9.2,3)),
    ((2.3,8),(3.3,9.8),(5.2,11.0),(7.1,9.8),(8.3,8)))


def relief_distance(x,y):
    distance=100
    for path in LOWER_RELIEF:
        for (ax,ay),(bx,by) in zip(path,path[1:]):
            dx,dy=bx-ax,by-ay
            t=max(0,min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)))
            distance=min(distance,((x-ax-t*dx)**2+(y-ay-t*dy)**2)**.5)
    return distance


def color(role, face, x, y, width, height, violet=False):
    """Sample the authored volume at native UV density, with shared midtones.

    Half steps prevent a white crest from jumping straight into purple shadow.
    No random grain or repeated decorative marks are added to the bone.
    """
    rows = VOLUME.get(role)
    if rows is None:
        rows = [''.join(str(int(v)*2) for v in row) for row in FRONTS[role]]
    u = (x + .5) * 11 / width
    v = (y + .5) * 15 / height
    ix, iy = int(u), int(v)
    fx, fy = u-ix, v-iy
    a = int(rows[iy][ix])*(1-fx) + int(rows[iy][min(11,ix+1)])*fx
    b = int(rows[min(15,iy+1)][ix])*(1-fx) + int(rows[min(15,iy+1)][min(11,ix+1)])*fx
    tone = a*(1-fy)+b*fy
    if role == 'vertebra':
        # A single quiet, elongated plane follows each existing segment.
        # Two painted bulges per tiny UV island made the abdomen look tiled.
        s, t = (x+.5)/width, (y+.5)/height
        def smooth(a,b,value):
            q=max(0,min(1,(value-a)/(b-a)))
            return q*q*(3-2*q)
        tone = 6.0 - .55*s - .35*t
        tone -= 1.25*(1-smooth(0,.22,s))
        tone -= 1.55*smooth(.72,1,s)
        tone -= 1.2*(1-smooth(0,.16,t))
        tone -= .65*smooth(.8,1,t)
        if violet and face in ('front','back'):
            # Fractional coverage joins diagonal strokes across texels, while
            # the offset shadow makes them carved ridges rather than decals.
            ridge=1-smooth(.25,1.35,relief_distance(s*12,t*16))
            recess=1-smooth(.2,1.45,relief_distance(s*12-.7,t*16-.7))
            tone+=1.8*ridge-1.15*recess
    if face in ('back','lane2','lane3'):
        tone = tone*.78 + .35
    elif face in ('inner','lane4','lane5'):
        tone = tone*.57 + .25
    elif face == 'outer':
        tone = tone*.85
    elif face.startswith('cap') or face in ('top','bottom'):
        # End grain is a restrained bevel surrounding a recessed centre.
        rim = min(u,11-u,v,15-v)
        tone = 4.7 if rim < 1.4 else 3.2
        if face in ('cap0','bottom'): tone -= 1.3
    tone = max(0,min(8,tone if role=='vertebra' else round(tone*2)/2))
    palette = RELIC_TONES if violet else BONE_TONES
    low = int(tone); high = min(8,low+1); blend = tone-low
    return [(int(palette[low][k:k+2],16)*(1-blend)+
             int(palette[high][k:k+2],16)*blend)/255 for k in (0,2,4)]
