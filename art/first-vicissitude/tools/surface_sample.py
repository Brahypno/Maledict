"""Authored surface sample. Pixel drawings are source data, not seeded noise.

The first 112 rows retain the prototype; the last 144 hold authored surfaces.
Coordinates in drawings run from the attachment toward the free end.
"""
import json
import math
from ring_surfaces import RINGS, paint_ring
from wing_surfaces import PLANS, primary_pixels
from body_surfaces import FRONTS as BODY_PLANS, shade as body_shade

# 12-pixel wide structural drawings: recess, shade, body, lit plane, edge.
INK = ('302b40', '595269', '9397ad', 'c3ccda', 'e1e3e6')
VIOLET = ('211d30', '393048', '594665', '80668e', 'b2a0bc')
DRAWINGS = {
 'pectoral': '''111111000000
233332211100
344443332210
344444433321
344444443332
333444443332
223344443332
122334443332
012233443321
001223333321
000122333210
000012232100
000001121000
000000110000
000000000000
000000000000''',
 'scapula': '''000111111100
001233333210
012344443321
123444333321
234443322321
234433222321
234332212321
233322112321
233221112321
233211122321
233211223321
123222333210
012233332100
001233221000
000122110000
000011000000''',
 'rib': '''001111111100
012222222210
123333333321
234444433332
234444433332
233333333332
122222222221
011111111110
000000000000
011111111110
122222222221
123333333321
123333333321
012222222210
001111111100
000000000000''',
 'flank': '''011111111100
123333333210
234444433321
234443333321
233333332221
122333322210
011222221100
000111110000
001222221100
012333332210
123444333321
123443333221
123333322210
012332221100
001222110000
000111100000''',
 'sternum': '''000011110000
000123321000
001234432100
001234432100
001234432100
001234432100
001234432100
001233332100
001233332100
001233332100
001223322100
000122221000
000122221000
000012210000
000001100000
000000000000''',
 'vertebra': '''000111111000
001233332100
012344443210
123444444321
233334433332
233224422332
122114411221
011004400110
001004400100
011114411110
122224422221
233334433332
123344443321
012333333210
001222222100
000111111000''',
 'shoulder': '''111000001111
122211122221
233322233332
334433334433
344443344443
344443344443
334433344443
233333344432
223333444432
122334444321
112334443211
011233332110
001122221100
000111111000
000011110000
000001100000''',
 'upper': '''001111111100
012222222210
123333333321
123444433321
123444433321
123444333321
123443333221
123443332221
123433322211
123433322211
123333222211
123333222211
123332222110
012332221100
001222211000
000111100000''',
 'forearm': '''000111111000
001222222100
012233322210
123344332221
123444333221
234444333221
234443333221
234433333221
234433332221
234333332211
234333322211
233333222110
123332221100
012332211000
001222110000
000111100000''',
 'joint': '''111111111111
222222222222
333333333333
444443333333
222222222222
000000000000
011111111110
122222222221
122233332221
122233332221
011111111110
000000000000
222222222222
333333333333
222222222222
111111111111''',
 'palm': '''001111111100
012222222210
123333333321
234444444432
233333333332
233222222332
233222222332
233221122332
233211122332
233211222332
233222222332
233222222332
123333333321
012222222210
001111111100
000000000000''',
 'finger': '''011111111110
122222222221
233333333332
344444444443
344444444443
233333333332
123333333321
122333333221
122333333221
122233332221
122233332221
122222222221
011111111110
000000000000
011111111110
122222222221''',
 'shell': '''000111111000
001222222100
012333332210
123444333321
123333333321
123332233321
123322223321
123322223321
123322223321
123322223321
123222223321
122222233210
122222332100
012223321000
001233210000
000122100000''',
 'root': '''000111111000
011222222110
122333333221
233444433332
233444333332
123443333321
012333333210
001222222100
000111111000
001122221100
012233332210
123344333321
123443333321
123433333221
012333332210
001222221100''',
}

# Part-to-drawing assignment; mirrored parts share one entry.
PARTS = {
 'Ossuary socket seat':'ring_socket', 'Fate ring stock':'ring_chest',
 'Fate arc':'ring_halo',
 'Humerus':'upper', 'Shoulder ossuary crown':'shoulder',
 'Broken shoulder carapace':'shell', 'Upper arm bone belly':'upper',
 'Elbow axle':'joint', 'Elbow cheek':'joint', 'Forearm paired strut':'upper',
 'Forearm radial crest':'forearm', 'Forearm ulnar crest':'forearm',
 'Broken outer bracer':'shell', 'Continuous wrist cuff':'joint',
 'Inset wrist bridge':'joint', 'Closed gauntlet palm':'palm',
 'Closed finger knuckle':'finger', 'Curled fingertip':'finger',
 'Opposed thumb plate':'finger', 'Scapular wing load bridge':'root',
 'Shared wing root':'root', 'High crescent primary':'wing',
 'Middle crescent primary':'wing_middle', 'Low crescent primary':'wing_low',
 'Trailing crescent primary':'wing_trailing',
 'Root upper hook':'wing_hook', 'Upper hooked spur':'wing_hook',
 'Middle returning spur':'wing_spur', 'Low returning spur':'wing_spur',
 'Trailing inner spur':'wing_spur',
 'Thoracic load arch':'flank', 'Clavicular bone arch':'pectoral',
 'Broken dorsal scapula':'scapula', 'Thoracic remnant':'flank',
 'Pectoral remnant':'pectoral', 'Pectoral bone ridge':'pectoral',
 'Cervical fork':'sternum', 'Lumbar connection':'vertebra',
 'Wrapping costal arch':'rib', 'Upper sternal keel':'sternum',
 'Lower ossuary bridge':'vertebra', 'Vertebral spindle':'vertebra',
 'Ventral ossuary segment':'vertebra', 'Dorsal vertebral plate':'scapula',
 'Transverse ossuary flange':'rib',
}

# Each wing tier has its own painted width and fracture placement.
WINGS = {
 'wing': (80,12,2,((20,11),(20,10),(21,9),(21,8),(22,7),(43,11),(43,10),(44,9),(45,8))),
 'wing_middle': (80,12,3,((29,11),(30,10),(30,9),(31,8),(59,11),(59,10),(60,9))),
 'wing_low': (88,12,3,((36,11),(36,10),(37,9),(38,8),(64,11),(65,10))),
 'wing_trailing': (64,10,2,((23,9),(23,8),(24,7),(46,9),(47,8))),
 'wing_hook': (28,8,2,((17,7),(18,6),(18,5))),
 'wing_spur': (24,6,1,((13,5),(14,4))),
}

BACK_DRAWINGS = {
 'scapula': '''000111111000
001233332100
012344433210
123444333321
234443332221
234433222211
234332211110
233322111100
233221112100
233211123210
233211233210
123222332100
012233321000
001233210000
000122100000
000011000000''',
 'vertebra': '''000111111000
001222222100
012233332210
123334433321
233224422332
233114411332
122004400221
011004400110
011004400110
122114411221
233224422332
233334433332
123344443321
012333333210
001222222100
000111111000''',
 'flank': '''001111111100
012233332210
123343332221
123343322211
123333222110
012222211100
001111100000
011222211000
122333221100
123443322110
123443332211
123433332211
123333322110
012332221100
001222110000
000111100000''',
}

# Cross-width pixel profiles placed at explicit lengths along each short blade.
SPUR_PALETTE=('302739','53405f','796083','9299af','c2ccd9','e1e6e8')
SPUR_PROFILES={
 'wing_hook': ((0,'00122100'),(3,'13443210'),(7,'34543210'),
               (14,'34533210'),(23,'23433210')),
 'wing_spur': ((0,'012210'),(3,'234321'),(6,'354321'),
               (12,'344321'),(20,'233210')),
}

class SurfaceSample:
    def __init__(self, atlas):
        self.atlas = atlas
        self.rows = [0]*256
        self.maps = {}

    def role(self, name):
        return next((value for prefix, value in PARTS.items() if name.startswith(prefix)), None)

    def region(self, role, material, face, width, height):
        key = f'surface:{role}:{material}:{face}'
        if key in self.maps:
            return self.maps[key]
        for y in range(112, 257-height):
            for x in range(257-width):
                mask=((1<<width)-1)<<x
                if all(self.rows[yy]&mask==0 for yy in range(y,y+height)):
                    for yy in range(y,y+height): self.rows[yy]|=mask
                    r=dict(name=key,material=material,x=x,y=y,width=width,height=height,
                           source='authored_surface_sample',surface=face)
                    self.maps[key]=r
                    self.atlas.regions[key]=r
                    self.paint(r,role,face)
                    return r
        raise ValueError('Authored surfaces exceed their reserved 256 x 144 area')

    def paint(self, r, role, face):
        if role in RINGS:
            paint_ring(self.atlas,r,role,face)
            return
        if role in PLANS:
            drawing=primary_pixels(role,face,r['width'],r['height'])
            for y in range(r['height']):
                for x in range(r['width']):
                    color=drawing[y*r['width']+x]
                    rgb=[int(color[k:k+2],16)/255 for k in (0,2,4)]
                    i=((r['y']+y)*256+r['x']+x)*4
                    self.atlas.base[i:i+4]=rgb+[1]
                    self.atlas.emission[i:i+4]=[0,0,0,0]
            return
        palette=VIOLET if r['material'] in (0,13,8) else INK
        rows=DRAWINGS.get(role,DRAWINGS['root']).splitlines()
        for y in range(r['height']):
            for x in range(r['width']):
                xx=min(11,x*12//r['width']); yy=min(15,y*16//r['height'])
                shade=int(rows[yy][xx])
                if face in ('back','lane2','lane3'):
                    shade=2 if 4<=xx<=7 and 2<yy<13 else 1
                    if yy in (0,15): shade=0
                elif face in ('inner','lane4','lane5'):
                    shade=2 if xx<7 else 1
                    if yy<2 or yy>13: shade=0
                elif face.startswith('cap') or face in ('top','bottom'):
                    edge=min(xx,11-xx,yy,15-yy)
                    shade=3 if edge<2 else 1 if edge<3 else 2
                    if face in ('cap0','bottom'): shade=max(0,shade-1)
                if face=='back' and role in ('pectoral','scapula','flank','rib','sternum','vertebra'):
                    drawing=BACK_DRAWINGS.get(role,DRAWINGS[role]).splitlines()
                    shade=int(drawing[yy][xx])
                if role in WINGS:
                    along=x; across=y
                    shade=2
                    _,_,ridge,fractures=WINGS[role]
                    if across<ridge: shade=4 if across==0 else 3
                    elif across==ridge: shade=1
                    elif across==ridge+1: shade=3
                    elif across>=r['height']-2: shade=1
                    if along<7: shade=max(0,shade-2)
                    cracks=set(fractures)
                    if (along,across) in cracks: shade=0
                    elif (along-1,across) in cracks: shade=3
                    if face!='front': shade=max(0,shade-1)
                    if across<ridge: palette=INK
                    else: palette=VIOLET
                if role in SPUR_PROFILES and face=='front':
                    profile=next(p for start,p in reversed(SPUR_PROFILES[role]) if x>=start)
                    palette=SPUR_PALETTE
                    shade=int(profile[y])
                    cracks=set(WINGS[role][3])
                    if (x,y) in cracks: shade=0
                    elif (x-1,y) in cracks: shade=3
                if role in BODY_PLANS:
                    shade=body_shade(role,face,x,y,r['width'],r['height'])
                rgb=[int(palette[shade][k:k+2],16)/255 for k in (0,2,4)]
                i=((r['y']+y)*256+r['x']+x)*4
                self.atlas.base[i:i+4]=rgb+[1]
                self.atlas.emission[i:i+4]=[0,0,0,0]

    def apply(self,obj,verts,uvs,topology):
        role=self.role(obj.name)
        if role is None: return False
        mat=obj['material_index']; mirrored='right' in obj.name
        coords=[(-v[0] if mirrored else v[0],v[1],v[2]) for v in verts]
        lo=[min(v[k] for v in coords) for k in range(3)]
        span=[max(v[k] for v in coords)-lo[k] for k in range(3)]
        layer=obj.data.uv_layers.new(name='AtlasUV')
        islands=[]
        for poly in obj.data.polygons:
            ids=list(poly.vertices)
            if topology and topology[0]=='arc':
                design=RINGS[role]
                lanes={i%4 for i in ids}
                centers=[sum((verts[k+j] for j in range(4)),verts[0]*0)/4 for k in range(0,len(verts),4)]
                distances=[0]
                for a,b in zip(centers,centers[1:]): distances.append(distances[-1]+(b-a).length)
                if len({i//4 for i in ids})==1:
                    face='cap0' if min(ids)==0 else 'cap1'
                    corners=((0,0),(1,0),(1,1),(0,1))
                    local={i:corners[i%4] for i in ids}
                    size=(design['width'],design['depth'])
                else:
                    face,low=('front',0) if lanes=={0,1} else ('back',3) if lanes=={2,3} else ('outer',1) if lanes=={1,2} else ('inner',0)
                    local={i:(distances[i//4]/distances[-1],0 if i%4==low else 1) for i in ids}
                    size=(design['length'],design['width'] if face in ('front','back') else design['depth'])
            elif topology and topology[0]=='tube':
                sides,points=topology[1:]
                def corner(i):
                    k=i%sides
                    return (sides//2-k)%sides if mirrored else k
                ring_ids={i//sides for i in ids}
                if len(ring_ids)==1:
                    face='cap0' if 0 in ring_ids else 'cap1'
                    local={i:(.5+.48*math.cos(math.tau*corner(i)/sides),
                              .5+.48*math.sin(math.tau*corner(i)/sides)) for i in ids}
                    size=(8,8)
                else:
                    ks={corner(i) for i in ids}
                    lane=next(k for k in range(sides) if ks=={k,(k+1)%sides})
                    face=f'lane{lane}'
                    distances=[0]
                    for a,b in zip(points,points[1:]): distances.append(distances[-1]+(b-a).length)
                    local={i:(0 if corner(i)==lane else 1,distances[i//sides]/max(distances[-1],.001)) for i in ids}
                    size=(6,16)
            elif topology and topology[0]=='spur_sheet':
                original=topology[1]
                lanes={original[i]%3 for i in ids}
                face='front' if lanes=={0,1} else 'back' if lanes=={1,2} else 'inner'
                local={i:(uvs[i][1],0 if original[i]%3==min(lanes) else 1) for i in ids}
                length,width,_,_=WINGS[role]
                size=(length,width if face=='front' else max(3,width//2))
                if len({original[i]//3 for i in ids})==1:
                    face='cap0' if min(original[i] for i in ids)==0 else 'cap1'
                    size=(4,4)
                    local={i:((original[i]%3)/2,0 if original[i]%3!=1 else 1) for i in ids}
            elif role in WINGS:
                lanes={i%3 for i in ids}
                face='front' if lanes=={0,1} else 'back' if lanes=={1,2} else 'inner'
                centers=[sum((verts[k+j] for j in range(3)),verts[0]*0)/3 for k in range(0,len(verts),3)]
                distances=[0]
                for a,b in zip(centers,centers[1:]): distances.append(distances[-1]+(b-a).length)
                local={i:(distances[i//3]/distances[-1],0 if i%3==min(lanes) else 1) for i in ids}
                length,width,_,_=WINGS[role]
                size=(length,width if face=='front' else max(3,width//2))
                if len({i//3 for i in ids})==1:
                    face='cap0' if min(ids)==0 else 'cap1'; size=(4,4)
                    local={i:((i%3)/2,0 if i%3!=1 else 1) for i in ids}
            else:
                # Canonical mirrored coordinates give mirrored parts the same UV direction.
                a,b,c=[coords[i] for i in ids[:3]]
                ab=[b[k]-a[k] for k in range(3)]; ac=[c[k]-a[k] for k in range(3)]
                n=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
                axis=max(range(3),key=lambda k:abs(n[k]))
                center=sum(coords[i][axis] for i in ids)/len(ids)
                high=center>lo[axis]+span[axis]/2
                face=(('inner','outer'),('top','bottom'),('front','back'))[axis][high]
                axes=[k for k in range(3) if k!=axis]
                local={i:tuple((coords[i][k]-lo[k])/max(span[k],.001) for k in axes) for i in ids}
                size=(12,16) if axis==2 else (6,16) if axis==0 else (12,6)
            r=self.region(role,mat,face,*size)
            islands.append(r['name'])
            for loop in poly.loop_indices:
                u,v=local[obj.data.loops[loop].vertex_index]
                layer.data[loop].uv=((r['x']+.5+u*(r['width']-1))/256,(r['y']+.5+v*(r['height']-1))/256)
        obj['surface_islands']=json.dumps(islands)
        return True
