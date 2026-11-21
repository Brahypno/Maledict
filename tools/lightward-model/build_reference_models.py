"""Export the custom Lightward armor and retextured Malum leggings.
Run with Blender --background --factory-startup --python this_file.
The helmet, chest, arm and boot assets are shared with the runtime layers.
The Malum reference manifest supplies comparisons and the leggings.
"""
import base64
import json
import math
from pathlib import Path
import struct
import uuid
import zipfile
import zlib
import sys
import bpy
from mathutils import Euler, Matrix, Vector

sys.dont_write_bytecode = True
sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_crystal_root_boots import PATHS as BOOT_ROOT_PATHS, ROOT_WIDTH, split_corners, GROWTH_SITES as BOOT_GROWTH_SITES, SURFACE

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT/'art/aufheben-lightward/model'
ASSETS = ROOT/'src/main/resources/assets/maledict'
REF = json.loads(Path(__file__).with_name('reference_geometry.json').read_text(encoding='utf-8'))
SLOTS = ('helmet','chestplate','leggings','boots')
BONES = {'head':(0,0,0),'body':(0,0,0),'left_arm':(5,2,0),'right_arm':(-5,2,0),
         'leggings':(0,0,0),'left_legging':(1.9,12,0),'right_legging':(-1.9,12,0),
         'left_foot':(1.9,12,0),'right_foot':(-1.9,12,0)}
JAR = next((Path.home()/'.gradle/caches/modules-2/files-2.1/curse.maven/malum-484064/6646111').rglob('*.jar'))

ORIGINAL_HELMET = REF['helmet']['nodes']
ORIGINAL_CHEST = REF['chestplate']['nodes']
ORIGINAL_BOOTS = REF['boots']['nodes']


def custom_nodes(slot,bone):
    geometry = ASSETS/f'models/armor/aufheben_lightward_{slot}.geometry.json'
    parts = json.loads(geometry.read_text(encoding='utf-8'))
    return {
        name: dict(name=name, parent=part.get('parent',bone), offset=part.get('offset',[0,0,0]),
                   rotation=part.get('rotation',[0,0,0]),
                   cubes=[dict({'inflate':0, 'mirror':False}, **cube) for cube in part['cubes']],
                   meshes=part.get('meshes',[]))
        for name,part in parts.items()
    }


REF['helmet']['nodes'] = custom_nodes('helmet','head')
CHEST_DETAILS = custom_nodes('chestplate','body')
ARM_DETAILS = custom_nodes('arms','body')
REF['chestplate']['nodes'] = CHEST_DETAILS | ARM_DETAILS
BOOT_DETAILS = custom_nodes('boots','left_foot')
REF['boots']['nodes'] = BOOT_DETAILS


def png_read(data):
    offset,raw,width,height,channels = 8,b'',0,0,0
    while offset < len(data):
        length = struct.unpack('>I',data[offset:offset+4])[0]
        kind,value = data[offset+4:offset+8],data[offset+8:offset+8+length]
        if kind == b'IEND':
            break
        if kind == b'IHDR':
            width,height,depth,color,*_ = struct.unpack('>IIBBBBB',value)
            assert depth == 8 and color in (2,6)
            channels = 4 if color == 6 else 3
        if kind == b'IDAT': raw += value
        offset += length+12
    stream,rows,previous = zlib.decompress(raw),[],bytearray(width*channels)
    for y in range(height):
        stride = width*channels
        start = y*(stride+1)
        mode,row = stream[start],bytearray(stream[start+1:start+1+stride])
        for i in range(stride):
            a = row[i-channels] if i >= channels else 0
            b,c = previous[i],previous[i-channels] if i >= channels else 0
            p = a+b-c
            distances = (abs(p-a),abs(p-b),abs(p-c))
            paeth = (a,b,c)[distances.index(min(distances))]
            row[i] = (row[i]+(0,a,b,(a+b)//2,paeth)[mode]) & 255
        rows.append([tuple(row[i:i+channels])+(() if channels == 4 else (255,)) for i in range(0,stride,channels)])
        previous = row
    return rows


def png_write(path,pixels):
    h,w = len(pixels),len(pixels[0])
    def chunk(kind,data):
        return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))
    raw = b''.join(b'\0'+bytes(c for p in row for c in p) for row in pixels)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))
                     +chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))


def paint(pixels,x,y,color):
    old = pixels[y][x]
    if old[3]: pixels[y][x] = tuple(bytes.fromhex(color))+(old[3],)


def paint_rows(pixels,u,v,rows):
    for y,row in enumerate(rows,v):
        for x,color in enumerate(row,u):
            paint(pixels,x,y,color)


HELMET_PALETTE = {
    shade: tuple(bytes.fromhex(color))+(255,)
    for shade,color in {
        'W':'EDF1FC', 'P':'CDD4EA', 'S':'A5AEC5', 'M':'747E95',
        'B':'494E61', 'A':'292C38', 'D':'161922',
        'C':'A3D8DE', 'Q':'6EAAB9', 'G':'E0C28A',
        'V':'8C51BA', 'T':'6B3692', 'U':'462362',
    }.items()
}

CHEST_DETAIL_PALETTE = HELMET_PALETTE | {
    'N': (24,25,28,255), 'F': (30,31,36,255), 'O': (16,17,20,255),
    'Z': (40,32,26,255), 'R': (67,50,33,255), 'I': (107,76,44,255),
    'H': (141,106,65,255), 'J': (181,141,88,255),
    'K': (245,225,184,255),
}

ARM_PALETTE = {
    shade: tuple(bytes.fromhex(color))+(255,)
    for shade,color in {
        'W':'EDF1FC', 'P':'CDD4EA', 'S':'A5AEC5', 'M':'747E95',
        'B':'494E61', 'A':'292C38', 'D':'161922', 'N':'1E1F24',
        'F':'24252C', 'C':'A3D8DE', 'Q':'6EAAB9', 'E':'D8C59C',
        'G':'C6A469', 'J':'A7834E', 'H':'795D38', 'I':'59452D',
    }.items()
}


def helmet_texture():
    # One set of texels per plate. Paired parts mirror the same UV islands.
    # Each first row shades up/east/north/west/south/down; face art overrides it.
    panels = {
        (0,0): ('BSBSSD', {'up': ('SPMSSSMPS','WPMSBSMPW','WPMSBSMPW',
                                  'WPMMBMMPW','WPMMBMMPW','WPMMBMMPW',
                                  'WPMSBSMPW','WPMSBSMPW','SPMSSSMPS'),
                            'south': ('PSPPPPPSP','WPMSSSMPW','WPSMMMSPW','SPMBBBMPS')}),
        (40,0): ('SBASBD', {'west': ('SPPPPPWPS','PWPPPPWPS','SWPSPPPPS','SWPSPSSSM','MSPSSSSMB')}),
        # The silver bevels turn inward toward the existing nape plate.
        (64,0): ('SAAAPD', {'south': ('SPWWPWWPS','PWSPSPSWP','SWSPPSPWS',
                                     'MSWPSPWSM','BMSWSWSMB','ABMSMSMBA')}),
        (88,0): ('PBAAPD', {'south': ('SPWWPPS','BMSPSMB')}),
        (104,0): ('PSAPBD', {'west': ('PPWPP',), 'east': ('BMMMM',)}),
        (0,18): ('WSPPBD', {'west': ('WCWGP','PQPPP','SPSSS'), 'north': ('CW','WP','PS')}),
        (16,18): ('WSBSBD', {'west': ('WWPP','SPWP','MSSP')}),
        (32,18): ('PSPPSD', {'south': ('WP','PS')}),
        (48,18): ('WSPPBD', {'north': ('PWGWWWGWP','SCPPSPPCS'), 'up': ('PWGWWWGWP','SPPPWPPPS')}),
        (76,18): ('PBBBBD', {'north': ('SMBBBMS',)}),
        (96,18): ('WPWPSD', {'north': ('PWP','WWW','PWP')}),
        (108,18): ('WPWPBD', {'north': ('W','P'), 'up': ('C',)}),
        (116,18): ('WPWPSD', {'north': ('W','W','P'), 'west': ('WP','PS','SM'), 'east': ('WP','PS','SM')}),
        (0,32): ('SBSBAD', {'north': ('SPMSMPS','PPSSSPP','SPPPPPS')}),
        (20,32): ('WPWPSD', {'north': ('WP','WP','WP','WP','PS')}),
        (30,32): ('PSPSBD', {'north': ('MSPPPSM',)}),
        (46,32): ('PSPSBD', {'north': ('S','P','P','S')}),
        (56,32): ('WPWPSD', {'north': ('WP','WW','PS')}),
        (66,32): ('ABABAD', {'north': ('V','T','U')}),
    }
    pixels = [[(0,0,0,0) for _ in range(128)] for _ in range(128)]
    paint_panels(pixels,REF['helmet']['nodes'],panels)
    return pixels


def paint_panels(pixels,nodes,panels,palette=HELMET_PALETTE):
    painted = set()
    for part in nodes.values():
        for cube in part['cubes']:
            key = tuple(cube['uv'])
            if key in painted:
                continue
            painted.add(key)
            shades, patterns = panels[key]
            faces = face_uv(cube)
            for face,shade in zip(('up','east','north','west','south','down'),shades):
                u,v,width,height = faces[face]
                rows = patterns.get(face, (shade*width,)*height)
                assert len(rows) == height and all(len(row) == width for row in rows), (key,face)
                for y,row in enumerate(rows,v):
                    for x,shade in enumerate(row,u):
                        assert pixels[y][x][3] == 0, (key,face,x,y)
                        pixels[y][x] = palette[shade]


def boot_body_texture(source):
    # Preserve the established Lightward boot art, including the silver bands,
    # dark gaps, gilt mouth and bismuth colors. New roots use separate UV space.
    metal_ramp = [(limit,tuple(bytes.fromhex(color))) for limit,color in
                  ((37,'111214'),(59,'1E1F24'),(82,'2D2F35'),(108,'737786'),
                   (144,'989DB2'),(180,'BDC4DE'),(255,'D3DAF7'))]
    pixels = [[next(color for limit,color in metal_ramp if .2126*r+.7152*g+.0722*b <= limit)+(a,)
               for r,g,b,a in row] for row in source]
    for u in (42,66):
        paint_rows(pixels,u,121,(('989DB2','A88652','D9B579','BE975F','8C6C45','989DB2'),
                               ('BDC4DE','D3DAF7','B98CE3','947DE1','D3DAF7','BDC4DE'),
                               ('989DB2','BDC4DE','758BE4','5C9AD8','BDC4DE','989DB2'),
                               ('2D2F35','1E1F24','18191C','55BCCE','18191C','2D2F35'),
                               ('1E1F24','18191C','111214','111214','18191C','1E1F24'),
                               ('737786','989DB2','BDC4DE','BDC4DE','989DB2','737786'),
                               ('2D2F35','1E1F24','18191C','18191C','1E1F24','2D2F35')))
    for u in (36,48,54,60,72,78):
        paint_rows(pixels,u,124,(('2D2F35','1E1F24','18191C','18191C','1E1F24','2D2F35'),
                               ('1E1F24','18191C','111214','111214','18191C','1E1F24')))
    wing_rows = (('989DB2','BDC4DE','B18A54','D9B477','BE985F','BDC4DE','989DB2'),
                 ('BDC4DE','2D2F35','1E1F24','9F81D5','1E1F24','2D2F35','BDC4DE'),
                 ('989DB2','1E1F24','18191C','5FAABB','18191C','1E1F24','989DB2'),
                 ('737786','989DB2','BDC4DE','BDC4DE','BDC4DE','989DB2','737786'))
    for u,mirrored in ((84,False),(92,True),(100,False),(108,True)):
        for y,row in enumerate(wing_rows,124):
            for x,color in enumerate(row[::-1] if mirrored else row):
                paint(pixels,u+x,y,color)
    return pixels


def paint_boot_inlay(pixels,cube):
    routes={face:[] for face in ('north','east','south','west')}
    for index,(_,tier,points) in enumerate(BOOT_ROOT_PATHS):
        for a,b in zip(points,points[1:]):
            for start,end in split_corners(a,b):
                face_index=math.floor((start[0]+end[0])/12)
                routes[('north','east','south','west')[face_index%4]].append(
                    ((start[0]-face_index*6,start[1]),(end[0]-face_index*6,end[1]),tier,index))
    q=cube['inflate']
    for face,segments in routes.items():
        u,v,w,h=face_uv(cube)[face]
        face_index=('north','east','south','west').index(face)
        for yy in range(h):
            y=6-q+(yy+.5)/h*(7+2*q)
            for xx in range(w):
                original=pixels[v+yy][u+xx]
                if not original[3] or max(original[:3])-min(original[:3])>60:
                    continue
                x=(xx+.5)/w*6
                tone=.2126*original[0]+.7152*original[1]+.0722*original[2]
                pressure=0
                for _,su,sy,heading in BOOT_GROWTH_SITES:
                    du=(face_index*6+x-su+12)%24-12
                    dx,dy=du*SURFACE/3,y-sy
                    across=dx*math.cos(heading)+dy*math.sin(heading)
                    along=-dx*math.sin(heading)+dy*math.cos(heading)
                    pressure=max(pressure,math.exp(-(across/.95)**2-(along/1.25)**2))
                best,target,shadow=0,None,0
                for a,b,tier,index in segments:
                    dx,dy=b[0]-a[0],b[1]-a[1]
                    squared=dx*dx+dy*dy
                    length=math.sqrt(squared)
                    t=max(0,min(1,((x-a[0])*dx+(y-a[1])*dy)/squared))
                    distance=math.hypot(x-a[0]-t*dx,y-a[1]-t*dy)
                    half=ROOT_WIDTH[tier]/2
                    # Opaque root cores carry the black/white inversion;
                    # antialias only their outermost edge.
                    coverage=max(0,min(1,(half+.17-distance)/.12))
                    # Shadow is offset below/right, consistently with the
                    # upper-left highlight, rather than tied to path direction.
                    sx,sy=x-.16,y-.22
                    st=max(0,min(1,((sx-a[0])*dx+(sy-a[1])*dy)/squared))
                    sd=math.hypot(sx-a[0]-st*dx,sy-a[1]-st*dy)
                    shadow=max(shadow,max(0,min(1,(half+.17-sd)/.15))
                               *{'trunk':.64,'branch':.52,'tip':.32}[tier])
                    if coverage<=best:
                        continue
                    signed=(dx*(y-a[1])-dy*(x-a[0]))/length
                    toward_light=signed*((-.60*-dy-.80*dx)/length)
                    # White substrate -> near-black root. Black substrate ->
                    # silver-white root. Keep the core color unequivocal.
                    dark_root=tone>=110
                    if distance<=half+.05:
                        shade='N' if dark_root else 'W'
                    elif toward_light>=0:
                        shade='S' if dark_root and pressure>.65 else 'M' if dark_root else 'P'
                    else:
                        shade='O' if dark_root else 'M'
                    best,target=coverage,CHEST_DETAIL_PALETTE[shade]
                base=original
                if shadow:
                    shade=CHEST_DETAIL_PALETTE['B' if tone>110 else 'O']
                    base=tuple(round(a*(1-shadow)+b*shadow) for a,b in zip(original[:3],shade[:3]))+(original[3],)
                if best:
                    base=tuple(round(a*(1-best)+b*best) for a,b in zip(base[:3],target[:3]))+(original[3],)
                pixels[v+yy][u+xx]=base


def boots_texture(source=None,gray=False):
    if source is None:
        with zipfile.ZipFile(JAR) as jar:
            source=png_read(jar.read('assets/malum/textures/armor/'+REF['boots']['texture']+'.png'))
    base=boot_body_texture(source)
    pixels=[[(0,0,0,0) for _ in range(128)] for _ in range(128)]
    for cube,old in zip(BOOT_DETAILS['boot_core_left']['cubes'],ORIGINAL_BOOTS['left_boot']['cubes']):
        old=dict(old,size=[int(value) for value in old['size']])
        for face,(u,v,w,h) in face_uv(cube).items():
            ou,ov,ow,oh=face_uv(old)[face]
            for y in range(h):
                for x in range(w):
                    color=base[ov+y*oh//h][ou+x*ow//w]
                    pixels[v+y][u+x]=HELMET_PALETTE['P'][:3]+(color[3],) if gray else color
        if not gray:
            paint_boot_inlay(pixels,cube)
    return pixels


def retexture(slot,source):
    if slot == 'boots':
        return boots_texture(source)
    # Sampled from the chestplate's black panels, silver planes and bevels.
    metal_ramp = [(limit,tuple(bytes.fromhex(color))) for limit,color in
                  ((37,'111214'),(59,'1E1F24'),(82,'2D2F35'),(108,'737786'),
                   (144,'989DB2'),(180,'BDC4DE'),(255,'D3DAF7'))]
    pixels = []
    for y,row in enumerate(source):
        result = []
        for x,(r,g,b,a) in enumerate(row):
            tone = .2126*r+.7152*g+.0722*b
            if slot == 'chestplate':
                color = (min(255,round(.88*tone)),min(255,round(.91*tone)),min(255,round(1.03*tone)))
            else:
                color = next(color for limit,color in metal_ramp if tone <= limit)
            result.append(color+(a,))
        pixels.append(result)
    if slot == 'chestplate':
        pixels = [[(0,0,0,0) for _ in range(128)] for _ in range(128)]
        paint_panels(pixels,CHEST_DETAILS,{
            # An open U-shaped frame surrounds a 5 x 4.5 cavity. Its wall and
            # floor extend behind the rim to the recessed star and back wall.
            (0,0): ('FNFFNO', {'north': ('FN','NF','NN','FN','NF'),
                               'south': ('FN','NF','NN','FN','NF')}),
            (34,0): ('FNNNNO', {'south': ('FNNFNF','NFNNFN','NNFNNF','FNNFNN','NNFNNF')}),
            (58,0): ('FNNNNO', {'north': ('NFNNFNNFN','FNNFNNFNN','NNFNNFNNF','FNNFNNFNN'),
                                'south': ('NFNNFNNFN','FNNFNNFNN','NNFNNFNNF','FNNFNNFNN')}),
            (0,14): ('FNFFNO', {'north': ('FNF','NFN','NNF','FNN'),
                                'west': ('FFNFNNFNF','FNNFNNFNN','NNFNNFNNF','NFNNFNNFN')}),
            (26,14): ('NOOONO', {'north': ('OOOOO','ONNNO','NNZNN','NZRZN','NNZNN')}),
            (40,14): ('FNOONO', {'west': ('FN','NN','ON','NZ','NR'),
                                 'east': ('NF','NN','NO','ZN','RN')}),
            (48,14): ('NOOONO', {'up': ('NNNNN','NZZZN','NZRZN')}),
            (66,14): ('WSPMSB', {'north': ('PWP','WWP','WPS','PSM'),
                                  'up': ('PWP','SPS'), 'west': ('SS','MS','MB','BN'),
                                  'east': ('PS','SP','SM','MB')}),
            (78,14): ('PSPMSB', {'north': ('WP','PS'), 'up': ('WP','PS'),
                                  'west': ('SM','MB'), 'east': ('SP','MS')}),
            (0,30): ('PSPMSB', {'north': ('SPWPS',), 'up': ('PSPSP',)}),
            (14,30): ('GJGJHD', {'north': ('GJH','KGH','GJJ'), 'west': ('GJ','JH','HH')}),
            (24,30): ('HJHJHO', {}),
            # The mantle and coverts shade the roots of the longer flight feathers.
            (96,0): ('WSPPMB', {'up': ('PWWPS','WWPWS','PWPPS','SSPPS'),
                                'south': ('PSPSM',),
                                'down': ('BMMMB','MSSSM','MSSSM','BMMMB')}),
            # The inner wing faces retain shade, with longitudinal bevels
            # continuing onto the narrow feather tips instead of flat black.
            (96,8): ('WSMSPM', {'south': ('SPWPS','PSWSP','SMPSP','MSSSM'),
                                'north': ('BMMMB','MSSSM','MSPSM','BMSMB'),
                                'west': ('SP','PS','SM','MB')}),
            (116,8): ('PSBMSM', {'south': ('SP','PS','SM','MB'),
                                 'north': ('BM','MS','SM','MB')}),
            (96,16): ('WMMSSM', {'south': ('WP','PW','PS','CS','SM'),
                                 'north': ('BM','MS','MS','SQ','SM')}),
            (104,16): ('WMMSSM', {'south': ('WP','WP','PW','PS','QS','SM'),
                                  'north': ('BM','MS','MS','SQ','SM','SM')}),
            (112,16): ('PMMSMM', {'south': ('S','M'), 'north': ('S','M')}),
            (96,24): ('PSMSMM', {'south': ('PS','WS','PW','PS','PS','SS','SM'),
                                 'north': ('BM','MM','MS','MS','MS','SM','SM'),
                                 'west': ('P','P','P','S','S','S','M')}),
            (106,24): ('PSMSMM', {'south': ('PS','WP','PS','PS','CS','SM'),
                                  'north': ('BM','MS','MS','MS','SM','SM'),
                                  'west': ('P','P','S','S','S','M')}),
            (116,24): ('PSMSMM', {'south': ('SP','PW','PP','PS','SM'),
                                  'north': ('BM','MS','MS','SM','SM'),
                                  'west': ('P','S','S','S','M')}),
            (96,34): ('SMMSMB', {'south': ('P','S','M'), 'north': ('S','M','B')}),
            (102,34): ('SMMSMB', {'south': ('S','S','M'), 'north': ('S','M','B')}),
            (108,34): ('SMMSMB', {'south': ('S','M','M'), 'north': ('S','M','B')}),
        },CHEST_DETAIL_PALETTE)
        paint_panels(pixels,ARM_DETAILS,{
            # Soft near-black folds leave the elbows and inner arms exposed.
            (0,56): ('NANFND', {'north': ('FNNF','NFNN','NNFN','FNNF','NFNN','NNFN'),
                                 'west': ('FNNA','NFNN','NNFN','FNNA','NFNN','NNFN'),
                                 'east': ('NNFN','NFNN','FNNF','NNFN','NFNN','FNNF')}),
            (18,56): ('NANFND', {'north': ('FNNF','NFNN','NNFN','FNNF'),
                                  'west': ('FNNF','NFNN','NNFN','FNNF')}),
            (36,56): ('NANFND', {'north': ('NFNN','FNNF','NNFN'),
                                  'west': ('NNFN','NFNN','FNNF')}),
            # A broad shoulder cap falls into two narrower front lames.
            # Bismuth catches a few cyan and pale gold texels in the silver.
            (54,56): ('PSPMSB', {'up': ('SPWP','PCWP','PWES','SPPS','MSSM'),
                                  'north': ('PWWP',),
                                  'west': ('PCPSM',), 'east': ('SPQSM',)}),
            (74,56): ('WPPSMB', {'north': ('PWWP','SPPS'), 'up': ('WWPP',)}),
            (86,56): ('PSPMSB', {'north': ('PWP','MSS')}),
            (96,56): ('PSSMBB', {'north': ('PS',)}),
            (104,56): ('PSMSBB', {'west': ('WP','CP','SS'), 'east': ('PS','QS','MM')}),
            (112,56): ('MSMSBB', {'west': ('SS',), 'east': ('MM',)}),
            # The wrist end has two columns; the upper forearm has three.
            (0,70): ('WSPMSB', {'north': ('PWP','WCP','SPS'), 'west': ('P','S','M')}),
            (10,70): ('PSPMSB', {'north': ('WP','PE','SM')}),
            (18,70): ('PSMSSB', {'north': ('P','C','S','M'),
                                  'west': ('PS','SM','MS','MB'), 'east': ('SP','PS','MS','MB')}),
            (26,70): ('GHJHID', {'north': ('J','G','J','J','H')}),
            (54,70): ('PSPMSB', {'north': ('PS',)}),
            # Closed bindings share edge shades on all four sides; the outer
            # gold passes over the silver rather than disappearing below it.
            (64,70): ('JHGHHD', {'north': ('HJGGJH',), 'south': ('HJGGJH',)}),
            (80,70): ('JHHGHD', {'west': ('HJGJH',), 'east': ('HJGJH',)}),
            (96,70): ('JHGHHD', {'north': ('HJGJH',), 'south': ('HJGJH',)}),
            (110,70): ('JHHGHD', {'west': ('HJGJH',), 'east': ('HJGJH',)}),
            # Short returns join the front shoulder lames to the side plates,
            # underneath the existing cap rather than enlarging the outline.
            (0,84): ('PSPMSB', {'west': ('WP','SS'), 'east': ('SP','MS')}),
            (8,84): ('PSPMSB', {'west': ('PS','SM'), 'east': ('SS','MM')}),
            (16,84): ('SSSMBB', {'west': ('SS',), 'east': ('MM',)}),
        },ARM_PALETTE)
    elif slot == 'leggings':
        # A violet-to-cyan fold sits between the graduated gold belt edges.
        for u in (6,34):
            for x,color in enumerate(('BDC4DE','2D2F35','18191C','BD91EB',
                                      '9E82E3','18191C','2D2F35','BDC4DE')):
                paint(pixels,u+x,69,color)
            for x,color in enumerate(('989DB2','C89F63','E6C386','64CFDA',
                                      '45ADB8','CFAD72','A68350','989DB2')):
                paint(pixels,u+x,70,color)
        # Both angled thigh guards share these mirrored three-texel faces.
        # Gilt upper corners lead into the silver bevel and dark stepped hem.
        for u in (6,24):
            paint_rows(pixels,u,123,(('A7814F','DDB97C','989DB2'),
                                      ('AD8953','D3DAF7','BDC4DE'),
                                      ('BDC4DE','2D2F35','1E1F24'),
                                      ('989DB2','18191C','111214'),
                                      ('737786','2D2F35','1E1F24')))
        thigh_panel = (('989DB2','BDC4DE','D3DAF7','D3DAF7','BDC4DE','989DB2'),
                       ('737786','2D2F35','1E1F24','1E1F24','2D2F35','737786'),
                       ('737786','1E1F24','111214','111214','1E1F24','737786'),
                       ('565965','1E1F24','18191C','18191C','1E1F24','565965'),
                       ('464852','2D2F35','1E1F24','1E1F24','2D2F35','464852'))
        for u in (0,9,18,27):
            paint_rows(pixels,u,123,thigh_panel)
    return pixels


def helmet_icon():
    palette = {'.': (0,0,0,0), **HELMET_PALETTE}
    # The darker side plates sit behind the bright, front-facing mask.
    left_rows = (
        '........',
        '........',
        '.......B',
        '....M..W',
        '....P.BP',
        '..BCPSSW',
        '..BMSGWP',
        '..BQMBAM',
        '..BMDAAW',
        '..BMPVPW',
        '..ABPTWW',
        '..AMSUPW',
        '...BPSSP',
        '...BMPWP',
        '....BMMM',
        '........',
    )
    return [[palette[shade] for shade in row+row[::-1]] for row in left_rows]


def armor_icon(source, accents):
    assert len(source) == len(source[0]) == 16
    palette = {
        '2D2349':'181821', '40365B':'25232E', '593B7C':'2D2F35',
        '744B90':'494956', '8A5EAE':'737786', 'A96EC7':'989DB2',
        'CE7CEE':'BDC4DE', 'EE8FFF':'C6CFDA', 'FFC5E4':'D3DAF7',
    }
    colors = {tuple(bytes.fromhex(key)):tuple(bytes.fromhex(value)) for key,value in palette.items()}
    pixels = [[colors[p[:3]]+(p[3],) if p[3] else p for p in row] for row in source]
    for x,y,color in accents:
        paint(pixels,x,y,color)
    # Inventory icons share the helmet's cold silver/black ramp, including
    # neutral accent texels. Keep the original silhouette and colored details.
    metal_tones={tuple(bytes.fromhex(color)):CHEST_DETAIL_PALETTE[shade][:3]
                 for color,shade in {'181821':'D','25232E':'A','2D2F35':'A','494956':'B',
                                     '737786':'M','989DB2':'S','A5AAB8':'S','BDC4DE':'P',
                                     'D3DAF7':'W','111214':'O','18191C':'N','1E1F24':'F'}.items()}
    metal_tones[tuple(bytes.fromhex('C6CFDA'))]=(221,226,243)
    pixels=[[metal_tones.get(pixel[:3],pixel[:3])+(pixel[3],) if pixel[3] else pixel
             for pixel in row] for row in pixels]
    # Match the paired silhouette while shading the right-hand metal planes.
    for row in pixels:
        for x in range(9,16):
            r,g,b,a = row[x]
            if a:
                row[x] = (r-7,g-7,b-7,a)
    return pixels


def ident(name): return str(uuid.uuid5(uuid.NAMESPACE_URL,'maledict/lightward/reference/'+name))


def face_uv(c):
    u,v = c['uv']; w,h,d = c['size']
    # ModelPart uses negative Y above the head. The physical top uses the
    # first horizontal tile (Direction.DOWN in the cube constructor).
    faces = dict(up=(u+d,v,w,d),down=(u+d+w,v,w,d),west=(u,v+d,d,h),
                north=(u+d,v+d,w,h),east=(u+d+w,v+d,d,h),south=(u+2*d+w,v+d,w,h))
    scale = c.get('uv_scale',1)
    return {face:tuple(int(value*scale) for value in values) for face,values in faces.items()}


def cube_deformation(cube):
    value = cube['inflate']
    return value if isinstance(value,list) else (value,value,value)


def arm_uv_occupancy():
    # Left image shows the active chest and old arms. Right shows the same
    # chest islands and new arms. Every allocation is checked before painting.
    chest,old_arm,new_arm = set(),set(),set()
    for nodes,target in ((CHEST_DETAILS,chest),(ORIGINAL_CHEST,old_arm),(ARM_DETAILS,new_arm)):
        for part in nodes.values():
            if target is old_arm and part['name'] in ('torso','thing'):
                continue
            for cube in part['cubes']:
                for u,v,w,h in face_uv(cube).values():
                    u,v,w,h = map(int,(u,v,w,h))
                    target.update((x,y) for y in range(v,v+h) for x in range(u,u+w))
    assert not chest & new_arm, 'New arm UV overlaps the chest or wings'
    assert all(0 <= x < 128 and 0 <= y < 128 for x,y in new_arm)
    background = (24,25,28,255)
    kept,removed,added = (96,154,139,255),(119,112,126,255),(192,156,91,255)
    pixels = [[kept if (x%128,y) in chest else
               (removed if x < 128 else added) if (x%128,y) in (old_arm if x < 128 else new_arm)
               else background for x in range(256)] for y in range(128)]
    png_write(ART/'preview-arm-uv-occupancy.png',[[p for p in row for _ in range(3)]
                                               for row in pixels for _ in range(3)])


def blockbench():
    exports = [('',SLOTS)]
    exports.extend(('_'+slot,(slot,)) for slot in SLOTS)
    for suffix,slots in exports:
        elements,groups,textures = [],[],[]
        for tid,slot in enumerate(slots):
            texture = ASSETS/f'textures/models/armor/aufheben_lightward_{slot}.png'
            textures.append(dict(name=texture.name,uuid=ident(slot+'/texture'),id=str(tid),width=128,height=128,
                                 uv_width=128,uv_height=128,source='data:image/png;base64,'+base64.b64encode(texture.read_bytes()).decode()))
            nodes = REF[slot]['nodes']
            def group(key,pivot):
                node = nodes[key]; absolute = [a+b for a,b in zip(pivot,node['offset'])]
                children = []
                for index,cube in enumerate([] if node.get('meshes') else node['cubes']):
                    x,y,z = [a+b for a,b in zip(absolute,cube['pos'])]; w,h,d = cube['size']
                    stretch = isinstance(cube['inflate'],list)
                    explicit_uv = stretch or cube.get('uv_scale',1) != 1
                    if stretch:
                        qx,qy,qz = cube_deformation(cube)
                        x,y,z,w,h,d = x-qx,y-qy,z-qz,w+2*qx,h+2*qy,d+2*qz
                    uid = ident(slot+'/'+key+'/'+str(index))
                    faces = {face:dict(uv=[u,v,u+fw,v+fh],texture=tid) for face,(u,v,fw,fh) in face_uv(cube).items()}
                    if explicit_uv and cube['mirror']:
                        # Explicit face UVs carry the right boot's mirror;
                        # mirror_uv alone applies to box-UV elements.
                        faces['east'],faces['west'] = faces['west'],faces['east']
                        for face in faces.values():
                            u,v,uu,vv = face['uv']
                            face['uv'] = [uu,v,u,vv]
                    elements.append(dict(name=node['name']+'_'+str(index),uuid=uid,type='cube',box_uv=not explicit_uv,
                                         uv_offset=cube['uv'],mirror_uv=cube['mirror'] and not explicit_uv,inflate=0 if stretch else cube['inflate'],
                                         **{'from':[x,24-y-h,z],'to':[x+w,24-y,z+d]},
                                         origin=[absolute[0],24-absolute[1],absolute[2]],faces=faces))
                    children.append(uid)
                for index,mesh in enumerate(node.get('meshes',[])):
                    uid=ident(slot+'/'+key+'/mesh/'+str(index))
                    vertices={str(i):[point[0]+absolute[0],24-point[1]-absolute[1],point[2]+absolute[2]]
                              for i,point in enumerate(mesh['vertices'])}
                    faces={str(i):dict(vertices=[str(j) for j in face['vertices'][::-1]],
                                      uv={str(j):uv for j,uv in zip(face['vertices'],face['uv'])},texture=tid)
                           for i,face in enumerate(mesh['faces'])}
                    elements.append(dict(name=key+'_surface_'+str(index),uuid=uid,type='mesh',
                                         origin=[absolute[0],24-absolute[1],absolute[2]],vertices=vertices,faces=faces))
                    children.append(uid)
                for child,definition in nodes.items():
                    if definition['parent'] == key: children.append(group(child,absolute))
                rx,ry,rz = node['rotation']
                return dict(name=node['name'],uuid=ident(slot+'/'+key),origin=[absolute[0],24-absolute[1],absolute[2]],
                            rotation=[-math.degrees(rx),math.degrees(ry),-math.degrees(rz)],children=children)
            for bone,pivot in BONES.items():
                children = [group(key,pivot) for key,node in nodes.items() if node['parent'] == bone]
                if children: groups.append(dict(name=bone,uuid=ident(slot+'/'+bone),origin=[pivot[0],24-pivot[1],pivot[2]],children=children,rotation=[0,0,0]))
        data=dict(meta=dict(format_version='4.10',model_format='free' if 'boots' in slots else 'modded_entity',box_uv=True),name='Aufheben Lightward'+suffix,
                  resolution=dict(width=128,height=128),elements=elements,outliner=groups,textures=textures)
        (ART/('aufheben_lightward'+suffix+'.bbmodel')).write_text(json.dumps(data,indent=2),encoding='utf-8')


def preview_root(bone):
    pivot = Matrix.Translation(Vector(BONES[bone]))
    if bone in ('left_legging','left_foot'):
        return pivot @ Euler((-.18,0,-.13),'XYZ').to_matrix().to_4x4()
    if bone in ('right_legging','right_foot'):
        return pivot @ Euler((.18,0,.13),'XYZ').to_matrix().to_4x4()
    return pivot


def node_matrix(nodes,key):
    node = nodes[key]
    local = Matrix.Translation(Vector(node['offset'])) @ Euler(node['rotation'],'XYZ').to_matrix().to_4x4()
    if node['parent'] in nodes: return node_matrix(nodes,node['parent']) @ local
    return preview_root(node['parent']) @ local


def material(path,name):
    image = bpy.data.images.load(str(path),check_existing=False); image.pack()
    mat = bpy.data.materials.new(name); mat.use_nodes = True
    shader = mat.node_tree.nodes.get('Principled BSDF'); shader.inputs['Roughness'].default_value = .9
    tex = mat.node_tree.nodes.new('ShaderNodeTexImage'); tex.image,tex.interpolation = image,'Closest'
    mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
    mat.node_tree.links.new(tex.outputs['Alpha'],shader.inputs['Alpha'])
    return mat


def create_mesh(slot,mat,nodes=None):
    objects = []
    if nodes is None:
        nodes = REF[slot]['nodes']
    for key,node in nodes.items():
        transform = node_matrix(nodes,key)
        for index,data in enumerate(node.get('meshes',[])):
            vertices=[]
            for point in data['vertices']:
                x,y,z=transform@Vector(point)
                vertices.append((x,z,24-y))
            mesh=bpy.data.meshes.new(key+'_surface_'+str(index))
            mesh.from_pydata(vertices,[],[face['vertices'] for face in data['faces']]); mesh.update()
            obj=bpy.data.objects.new(slot+'/'+key+'/'+str(index),mesh); bpy.context.collection.objects.link(obj)
            obj.data.materials.append(mat); layer=mesh.uv_layers.new()
            for polygon,face in zip(mesh.polygons,data['faces']):
                for loop,(u,v) in zip(polygon.loop_indices,face['uv']):
                    layer.data[loop].uv=(u/128,1-v/128)
            objects.append(obj)
        for index,c in enumerate([] if node.get('meshes') else node['cubes']):
            x,y,z = c['pos']; w,h,d = c['size']; qx,qy,qz = cube_deformation(c)
            x,y,z,w,h,d = x-qx,y-qy,z-qz,w+2*qx,h+2*qy,d+2*qz
            points = [(x,y,z),(x+w,y,z),(x+w,y+h,z),(x,y+h,z),
                      (x,y,z+d),(x+w,y,z+d),(x+w,y+h,z+d),(x,y+h,z+d)]
            vertices = []
            for point in points:
                a,b,cc = transform @ Vector(point); vertices.append((a,cc,24-b))
            # Use ModelPart's corner order and mirror both the vertices and winding.
            faces = [(1,0,3,2),(4,5,6,7),(5,4,0,1),(2,3,7,6),(5,1,2,6),(0,4,7,3)]
            if c['mirror']:
                mirror_x = (1,0,3,2,5,4,7,6)
                faces = [tuple(mirror_x[i] for i in reversed(face)) for face in faces]
            names = ['north','south','up','down','east','west']
            mesh = bpy.data.meshes.new(key+str(index)); mesh.from_pydata(vertices,[],faces); mesh.update()
            obj = bpy.data.objects.new(slot+'/'+key+'/'+str(index),mesh); bpy.context.collection.objects.link(obj)
            obj.data.materials.append(mat); layer = mesh.uv_layers.new()
            for polygon,name in zip(mesh.polygons,names):
                u,v,fw,fh = face_uv(c)[name]
                corners = [(u+fw,v),(u,v),(u,v+fh),(u+fw,v+fh)]
                if name == 'down':
                    corners = [(uu,2*v+fh-vv) for uu,vv in corners]
                if c['mirror']:
                    corners.reverse()
                for loop,(uu,vv) in zip(polygon.loop_indices,corners): layer.data[loop].uv = (uu/128,1-vv/128)
            objects.append(obj)
    return objects


def mannequin():
    face = [[(163,113,78,255)]*8 for _ in range(8)]
    for y in (0,1): face[y] = [(59,39,29,255)]*8
    for x,color in ((1,(230,225,219,255)),(2,(47,80,111,255)),(5,(47,80,111,255)),(6,(230,225,219,255))): face[4][x] = color
    face[6][3] = face[6][4] = (99,59,43,255)
    path = ART/'mannequin-face.png'; png_write(path,face)
    mat = material(path,'Preview player skin (not exported)')
    # Explicit front face: its UV maps the visible eyes to Java y=-3.5.
    vertices = [(-4,-4,32),(-4,-4,24),(4,-4,24),(4,-4,32)]
    mesh = bpy.data.meshes.new('preview face'); mesh.from_pydata(vertices,[],[(0,1,2,3)]); mesh.update()
    obj = bpy.data.objects.new('Preview player face (not exported)',mesh); bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat); layer = mesh.uv_layers.new()
    for loop,uv in zip(mesh.polygons[0].loop_indices,((0,1),(0,0),(1,0),(1,1))): layer.data[loop].uv = uv
    cloth = bpy.data.materials.new('Preview trousers (not exported)')
    cloth.diffuse_color = (.055,.047,.068,1)
    java_to_blender = Matrix(((1,0,0,0),(0,0,1,0),(0,-1,0,24),(0,0,0,1)))
    for bone in ('left_foot','right_foot'):
        bpy.ops.mesh.primitive_cube_add(size=1)
        leg = bpy.context.object
        leg.name = 'Preview player leg (not exported)'
        leg.matrix_world = (java_to_blender @ preview_root(bone)
                            @ Matrix.Translation(Vector((0,6,0))) @ Matrix.Diagonal((4,12,4,1)))
        leg.data.materials.append(cloth)


def eye_clearance():
    nodes = REF['helmet']['nodes']
    for x in (-2.5,-1.5,1.5,2.5):
        eye = Vector((x,-3.5,-4))
        for key,node in nodes.items():
            inverse = node_matrix(nodes,key).inverted()
            # Sample the short viewing ray from the skin to the front of the armor.
            for step in range(21):
                point = inverse @ (eye - Vector((0,0,step*.1)))
                for cube in node['cubes']:
                    assert not all(start < coordinate < start+length for coordinate,start,length
                                   in zip(point,cube['pos'],cube['size'])), (key, 'Eye is occluded')


def preview():
    bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)
    mats,objects,original = {},{},{}
    for slot in SLOTS:
        mats[slot] = material(ASSETS/f'textures/models/armor/aufheben_lightward_{slot}.png',slot+' Lightward')
        original[slot] = material(ART/(slot+'-original.png'),slot+' original Malum')
        objects[slot] = create_mesh(slot,mats[slot])
    mannequin(); scene = bpy.context.scene
    bpy.context.preferences.filepaths.save_version = 0; bpy.context.preferences.filepaths.file_preview_type = 'NONE'
    scene.render.engine = 'BLENDER_EEVEE'
    scene.render.resolution_x,scene.render.resolution_y,scene.render.resolution_percentage = 800,900,100
    scene.world.color = (.13,.13,.13); scene.render.image_settings.file_format = 'PNG'
    scene.view_settings.view_transform,scene.view_settings.look = 'Standard','None'
    lights = (((-15,-20,45),4500,25),((16,4,35),2200,25),((0,-22,9),1150,5))
    for position,power,target_height in lights:
        bpy.ops.object.light_add(type='AREA',location=position)
        lamp = bpy.context.object; lamp.data.energy,lamp.data.size = power,12
        lamp.rotation_euler = (Vector((0,0,target_height))-lamp.location).to_track_quat('-Z','Y').to_euler()
    bpy.ops.object.camera_add(); camera = bpy.context.object
    camera.data.type,camera.data.ortho_scale = 'ORTHO',41; scene.camera = camera
    for name,position in (('front',(0,-60,20)),('three-quarter',(38,-60,25)),('back',(28,60,25))):
        camera.location = position; camera.rotation_euler = (Vector((0,0,18))-camera.location).to_track_quat('-Z','Y').to_euler()
        scene.render.filepath = str(ART/('preview-'+name+'.png')); bpy.ops.render.render(write_still=True)
        if name == 'front':
            baseline = ROOT/'build/lightward-model-check/chest-before-arms.png'
            if baseline.exists():
                for obj in objects['chestplate']: obj.hide_render = True
                old_arms = {k:n for k,n in ORIGINAL_CHEST.items() if k not in ('torso','thing')}
                before = create_mesh('chestplate',material(baseline,'Previous Lightward arms'),CHEST_DETAILS | old_arms)
                scene.render.filepath = str(ART/'preview-arms-before.png')
                bpy.ops.render.render(write_still=True)
                for obj in before: bpy.data.objects.remove(obj,do_unlink=True)
                for obj in objects['chestplate']: obj.hide_render = False
            for slot in ('helmet','chestplate','boots'):
                for obj in objects[slot]: obj.hide_render = True
            comparison_helmet = create_mesh('helmet',original['helmet'],ORIGINAL_HELMET)
            comparison_chest = create_mesh('chestplate',original['chestplate'],ORIGINAL_CHEST)
            comparison_boots = create_mesh('boots',original['boots'],ORIGINAL_BOOTS)
            for obj in objects['leggings']: obj.data.materials[0] = original['leggings']
            scene.render.filepath = str(ART/'preview-original-same-camera.png'); bpy.ops.render.render(write_still=True)
            for obj in comparison_helmet + comparison_chest + comparison_boots: bpy.data.objects.remove(obj,do_unlink=True)
            for slot in ('helmet','chestplate','boots'):
                for obj in objects[slot]: obj.hide_render = False
            for slot in SLOTS[1:]:
                for obj in objects[slot]: obj.data.materials[0] = mats[slot]
    bpy.ops.wm.save_as_mainfile(filepath=str(ART/'aufheben_lightward.blend'))
    for slot in SLOTS[1:]:
        for obj in objects[slot]: obj.hide_render = True
    camera.data.ortho_scale = 15
    scene.render.resolution_x = scene.render.resolution_y = 560
    camera.location = (20,-52,47)
    camera.rotation_euler = (Vector((0,0,29.2))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-helmet.png')
    bpy.ops.render.render(write_still=True)
    camera.location = (0,0,75)
    camera.rotation_euler = (0,0,0)
    scene.render.filepath = str(ART/'preview-helmet-top.png')
    bpy.ops.render.render(write_still=True)

    # Fill the rear close-up so the back plate can be inspected under direct light.
    bpy.ops.object.light_add(type='AREA',location=(-10,20,38))
    fill = bpy.context.object
    fill.data.energy,fill.data.size = 3500,12
    fill.rotation_euler = (Vector((0,0,29.2))-fill.location).to_track_quat('-Z','Y').to_euler()
    camera.location = (0,52,32)
    camera.rotation_euler = (Vector((0,0,29.2))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-helmet-back.png')
    bpy.ops.render.render(write_still=True)
    for obj in objects['chestplate']: obj.hide_render = False
    fill.rotation_euler = (Vector((0,0,23))-fill.location).to_track_quat('-Z','Y').to_euler()
    camera.data.ortho_scale = 28
    for name,position in (('fold',(21,58,33)),('wing-side',(57,14,29)),
                          ('wing-underside',(44,-24,31))):
        camera.location = position
        camera.rotation_euler = (Vector((0,1,23))-camera.location).to_track_quat('-Z','Y').to_euler()
        scene.render.filepath = str(ART/f'preview-chestplate-{name}.png')
        bpy.ops.render.render(write_still=True)
    fill.hide_render = True
    camera.data.ortho_scale = 25
    camera.location = (16,-56,28)
    camera.rotation_euler = (Vector((0,0,24))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-chestplate-shrine.png')
    bpy.ops.render.render(write_still=True)
    camera.location = (42,-48,29)
    camera.rotation_euler = (Vector((0,0,23.5))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-chestplate-shrine-depth.png')
    bpy.ops.render.render(write_still=True)
    for obj in objects['helmet']: obj.hide_render = True
    bpy.data.objects['Preview player face (not exported)'].hide_render = True
    camera.data.ortho_scale = 25
    scene.render.resolution_x,scene.render.resolution_y = 720,560
    camera.location = (0,-60,23)
    camera.rotation_euler = (Vector((0,0,20.5))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-chestplate-open-front.png')
    bpy.ops.render.render(write_still=True)
    for slot in SLOTS[2:]:
        for obj in objects[slot]: obj.hide_render = True
    for obj in bpy.context.scene.objects:
        if obj.name.startswith('Preview player'): obj.hide_render = True
    camera.data.ortho_scale = 26
    for name,position in (('front',(0,-60,22)),('side',(55,-12,26)),('back',(20,55,25))):
        camera.location = position
        camera.rotation_euler = (Vector((0,0,19.5))-camera.location).to_track_quat('-Z','Y').to_euler()
        fill.hide_render = name != 'back'
        scene.render.filepath = str(ART/f'preview-arms-{name}.png')
        bpy.ops.render.render(write_still=True)
    fill.hide_render = True
    for obj in objects['chestplate']:
        key = obj.name.split('/')[1]
        obj.hide_render = key not in ARM_DETAILS or ARM_DETAILS[key]['parent'] != 'left_arm'
    camera.data.ortho_scale = 20
    scene.render.resolution_x = scene.render.resolution_y = 720
    camera.location = (22,-50,27)
    camera.rotation_euler = (Vector((6,0,20))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-arm-detail.png')
    bpy.ops.render.render(write_still=True)
    camera.location = (32,40,27)
    camera.rotation_euler = (Vector((6,0,20))-camera.location).to_track_quat('-Z','Y').to_euler()
    fill.hide_render = False
    scene.render.filepath = str(ART/'preview-arm-back-detail.png')
    bpy.ops.render.render(write_still=True)
    fill.hide_render = True

    # Rigid arm rotations use the same arm pivots as the runtime overlay.
    java_space = Matrix(((1,0,0,0),(0,0,1,0),(0,-1,0,24),(0,0,0,1)))
    for obj in objects['chestplate']:
        obj.hide_render = False
        key = obj.name.split('/')[1]
        if key in ARM_DETAILS:
            bone = ARM_DETAILS[key]['parent']
            angle = .65 if bone == 'left_arm' else -.65
            pivot = Matrix.Translation(Vector(BONES[bone]))
            obj.matrix_world = java_space @ pivot @ Euler((angle,0,0),'XYZ').to_matrix().to_4x4() @ pivot.inverted() @ java_space.inverted()
    camera.data.ortho_scale = 28
    camera.location = (25,-60,29)
    camera.rotation_euler = (Vector((0,0,20))-camera.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath = str(ART/'preview-arms-walk.png')
    bpy.ops.render.render(write_still=True)


def boot_previews(gray_only=False):
    """Inspect the same runtime geometry on all four sides, without paint camouflage."""
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_EEVEE'
    scene.render.resolution_x = scene.render.resolution_y = 560
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = 'PNG'
    scene.view_settings.view_transform,scene.view_settings.look = 'Standard','None'
    scene.world.color = (.13,.13,.13)
    gray_path = ART/'boots-gray-atlas.png'
    png_write(gray_path,boots_texture(gray=True))
    gray = material(gray_path,'Crystal-root grey model')
    left = {k:p for k,p in BOOT_DETAILS.items() if p['parent'] == 'left_foot'}
    objects = create_mesh('boots',gray,left)
    java_space = Matrix(((1,0,0,0),(0,0,1,0),(0,-1,0,24),(0,0,0,1)))
    for obj in objects:
        obj.matrix_world = java_space @ preview_root('left_foot').inverted() @ java_space.inverted()
    for position,power in (((-16,-22,36),4000),((17,20,30),3500),((-15,12,19),1800)):
        bpy.ops.object.light_add(type='AREA',location=position)
        lamp = bpy.context.object
        lamp.data.energy,lamp.data.size = power,10
        lamp.rotation_euler = (Vector((0,0,14.5))-lamp.location).to_track_quat('-Z','Y').to_euler()
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.type,camera.data.ortho_scale = 'ORTHO',11.8
    scene.camera = camera
    for kind in (('gray',) if gray_only else ('gray','color')):
        if kind == 'color':
            colored = material(ASSETS/'textures/models/armor/aufheben_lightward_boots.png','Crystal-root matched silver')
            for obj in objects:
                obj.data.materials[0] = colored
        frames = []
        for view,position in (('front',(16,-38,22)),('outer',(38,10,21)),
                              ('back',(-16,38,21)),('inner',(-38,-10,21))):
            camera.location = position
            camera.rotation_euler = (Vector((0,0,14.5))-camera.location).to_track_quat('-Z','Y').to_euler()
            path = ART/f'preview-boots-{kind}-{view}.png'
            scene.render.filepath = str(path)
            bpy.ops.render.render(write_still=True)
            frames.append(png_read(path.read_bytes()))
        png_write(ART/f'preview-boots-{kind}-four-views.png',
                  [a+b for a,b in zip(frames[0],frames[1])]+[a+b for a,b in zip(frames[2],frames[3])])


def write_icon_previews(icons):
    for slot,icon in icons.items():
        png_write(ART/f'preview-{slot}-icon.png',[[pixel for pixel in row for _ in range(16)]
                                                 for row in icon for _ in range(16)])
    background = (105,109,121,255)
    png_write(ART/'preview-item-set.png',[
        [pixel if pixel[3] else background for slot in SLOTS for pixel in icons[slot][y] for _ in range(8)]
        for y in range(16) for _ in range(8)])


ART.mkdir(parents=True,exist_ok=True)
eye_clearance()
arm_uv_occupancy()
with zipfile.ZipFile(JAR) as jar:
    for slot in SLOTS:
        raw = jar.read('assets/malum/textures/armor/'+REF[slot]['texture']+'.png')
        (ART/(slot+'-original.png')).write_bytes(raw); source = png_read(raw)
        assert len(source) == len(source[0]) == 128
        texture = helmet_texture() if slot == 'helmet' else retexture(slot,source)
        png_write(ASSETS/f'textures/models/armor/aufheben_lightward_{slot}.png',texture)
    thigh_panel = ((4,7,'2D2F35'),(4,8,'18191C'),(4,9,'1E1F24'),(4,10,'18191C'),(5,10,'2D2F35'),
                   (4,11,'111214'),(5,11,'18191C'),(4,12,'18191C'),(5,12,'25232E'))
    leggings_accents = ((7,3,'B88CE4'),(8,3,'A181D8'),(6,4,'62C6CF'),(9,4,'57B1BF'),
                        (5,4,'D8B57A'),(10,4,'C29C65'),(4,5,'B9925A'),(11,5,'A27A4A'))
    leggings_accents += thigh_panel + tuple((15-x,y,color) for x,y,color in thigh_panel)
    boot_bevel = ((4,5,'D8B57A'),(5,5,'AD8956'),(4,6,'AA84D6'),(5,6,'57B7C8'),
                  (4,7,'18191C'),(5,7,'1E1F24'),(4,8,'1E1F24'),(5,8,'2D2F35'),
                  (4,9,'C6CFDA'),(3,10,'A5AAB8'),(2,11,'989DB2'))
    boot_accents = boot_bevel + tuple((15-x,y,color) for x,y,color in boot_bevel)
    chest_icon = ASSETS/'textures/item/aufheben_lightward_chestplate.png'
    icons = dict(helmet=helmet_icon(), chestplate=png_read(chest_icon.read_bytes()))
    for slot,accents in (('leggings',leggings_accents),('boots',boot_accents)):
        source = png_read(jar.read(f'assets/malum/textures/item/soul_stained_steel_{slot}.png'))
        icons[slot] = armor_icon(source,accents)
    for slot,icon in icons.items():
        if slot != 'chestplate':
            png_write(ASSETS/f'textures/item/aufheben_lightward_{slot}.png',icon)
    write_icon_previews(icons)
blockbench()
(ART/'geometry.json').write_text(json.dumps(REF,indent=2),encoding='utf-8')
preview()
boot_previews()
print('Exported Lightward armor, fixed wings, articulated arms and crystal-root boots. Four eye rays clear.')
