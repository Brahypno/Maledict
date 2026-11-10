"""Retexture the original Malum armor; retain geometry, UV, alpha and pivots.
Run with Blender --background --factory-startup --python this_file.
Runtime calls the original public layer factories; the reference manifest is
only for editable Blockbench exports and same-camera offline comparisons.
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

# Gray cheeks extend under the pupils; only their innermost front texel is purple.
REF['helmet']['nodes']['tears'] = dict(name='tears',parent='head',offset=[0,0,0],rotation=[0,0,0],cubes=[
    dict(pos=[-3,-3,-5.25],size=[2,3,1],uv=[84,34],inflate=0,mirror=False),
    dict(pos=[1,-3,-5.25],size=[2,3,1],uv=[84,34],inflate=0,mirror=True)])
REAR_GUARD = dict(pos=[-4.5,-8.5,4.5],size=[9,9,1],uv=[92,34],inflate=0,mirror=False)
REAR_LAME = dict(pos=[-1,-2.5,-.5],size=[2,5,1],uv=[92,46],inflate=0,mirror=False)
NAPE_LAME = dict(pos=[-3.5,-2.25,5.25],size=[7,2,1],uv=[100,46],inflate=0,mirror=False)
REF['helmet']['nodes']['rear_guard'] = dict(name='rear_guard',parent='head',offset=[0,0,0],
                                          rotation=[0,0,0],cubes=[REAR_GUARD,NAPE_LAME])
for side,sign in (('left',-1),('right',1)):
    name = f'{side}_rear_lame'
    REF['helmet']['nodes'][name] = dict(name=name,parent='head',offset=[sign*2.65,-4.6,5.5],
                                       rotation=[0,0,sign*.22],cubes=[dict(REAR_LAME,mirror=sign > 0)])


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


def tint(pixels,x,y,color,strength=.8):
    old = pixels[y][x]
    if old[3]:
        hue = tuple(bytes.fromhex(color))
        light = .55+.55*(.2126*old[0]+.7152*old[1]+.0722*old[2])/200
        pixels[y][x] = tuple(round(min(255,c*light*strength)) for c in hue)+(old[3],)


def gild(pixels,x,y,position):
    old = pixels[y][x]
    if old[3]:
        light = (.2126*old[0]+.7152*old[1]+.0722*old[2])/200
        level = max(.12,min(1,.18+.65*light+.17*(1-position)))
        shadow,highlight = (100,70,33),(239,211,151)
        pixels[y][x] = tuple(round(a+(b-a)*level) for a,b in zip(shadow,highlight))+(old[3],)


def paint_rear_armor(pixels,source):
    palette = {'W':'D3DAF7','P':'BDC4DE','S':'989DB2','M':'737786',
               'B':'2D2F35','A':'1E1F24','D':'111214'}
    guard = ('MSPWWWPSM',
             'SPPSSSPPS',
             'SPPBABPPS',
             'SPBAAABPS',
             'SMBADABMS',
             'SMPBABPMS',
             'MSPSBSPSM',
             'BSPSSSPSB',
             'BMSSSSSMB')
    lame = ('WP','PS','SB','SP','MS')
    nape = ('MSPWPSM','BMSPSMB')
    edges = {'up':'P','down':'B','east':'M','west':'M','north':'A'}
    for cube,back in ((REAR_GUARD,guard),(REAR_LAME,lame),(NAPE_LAME,nape)):
        for face,(u,v,width,height) in face_uv(cube).items():
            for y in range(height):
                for x in range(width):
                    assert source[v+y][u+x][3] == 0, 'Rear armor UV overlaps the original helmet'
                    shade = back[y][x] if face == 'south' else edges[face]
                    pixels[v+y][u+x] = tuple(bytes.fromhex(palette[shade]))+(255,)


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
            iris = slot == 'helmet' and 20 <= y < 24 and (9 <= x < 12 or 33 <= x < 36)
            if slot == 'chestplate' or iris:
                color = (min(255,round(.88*tone)),min(255,round(.91*tone)),min(255,round(1.03*tone)))
            else:
                color = next(color for limit,color in metal_ramp if tone <= limit)
            result.append(color+(a,))
        pixels.append(result)
    if slot == 'helmet':
        # Mirrored brow and temple plates use broad black insets and silver rims.
        for u in (6,36):
            paint_rows(pixels,u,6,(('BDC4DE','D3DAF7','BDC4DE'),
                                    ('989DB2','D3DAF7','BDC4DE'),
                                    ('2D2F35','1E1F24','1E1F24'),
                                    ('18191C','18191C','292A30')))
        temple = (('BDC4DE','D3DAF7','D3DAF7','D3DAF7','D3DAF7','D3DAF7','BDC4DE'),
                  ('989DB2','BDC4DE','BDC4DE','BDC4DE','BDC4DE','BDC4DE','989DB2'),
                  ('989DB2','2D2F35','1E1F24','1E1F24','1E1F24','2D2F35','989DB2'),
                  ('989DB2','1E1F24','18191C','111214','18191C','1E1F24','989DB2'),
                  ('737786','1E1F24','18191C','111214','18191C','1E1F24','737786'),
                  ('737786','2D2F35','1E1F24','1E1F24','1E1F24','2D2F35','737786'),
                  ('565965','737786','737786','737786','737786','737786','565965'))
        for u in (0,9,18,27):
            paint_rows(pixels,u,32,temple)
        for u in (9,33):
            for y,color in zip(range(20,24),('C284E8','888DE9','4BBFD7','66CBB0')):
                for x in range(u,u+3): tint(pixels,x,y,color,1.05 if x == u+1 else .88)
        for u in (7,37): paint(pixels,u,6,'DFBC78')
        assert all(source[y][x][3] == 0 for y in range(34,38) for x in range(84,90))
        for x in range(85,89): pixels[34][x] = (211,218,247,255)
        for y,silver in zip(range(35,38),((189,196,222),(152,157,178),(115,119,134))):
            for x in range(84,90): pixels[y][x] = silver+(255,)
        for y,color in zip(range(35,38),('8308E4','6006AB','2B0178')):
            pixels[y][86] = tuple(bytes.fromhex(color))+(255,)
        pixels[34][86] = (131,8,228,255)
        paint_rear_armor(pixels,source)
    elif slot == 'chestplate':
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
    helmet_tiles = ((84,34,6,4),(92,34,20,10),(92,46,6,6),(100,46,16,3)) if slot == 'helmet' else ()
    assert all(p[3] == source[y][x][3] for y,row in enumerate(pixels) for x,p in enumerate(row)
               if not any(u <= x < u+width and v <= y < v+height for u,v,width,height in helmet_tiles)), 'Original source alpha changed'
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


def helmet_icon(source):
    left_rows = ('..OOO.',
                 '.OWW.O',
                 'OSWWBO',
                 '.O.WPP',
                 '.OP.S.',
                 '.OBAA.',
                 '.OPSA.',
                 'OS..A.',
                 'OPS.AB',
                 'OSP.AA',
                 '.OBO..')
    paint_icon_metal(source,left_rows,2,2)
    # Mirror across the gap between columns seven and eight, including the crown
    # silhouette and the existing gold, iridescent and purple texels.
    return [row[:8]+list(reversed(row[:8])) for row in source]


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
    return dict(up=(u+d,v,w,d),down=(u+d+w,v,w,d),east=(u,v+d,d,h),
                north=(u+d,v+d,w,h),west=(u+d+w,v+d,d,h),south=(u+2*d+w,v+d,w,h))


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


def create_mesh(slot,mat):
    objects,nodes = [],REF[slot]['nodes']
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
            faces = [(0,3,2,1),(5,6,7,4),(0,1,5,4),(3,7,6,2),(1,2,6,5),(4,7,3,0)]
            names = ['north','south','up','down','west','east']
            mesh = bpy.data.meshes.new(key+str(index)); mesh.from_pydata(vertices,[],faces); mesh.update()
            obj = bpy.data.objects.new(slot+'/'+key+'/'+str(index),mesh); bpy.context.collection.objects.link(obj)
            obj.data.materials.append(mat); layer = mesh.uv_layers.new()
            for polygon,name in zip(mesh.polygons,names):
                u,v,fw,fh = face_uv(c)[name]; corners = [(u,v),(u,v+fh),(u+fw,v+fh),(u+fw,v)]
                if c['mirror']: corners = [(2*u+fw-uu,vv) for uu,vv in corners]
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
    for ex in (-2.5,-1.5,1.5,2.5):
        for key,node in nodes.items():
            assert node['rotation'][:2] == [0,0]
            transform = node_matrix(nodes,key)
            for c in node['cubes']:
                x,y,z = transform @ Vector(c['pos']); w,h,d = c['size']; q = c['inflate']
                assert node['rotation'][2] == 0 or z-q >= -4
                assert not (x-q < ex < x+w+q and y-q < -3.5 < y+h+q and z-q < -4), 'Eye is occluded'


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
            for slot in objects:
                for obj in objects[slot]: obj.data.materials[0] = original[slot]
            scene.render.filepath = str(ART/'preview-original-same-camera.png'); bpy.ops.render.render(write_still=True)
            for slot in objects:
                for obj in objects[slot]: obj.data.materials[0] = mats[slot]
    bpy.ops.wm.save_as_mainfile(filepath=str(ART/'aufheben_lightward.blend'))


ART.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(JAR) as jar:
    for slot in SLOTS:
        raw = jar.read('assets/malum/textures/armor/'+REF[slot]['texture']+'.png')
        (ART/(slot+'-original.png')).write_bytes(raw); source = png_read(raw)
        assert len(source) == len(source[0]) == 128
        png_write(ASSETS/f'textures/models/armor/aufheben_lightward_{slot}.png',retexture(slot,source))
    thigh_panel = ((4,7,'2D2F35'),(4,8,'18191C'),(4,9,'1E1F24'),(4,10,'18191C'),(5,10,'2D2F35'),
                   (4,11,'111214'),(5,11,'18191C'),(4,12,'18191C'),(5,12,'25232E'))
    leggings_accents = ((7,3,'B88CE4'),(8,3,'A181D8'),(6,4,'62C6CF'),(9,4,'57B1BF'),
                        (5,4,'D8B57A'),(10,4,'C29C65'),(4,5,'B9925A'),(11,5,'A27A4A'))
    leggings_accents += thigh_panel + tuple((15-x,y,color) for x,y,color in thigh_panel)
    boot_bevel = ((4,5,'D8B57A'),(5,5,'AD8956'),(4,6,'AA84D6'),(5,6,'57B7C8'),
                  (4,7,'18191C'),(5,7,'1E1F24'),(4,8,'1E1F24'),(5,8,'2D2F35'),
                  (4,9,'C6CFDA'),(3,10,'A5AAB8'),(2,11,'989DB2'))
    boot_accents = boot_bevel + tuple((15-x,y,color) for x,y,color in boot_bevel)
    icons = {}
    for slot,recolor in (('helmet',helmet_icon),('chestplate',chestplate_icon)):
        path = ASSETS/f'textures/item/aufheben_lightward_{slot}.png'
        icons[slot] = recolor(png_read(path.read_bytes()))
    for slot,accents in (('leggings',leggings_accents),('boots',boot_accents)):
        source = png_read(jar.read(f'assets/malum/textures/item/soul_stained_steel_{slot}.png'))
        icons[slot] = armor_icon(source,accents)
    for slot,icon in icons.items():
        png_write(ASSETS/f'textures/item/aufheben_lightward_{slot}.png',icon)
        png_write(ART/f'preview-{slot}-icon.png',[[pixel for pixel in row for _ in range(16)]
                                                 for row in icon for _ in range(16)])
    background = (105,109,121,255)
    png_write(ART/'preview-item-set.png',[
        [pixel if pixel[3] else background for slot in SLOTS for pixel in icons[slot][y] for _ in range(8)]
        for y in range(16) for _ in range(8)])
blockbench(); eye_clearance()
(ART/'geometry.json').write_text(json.dumps(REF,indent=2),encoding='utf-8')
preview()
print('Exported all four armor pieces using the original geometry and UVs. Four eye rays clear.')
