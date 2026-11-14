"""Export the custom Lightward helmet and retextured Malum armor.
Run with Blender --background --factory-startup --python this_file.
The helmet geometry asset is shared with the runtime layer. The Malum
reference manifest supplies the other pieces and the same-camera comparison.
"""
import base64
import json
import math
from pathlib import Path
import struct
import uuid
import zipfile
import zlib
import bpy
from mathutils import Euler, Matrix, Vector

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
HELMET = json.loads((ASSETS/'models/armor/aufheben_lightward_helmet.geometry.json').read_text(encoding='utf-8'))
REF['helmet']['nodes'] = {
    name: dict(name=name, parent='head', offset=part.get('offset', [0,0,0]),
               rotation=part.get('rotation', [0,0,0]),
               cubes=[dict({'inflate':0, 'mirror':False}, **cube) for cube in part['cubes']])
    for name,part in HELMET.items()
}


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


def gild(pixels,x,y,position):
    old = pixels[y][x]
    if old[3]:
        light = (.2126*old[0]+.7152*old[1]+.0722*old[2])/200
        level = max(.12,min(1,.18+.65*light+.17*(1-position)))
        shadow,highlight = (100,70,33),(239,211,151)
        pixels[y][x] = tuple(round(a+(b-a)*level) for a,b in zip(shadow,highlight))+(old[3],)


HELMET_PALETTE = {
    shade: tuple(bytes.fromhex(color))+(255,)
    for shade,color in {
        'W':'EDF1FC', 'P':'CDD4EA', 'S':'A5AEC5', 'M':'747E95',
        'B':'494E61', 'A':'292C38', 'D':'161922',
        'C':'A3D8DE', 'Q':'6EAAB9', 'G':'E0C28A',
        'V':'8C51BA', 'T':'6B3692', 'U':'462362',
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
    painted = set()
    for part in REF['helmet']['nodes'].values():
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
                        pixels[y][x] = HELMET_PALETTE[shade]
    return pixels


def retexture(slot,source):
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
        # The existing inclined upper breastplate, including its thin overlay.
        for u in (14,56):
            for row,colors in enumerate((('F0DFB2','DFBC78'),('DFBC78','BF985B'),('BF985B','967341'))):
                for column,color in enumerate(colors): paint(pixels,u+column,66+row,color)
        for u,color in ((8,'997DBB'),(13,'796191')): paint(pixels,u,76,color)
        for u,color in ((8,'66A7B4'),(13,'508592')): paint(pixels,u,78,color)
        # One-texel gilt edge on the marked shoulder-pad and forearm surfaces.
        for u in (8,36):
            for x in range(6): gild(pixels,u+x,110,x/5)
            gild(pixels,u+5,109,1)
        for u in (6,28):
            for x in range(5): gild(pixels,u+x,119,x/4)
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
    elif slot == 'boots':
        # A thin gilt cuff frames the short iridescent run down the instep.
        for u in (42,66):
            paint_rows(pixels,u,121,(('989DB2','A88652','D9B579','BE975F','8C6C45','989DB2'),
                                      ('BDC4DE','D3DAF7','B98CE3','947DE1','D3DAF7','BDC4DE'),
                                      ('989DB2','BDC4DE','758BE4','5C9AD8','BDC4DE','989DB2'),
                                      ('2D2F35','1E1F24','18191C','55BCCE','18191C','2D2F35'),
                                      ('1E1F24','18191C','111214','111214','18191C','1E1F24'),
                                      ('737786','989DB2','BDC4DE','BDC4DE','989DB2','737786'),
                                      ('2D2F35','1E1F24','18191C','18191C','1E1F24','2D2F35')))
        # The dark instep band continues around the sides and heel.
        for u in (36,48,54,60,72,78):
            paint_rows(pixels,u,124,(('2D2F35','1E1F24','18191C','18191C','1E1F24','2D2F35'),
                                      ('1E1F24','18191C','111214','111214','18191C','1E1F24')))
        # Continue the gilt bevel and color change on the visible outer plates.
        wing_rows = (('989DB2','BDC4DE','B18A54','D9B477','BE985F','BDC4DE','989DB2'),
                     ('BDC4DE','2D2F35','1E1F24','9F81D5','1E1F24','2D2F35','BDC4DE'),
                     ('989DB2','1E1F24','18191C','5FAABB','18191C','1E1F24','989DB2'),
                     ('737786','989DB2','BDC4DE','BDC4DE','BDC4DE','989DB2','737786'))
        for u,mirrored in ((84,False),(92,True),(100,False),(108,True)):
            for y,row in enumerate(wing_rows,124):
                for x,color in enumerate(row[::-1] if mirrored else row):
                    paint(pixels,u+x,y,color)
    return pixels


def paint_icon_metal(source,rows,start_x,start_y):
    # The existing icon supplies the silhouette and colored inlays. Dots leave
    # those texels in place while the matrix redraws the neutral metal.
    metal = {
        'O':'25232E', 'A':'18191C', 'B':'2D2F35', 'S':'989DB2',
        'P':'BDC4DE', 'W':'D3DAF7',
    }
    for y,row in enumerate(rows,start_y):
        for x,shade in enumerate(row,start_x):
            if shade != '.':
                paint(source,x,y,metal[shade])
    return source


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


def chestplate_icon(source):
    # Mirrored silver shoulders and black insets frame the original gold core.
    left_rows = ('.OOOO...',
                 '.OSPBOO.',
                 'OSWWPSOO',
                 'OPPWWPPS',
                 'OBSPWPBA',
                 'OABBW.A.',
                 'OAABSPA.',
                 'OAABA.A.',
                 '.OOOSBAA',
                 '...OSBBB',
                 '...OPWPP',
                 '...OBAAA',
                 '....OBBB',
                 '.....OOO')
    return paint_icon_metal(source,(row+row[::-1] for row in left_rows),0,1)


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
    return dict(up=(u+d,v,w,d),down=(u+d+w,v,w,d),west=(u,v+d,d,h),
                north=(u+d,v+d,w,h),east=(u+d+w,v+d,d,h),south=(u+2*d+w,v+d,w,h))


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
                for index,cube in enumerate(node['cubes']):
                    x,y,z = [a+b for a,b in zip(absolute,cube['pos'])]; w,h,d = cube['size']
                    uid = ident(slot+'/'+key+'/'+str(index))
                    faces = {face:dict(uv=[u,v,u+fw,v+fh],texture=tid) for face,(u,v,fw,fh) in face_uv(cube).items()}
                    elements.append(dict(name=node['name']+'_'+str(index),uuid=uid,type='cube',box_uv=True,
                                         uv_offset=cube['uv'],mirror_uv=cube['mirror'],inflate=cube['inflate'],
                                         **{'from':[x,24-y-h,z],'to':[x+w,24-y,z+d]},
                                         origin=[absolute[0],24-absolute[1],absolute[2]],faces=faces))
                    children.append(uid)
                for child,definition in nodes.items():
                    if definition['parent'] == key: children.append(group(child,absolute))
                rx,ry,rz = node['rotation']
                return dict(name=node['name'],uuid=ident(slot+'/'+key),origin=[absolute[0],24-absolute[1],absolute[2]],
                            rotation=[-math.degrees(rx),math.degrees(ry),-math.degrees(rz)],children=children)
            for bone,pivot in BONES.items():
                children = [group(key,pivot) for key,node in nodes.items() if node['parent'] == bone]
                if children: groups.append(dict(name=bone,uuid=ident(slot+'/'+bone),origin=[pivot[0],24-pivot[1],pivot[2]],children=children,rotation=[0,0,0]))
        data=dict(meta=dict(format_version='4.10',model_format='modded_entity',box_uv=True),name='Aufheben Lightward'+suffix,
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
        for index,c in enumerate(node['cubes']):
            x,y,z = c['pos']; w,h,d = c['size']; q = c['inflate']
            x,y,z,w,h,d = x-q,y-q,z-q,w+2*q,h+2*q,d+2*q
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
            for obj in objects['helmet']: obj.hide_render = True
            comparison_helmet = create_mesh('helmet',original['helmet'],ORIGINAL_HELMET)
            for slot in SLOTS[1:]:
                for obj in objects[slot]: obj.data.materials[0] = original[slot]
            scene.render.filepath = str(ART/'preview-original-same-camera.png'); bpy.ops.render.render(write_still=True)
            for obj in comparison_helmet: bpy.data.objects.remove(obj,do_unlink=True)
            for obj in objects['helmet']: obj.hide_render = False
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
    icons = dict(helmet=helmet_icon(), chestplate=chestplate_icon(png_read(chest_icon.read_bytes())))
    for slot,accents in (('leggings',leggings_accents),('boots',boot_accents)):
        source = png_read(jar.read(f'assets/malum/textures/item/soul_stained_steel_{slot}.png'))
        icons[slot] = armor_icon(source,accents)
    for slot,icon in icons.items():
        png_write(ASSETS/f'textures/item/aufheben_lightward_{slot}.png',icon)
    write_icon_previews(icons)
blockbench()
(ART/'geometry.json').write_text(json.dumps(REF,indent=2),encoding='utf-8')
preview()
print('Exported the custom Lightward helmet and the three Malum-derived armor pieces. Four eye rays clear.')
