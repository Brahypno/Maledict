"""Blender authoring, atlas, editable project and runtime mesh export. Run from repo root.

Java authoring coordinates: X left, Y down, Z back. Blender: (X, Z, -Y).
Every mesh is rigidly parented to its matching runtime joint; no subdivision-only detail.
"""
import bpy
import bmesh
import json
import math
import sys
import shutil
from pathlib import Path
from mathutils import Vector, Matrix, Euler

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / 'art/first-vicissitude'
TEX = ROOT / 'src/main/resources/assets/maledict/textures/entity'
MESH = ROOT / 'src/main/resources/assets/maledict/models/entity'
RIG = json.loads((ROOT / 'build/rig-tool/blender-rig.json').read_text())
PREVIEW = ART / 'preview/blender'
for directory in (TEX, MESH, PREVIEW):
    directory.mkdir(parents=True, exist_ok=True)
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
bpy.context.scene.render.engine = 'CYCLES'
bpy.context.scene.cycles.samples = 32
bpy.context.scene.cycles.use_denoising = True
bpy.context.scene.render.resolution_x = 1500
bpy.context.scene.render.resolution_y = 1300
bpy.context.scene.render.resolution_percentage = 100
bpy.context.scene.render.image_settings.file_format = 'PNG'
bpy.context.scene.world.color = (.15, .15, .15)
bpy.context.scene.view_settings.view_transform = 'AgX'
bpy.context.scene.render.film_transparent = False
bpy.context.preferences.filepaths.save_version = 0

BASIS = Matrix(((1,0,0),(0,0,1),(0,-1,0)))
def bv(v): return BASIS @ Vector(v)
def jv(v): return BASIS.transposed() @ Vector(v)
def mix(a,b,t): return Vector(a).lerp(Vector(b),t)

# Structural materials and original feather cutouts share the 256px atlas.
PALETTE = [
    ('Obsidian ritual shell', '272032', 0),
    ('Bruised violet ceramic', '624974', 0),
    ('Cold relic bone', 'CED3DF', 0),
    ('Recessed bone', '78839E', 0),
    ('Broken silver order', 'ABBCCE', 0),
    ('Cold star core', 'E6EDF5', .85),
    ('Muted fate seam', '7960A5', .55),
    ('Charcoal vestment', '211C2D', 0),
    ('Ash feather edge', '8598B5', 0),
    ('Umbral primary feather', '51485F', 0),
    ('Slate overlapping feather', '8794AC', 0),
    ('Broken slate feather', '8794AC', 0),
    ('Pale shoulder covert', 'B6C0CF', 0),
    ('Load bearing violet armor', '554262', 0),
    ('Ivory armor facing', 'C4CCDA', 0),
    ('Inscribed violet halo', '59436C', 0),
]

def atlas(emissive=False):
    name = 'first_vicissitude' + ('_emissive' if emissive else '')
    img = bpy.data.images.new(name, width=256, height=256, alpha=True)
    pixels = [0.0] * (256*256*4)
    for y in range(256):
        for x in range(256):
            idx = (y//64)*4+x//64
            idx = min(idx,len(PALETTE)-1)
            _, color, glow = PALETTE[idx]
            # Deliberate four-pixel clusters, stepped wear and a broad central shaft.
            # Avoid subpixel grain/striations that read as smooth plastic at game distance.
            px,py = (x%64)//4,(y%64)//4
            u,v = px/15,py/15
            shade = .86 + (.06 if (px*3+py*5)%11 < 3 else 0)
            shade += .10 if px in (1,2,13,14) else 0
            shade -= .12 if px in (6,7) else 0
            if py in (4,10) and px in (3,4,10,11):
                shade -= .10
            if idx in (0,1,7):
                # Broad painted tonal planes and an inset seam, legible at game distance.
                shade *= .75 if .43<u<.48 else 1
                shade += .12 if .06<u<.10 else 0
            rgb = [int(color[k:k+2],16)/255 for k in (0,2,4)]
            a = 1 if not emissive or glow else 0
            if 9 <= idx <= 12:
                # Original pixel feather: asymmetric rachis, diagonal barb groups, torn edge.
                # Shape comes from a bent sheet; the cutout supplies small gaps, not volume.
                fx,fy = x%64,y%64
                u,v = (fx-3)/57,(fy-3)/57
                shaft = .46 + .035*v
                edge_distance = min(u,1-u)
                barb = int((v + abs(u-shaft)*.32)*20)
                shade = .73 + (.11 if u<shaft else 0) + (.07 if barb%3==0 else 0)
                if abs(u-shaft)<.026:
                    shade=1.12
                elif abs(u-shaft)<.06:
                    shade=.62
                if edge_distance<.075:
                    shade=.97
                slit = barb in (4,9,14,18) and edge_distance < (.14 if idx==11 else .055)
                broken = idx==11 and v>.67 and u>.68 and int(v*24)%5<3
                a = 0 if emissive or slit or broken else 1
            if idx in (13,14):
                # Quiet broad planes, inset border and sparse stepped wear, not all-over noise.
                shade=.88
                if px in (1,14) or py in (1,14): shade=.65
                if px in (2,13) or py==13: shade=1.06
                if px<4: shade+=.045
                if (px,py) in ((3,12),(4,12),(12,3),(12,4)): shade=.73
            if idx==15:
                # The silver track and crossbars are painted into the ring's radial UV.
                silver=px in (6,7,8,9) or (py in (3,11) and 3<=px<=12)
                rgb=[int(c,16)/255 for c in (('AB','BC','CE') if silver else ('59','43','6C'))]
                shade=1 if silver else .82
                if px in (1,14): shade=1.10
            for k in range(3):
                # Byte-buffer sRGB images save these values directly to PNG. Do not linearize twice.
                pixels[(y*256+x)*4+k] = min(1,rgb[k]*shade) * (glow if emissive else 1)
            pixels[(y*256+x)*4+3] = a
    img.pixels.foreach_set(pixels)
    img.filepath_raw = str(TEX / (name+'.png'))
    img.file_format = 'PNG'
    img.save()
    # Render the saved PNG, exactly the same sRGB asset the game loads.
    path=img.filepath_raw
    bpy.data.images.remove(img)
    img=bpy.data.images.load(path,check_existing=False)
    img.pack()
    return img

base = atlas()
emission = atlas(True)
materials = []
for index,(name,color,glow) in enumerate(PALETTE):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    shader = nodes.get('Principled BSDF')
    texture = nodes.new('ShaderNodeTexImage')
    texture.image = base
    texture.interpolation = 'Closest'
    mat.node_tree.links.new(texture.outputs['Color'],shader.inputs['Base Color'])
    mat.node_tree.links.new(texture.outputs['Alpha'],shader.inputs['Alpha'])
    shader.inputs['Metallic'].default_value = .4 if index==4 else .08 if index in (0,1) else 0
    shader.inputs['Roughness'].default_value = .48 if index in (0,1,4) else .72
    if glow:
        light = nodes.new('ShaderNodeTexImage')
        light.image = emission
        mat.node_tree.links.new(light.outputs['Color'],shader.inputs['Emission Color'])
        shader.inputs['Emission Strength'].default_value = 1.5
    materials.append(mat)

joints = {}
pivots = {}
for data in RIG['joints']:
    name = data['name']
    pivot = Vector(data['pivot'])
    obj = bpy.data.objects.new(name,None)
    bpy.context.collection.objects.link(obj)
    obj.empty_display_type = 'PLAIN_AXES'
    obj.empty_display_size = 1
    obj['runtime_joint'] = name
    if data['parent']:
        obj.parent = joints[data['parent']]
        obj.location = bv(pivot-pivots[data['parent']])
    else:
        obj.location = bv(pivot)
    joints[name] = obj
    pivots[name] = pivot

meshes = []
def mesh(name,joint,verts,faces,mat=0,uvs=None):
    data = bpy.data.meshes.new(name)
    data.from_pydata([bv(Vector(v)-pivots[joint]) for v in verts],[],faces)
    data.update()
    bm = bmesh.new()
    bm.from_mesh(data)
    bmesh.ops.recalc_face_normals(bm, faces=list(bm.faces))
    bm.to_mesh(data)
    bm.free()
    obj = bpy.data.objects.new(name,data)
    bpy.context.collection.objects.link(obj)
    obj.parent = joints[joint]
    obj['runtime_joint'] = joint
    obj['material_index'] = mat
    data.materials.append(materials[mat])
    layer = data.uv_layers.new(name='AtlasUV')
    lo=[min(v[k] for v in verts) for k in range(3)]
    span=[max(v[k] for v in verts)-lo[k] for k in range(3)]
    for poly in data.polygons:
        normal=jv(poly.normal)
        axes=[k for k in range(3) if k!=max(range(3),key=lambda k:abs(normal[k]))]
        for loop in poly.loop_indices:
            vi = data.loops[loop].vertex_index
            u,v = uvs[vi] if uvs else tuple((verts[vi][k]-lo[k])/max(span[k],.001) for k in axes)
            layer.data[loop].uv = (((mat%4)*64+3+u*57)/256,
                                        ((mat//4)*64+3+v*57)/256)
    meshes.append(obj)
    return obj

def tube(name,joint,points,radii,mat=2,sides=4):
    pts = list(map(Vector,points))
    verts,faces,uvs = [],[],[]
    for i,p in enumerate(pts):
        tangent = (pts[min(i+1,len(pts)-1)]-pts[max(i-1,0)]).normalized()
        cross = tangent.cross(Vector((0,0,1))).normalized()
        if cross.length < .1: cross = tangent.cross(Vector((0,1,0))).normalized()
        normal = tangent.cross(cross).normalized()
        for k in range(sides):
            a = math.tau*k/sides
            verts.append(p+radii[i]*(cross*math.cos(a)+normal*math.sin(a)))
            uvs.append((k/(sides-1),i/(len(pts)-1)))
        if i:
            for k in range(sides):
                faces.append(((i-1)*sides+k,(i-1)*sides+(k+1)%sides,
                              i*sides+(k+1)%sides,i*sides+k))
    faces.extend([tuple(reversed(range(sides))),tuple(range((len(pts)-1)*sides,len(pts)*sides))])
    return mesh(name,joint,verts,faces,mat,uvs)

def blade(name,joint,start,end,width,mat=0,curve=2,thick=.65,ragged=False):
    a,b = Vector(start),Vector(end)
    direction = (b-a).normalized()
    side = Vector((-direction.y,direction.x,0)).normalized()
    # A twelve-triangle tapered plate. Feather shafts and wear live in the atlas.
    levels = [0,1]
    widths = [1,.18]
    verts,faces,uvs = [],[],[]
    for i,(t,w) in enumerate(zip(levels,widths)):
        center = a.lerp(b,t)+side*((1-abs(2*t-1))*curve*.45)
        notch = .74 if ragged and i in (3,5) else 1
        for offset,depth,u in [(-width*w*notch,-thick*.5,0),(width*w,-thick*.5,1),
                                (width*w,thick*.5,1),(-width*w*notch,thick*.5,0)]:
            verts.append(center+side*offset+Vector((0,0,depth)))
            uvs.append((u,t))
        if i:
            for k in range(4):
                faces.append(((i-1)*4+k,(i-1)*4+(k+1)%4,i*4+(k+1)%4,i*4+k))
    faces.extend([(3,2,1,0),tuple(range((len(levels)-1)*4,len(levels)*4))])
    return mesh(name,joint,verts,faces,mat,uvs)

def feather(name,joint,start,end,width,mat=9,bend=1.4):
    """Twelve triangles: a shallow folded vane, not a tapered solid weapon blade."""
    a,b=Vector(start),Vector(end)
    direction=(b-a).normalized()
    side=Vector((-direction.y,direction.x,0)).normalized()
    verts,uvs,faces=[],[],[]
    for i,(t,w) in enumerate([(0,.38),(.34,1),(.77,.82),(1,.09)]):
        center=a.lerp(b,t)+side*(bend*(1-abs(2*t-1)))
        for lateral,depth,u in [(-width*w,0,0),(0,-.65*math.sin(math.pi*t),.47),(width*w,0,1)]:
            verts.append(center+side*lateral+Vector((0,0,depth)))
            uvs.append((u,t))
        if i:
            for lane in range(2):
                k=(i-1)*3+lane
                faces.append((k,k+1,k+4,k+3))
    return mesh(name,joint,verts,faces,mat,uvs)

def arc(name,joint,center,rx,ry,start,end,thickness=1,mat=4,zshift=0):
    return relic_arc(name,joint,Vector(center)+Vector((0,0,zshift)),rx,ry,start,end,thickness,mat)

def block(name,joint,center,size,mat=1):
    c=Vector(center)
    verts=[c+Vector((x*size[0]/2,y*size[1]/2,z*size[2]/2))
           for z in (-1,1) for y in (-1,1) for x in (-1,1)]
    return mesh(name,joint,verts,[(0,1,3,2),(4,6,7,5),(0,4,5,1),
                                (2,3,7,6),(0,2,6,4),(1,5,7,3)],mat)

def armor(name,joint,outline,front,back,mat=1):
    # Broad inset face, chamfer and substantial sidewall; no pyramid fan across the chest.
    n=len(outline)
    cx,cy=sum(x for x,y in outline)/n,sum(y for x,y in outline)/n
    bevel=min(.7,(back-front)*.4)
    verts=[(x,y,z) for z in (front+bevel,back) for x,y in outline]
    verts += [(cx+(x-cx)*.82,cy+(y-cy)*.82,front) for x,y in outline]
    faces=[tuple(range(2*n,3*n)),tuple(range(n,2*n))]
    faces += [(2*n+i,2*n+(i+1)%n,(i+1)%n,i) for i in range(n)]
    faces += [(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    return mesh(name,joint,verts,faces,mat)

def relic_arc(name,joint,center,rx,ry,start,end,width=1.3,mat=2):
    # Flat annular band with tangible broken ends; radial UV follows its engraved track.
    c=Vector(center)
    verts,faces,uvs=[],[],[]
    steps=max(3,math.ceil(abs(end-start)/15))
    for i in range(steps+1):
        t=i/steps
        a=math.radians(start+(end-start)*t)
        p=c+Vector((rx*math.cos(a),ry*math.sin(a),math.sin(t*math.pi)*.7))
        radial=Vector((math.cos(a),math.sin(a),0))
        w=width*(.65+.35*math.sin(t*math.pi)**.7)
        for dr,dz,u in [(-w,-.48,0),(w,-.48,1),(w,.48,1),(-w,.48,0)]:
            verts.append(p+radial*dr+Vector((0,0,dz)))
            uvs.append((u,t))
        if i:
            for k in range(4): faces.append(((i-1)*4+k,(i-1)*4+(k+1)%4,i*4+(k+1)%4,i*4+k))
    faces.extend([(3,2,1,0),tuple(range(steps*4,steps*4+4))])
    return mesh(name,joint,verts,faces,mat,uvs)

# Broad shoulder/chest masses taper into an open rib cage; load-bearing joints stay thick.
for s,label in [(1,'left'),(-1,'right')]:
    j='chest_shell_'+label
    tube('Thoracic arch '+label,j,[(s*5,-21,3),(s*9.3,-16,3),(s*9,-8,3),(s*5,-2,3)],
         [1.8,2.1,1.7,1.25],3,6)
    armor('Clavicular yoke '+label,'torso',
          [(s*x,y) for x,y in [(1,-24),(5,-24.5),(11.5,-21.5),(11,-19),(5,-20),(1,-22)]],-2.9,3.5,14)
    tube('Scapular wing load bridge '+label,'body',[(s*5,-22,4),(s*8,-21,7),(s*10,-18,9)],[2.8,3.4,2.8],3,6)
    armor('Dorsal scapular plate '+label,'chest_shell_back',
          [(s*x,y) for x,y in [(2,-22),(7,-23),(10,-19),(8,-12),(4,-14)]],5.3,8,13)
    armor('Faceted thoracic flank '+label,j,
          [(s*x,y) for x,y in [(3.8,-14.8),(8.3,-17),(9,-11),(7.6,-5),(3.7,-5.8),(3.1,-9.5)]],-3.7,5.5,13)
    armor('Pectoral shield '+label,'torso',
          [(s*x,y) for x,y in [(.45,-22.4),(6.2,-23),(10,-20),(9,-15.2),(4,-13.7),(.45,-15.4)]],-5.8,2.8,13)
    armor('Pectoral ivory rim '+label,'torso',
          [(s*x,y) for x,y in [(1,-22.5),(6,-23.1),(9.9,-20.2),(9.4,-18.8),(5.6,-21),(1,-20.8)]],-6.1,-5.5,14)
    tube('Cervical fork '+label,'body',[(0,-26,3),(s*5,-20,3)],[1.4,1.8],2)
    tube('Lumbar connection '+label,'lower_root',[(s*5,-3,3),(0,3,2)],[1.6,1.7],3)
    for i,y in enumerate((-14,-9.8,-5.6)):
        tube('Wrapping costal arch %s %d'%(label,i),j,
             [(s*5.8,y,6),(s*9.3,y+.2,3.5),(s*9.3,y+.7,-1.5),(s*5.2,y+1.4,-4.8)],
             [.75,1.05,1.1,.7],14,4)
armor('Recessed sternal bridge','torso',
      [(-.85,-22.5),(.85,-22.5),(.85,-16),(0,-14.2),(-.85,-16)],-4.9,3.8,0)
armor('Faceted abdominal body','torso',
      [(-5.5,-5.8),(0,-5.2),(5.5,-5.8),(6.5,-2),(3,1.5),(-3,1.5),(-6.5,-2)],-3.6,5,0)
for s,label in [(1,'left'),(-1,'right')]:
    armor('Abdominal overlapping plate '+label,'torso',
          [(s*x,y) for x,y in [(.5,-4.5),(5.8,-5.4),(6,-1),(2,2),(.5,.5)]],-4.4,-2.8,13)
for name,joint,center,rx,ry,a,b,w in [
    ('Ascending fate remnant','chest_ring_left',(.25,-10,-5.7),3.6,4.6,-62,24,.65),
    ('Displaced fate remnant','chest_ring_right',(-.3,-10,-6.1),3.8,4.5,134,214,.8),
    ('Fallen fate remnant','chest_ring_bottom',(.3,-10,-5.5),3.5,4.6,54,92,.8)]:
    relic_arc(name,joint,center,rx,ry,a,b,w,2)
    relic_arc(name+' dark socket rim',joint,Vector(center)+Vector((0,0,.9)),rx+.35,ry+.35,a-3,b+3,w*1.25,13)

# Preserve the nested nonhuman crystal and shell joints used by the death reveal.
head_center=Vector((0,-32,0))
crystal_faces=[(0,2,4),(0,4,3),(0,3,5),(0,5,2),(1,4,2),(1,3,4),(1,5,3),(1,2,5)]
mesh('Nested star crystal core','head_core',
     [(0,-36.2,0),(0,-28.2,0),(-3.1,-32,0),(3.1,-32,0),(0,-32,-3.1),(0,-32,3.1)],crystal_faces,5)
shell_points=list(map(Vector,[(0,-40,0),(.4,-25.4,.2),(-6.4,-32.4,0),(6.6,-31.6,.3),(0,-32,-5.7),(0,-32,5.5)]))
for i,ids in enumerate(crystal_faces):
    points=[shell_points[k] for k in ids]
    center=sum(points,Vector())/3
    normal=(points[1]-points[0]).cross(points[2]-points[0]).normalized()
    if normal.dot(center-head_center)<0: normal=-normal
    points=[p.lerp(center,.09 if i!=1 else .14) for p in points]
    joint='head_shell_top' if i==2 else 'head_shell_left' if center.x<0 else 'head_shell_right'
    mesh('Fractured crystal shell %d'%i,joint,points+[p-normal*.6 for p in points],
         [(0,1,2),(5,4,3),(0,3,4,1),(1,4,5,2),(2,5,3,0)],[1,0,1,3,0,1,0,1][i])
    if i in (0,1):
        a,b=points[0],points[1]
        tube('Crystal edge clasp %d'%i,joint,[a.lerp(b,.12),a.lerp(b,.68)],[.28,.18],14,4)
for label,joint,points in [
    ('left','head_shell_left',[(-.7,-40.5,1.3),(-7.5,-32.8,1.3),(-3.3,-28,1.3)]),
    ('right','head_shell_right',[(3.2,-37.5,.8),(7.6,-31.9,.8),(1.1,-25,.8)])]:
    tube('Broken crystal enclosure '+label,joint,points,[.2,.55,.14],3)
blade('Upper star ray','head_shell_top',(-.3,-37.5,0),(-1.2,-42,1),.7,2,0,.65)
blade('Offset star splinter','head_shell_right',(5.5,-32,0),(8.2,-33.1,.4),.55,1,0,.55)

legacy=[-30,99,163,-108]
for i,(start,end) in enumerate([(-162,-74),(-57,19),(42,105),(126,158)],1):
    before=len(meshes)
    arc('Fate arc %d'%i,'halo_fragment_'+str(i),(0,-28,9),14.1,15,start,end,1.15,15)
    rot=BASIS @ Euler((0,0,math.radians(-legacy[i-1]))).to_matrix() @ BASIS.transposed()
    for obj in meshes[before:]:
        for v in obj.data.vertices: v.co=rot @ v.co

for s,label in [(1,'left'),(-1,'right')]:
    p=Vector((s*10,-15,0))
    upper,fore,hand='upper_arm_'+label,'forearm_'+label,'hand_'+label
    tube('Humerus '+label,upper,[p,p+Vector((s*.4,5,0)),p+Vector((0,12,0))],[2.4,2.1,2],3,6)
    armor('Load bearing pauldron '+label,upper,
          [(s*x,y) for x,y in [(8,-21.8),(12,-22.3),(16,-19.5),(16.6,-15),(13.6,-11.8),(8,-13.5)]],-3.6,5.8,13)
    armor('Pauldron ivory crown '+label,upper,
          [(s*x,y) for x,y in [(8.2,-22),(12,-22.5),(16.2,-19.5),(16.5,-17.8),(12,-20.2),(8.2,-19.8)]],-4,-3.3,14)
    armor('Upper arm ossuary mass '+label,upper,
          [(p.x+s*x,p.y+y) for x,y in [(-2.2,1),(2.6,.5),(3,4.8),(2,10),(-1.8,10),(-2.6,4.5)]],-2.9,2.4,14)
    tube('Elbow axle '+label,fore,[p+Vector((-2.3,12,0)),p+Vector((2.3,12,0))],[1.8,1.8],3,6)
    for side in (-1,1):
        block('Elbow cheek %s %d'%(label,side),upper,p+Vector((side*2.1,10.7,.2)),(1.1,3.1,3.5),13)
    for k in (-1,1):
        tube('Forearm paired strut %s %d'%(label,k),fore,
             [p+Vector((k*1.05,12,0)),p+Vector((k*1.1,22,0))],[1.05,.95],3,4)
    armor('Weighted vambrace '+label,fore,
          [(p.x+s*x,p.y+y) for x,y in [(-2.4,12.7),(2.5,12.3),(3.2,15.3),(2.8,19.5),(1.9,22),(-1.9,22),(-2.9,17)]],-3.1,2.6,13)
    armor('Vambrace ivory keel '+label,fore,
          [(p.x+s*x,p.y+y) for x,y in [(-1.3,14),(1.3,14),(1.65,17),(.7,20.6),(-.7,20.6),(-1.65,17)]],-3.5,-2.8,14)
    block('Continuous wrist cuff '+label,fore,p+Vector((0,21.4,0)),(4.3,1.6,4.3),3)
    block('Inset wrist bridge '+label,hand,p+Vector((0,22.3,0)),(3.3,2.5,3.4),0)
    block('Closed gauntlet palm '+label,hand,p+Vector((0,23.7,.65)),(4.8,3.8,3.5),13)
    for finger,x in enumerate((-1.55,0,1.55)):
        # Broad knuckles project from the palm; curled tips return underneath it.
        # Shallow gaps stop at the shared palm, keeping a closed fist silhouette.
        block('Closed finger knuckle %s %d'%(label,finger),hand,
              p+Vector((s*x,23.95,-1.9)),(1.43,3.0,1.65),14)
        block('Curled fingertip %s %d'%(label,finger),hand,
              p+Vector((s*x,25.15,-.65)),(1.43,1.0,2.8),14)
    armor('Opposed thumb plate '+label,hand,
          [(p.x+s*x,p.y+y) for x,y in [(-2.1,22.7),(-3.15,23.1),(-3.25,24.9),(-2.1,25.5),(-1.6,24.4)]],
          -1.6,1.7,14)

# Articulated ossuary frame with overlapping feather fans.
# Major bones stay solid; thin vanes carry original cutout pixels and follow existing joints.
for s,label in [(1,'left'),(-1,'right')]:
    def pt(x,y,z=10): return (s*x,y,z)
    prefix='wing_'+label+'_'
    for part,points,radii in [
        ('upper',[pt(9,-18,9),pt(18,-23,10),pt(27,-20,11)],[2.6,2.1,1.7]),
        ('outer',[pt(27,-20,11),pt(34,-26,12),pt(41,-22,12)],[1.9,1.55,1.3]),
        ('lower',[pt(41,-22,12),pt(52,-26,12),pt(61,-27,12)],[1.5,1,.25])]:
        tube('Articulated wing spar '+label+' '+part,prefix+part,points,radii,2)
    # Fork below the elbow: a visible negative space instead of a membrane-filled triangle.
    tube('Open radial fork '+label,prefix+'upper',
         [pt(12,-18,11),pt(19,-13,13),pt(27,-20,11)],[1.25,1.05,1.5],3)
    for i,(x,y,part) in enumerate([(27,-20,'outer'),(41,-22,'lower')]):
        block('Wing joint collar %s %d'%(label,i),prefix+part,pt(x,y,11),(3,3,3),3)
    # Bone fingers radiate from the elbow/wrist; their ends remain visible among the vanes.
    for i,(root,end,part) in enumerate([
        ((21,-20,12),(24,1,14),'upper'),
        ((29,-22,12),(38,3,16),'outer'),
        ((41,-23,12),(55,-2,16),'lower')]):
        a,b=Vector(pt(*root)),Vector(pt(*end))
        tube('Radiating bone finger %s %d'%(label,i),prefix+part,
             [a,a.lerp(b,.54)+Vector((s*1.5,0,0)),b],[1.0,.7,.16],2)

    # Inner secondaries close the shoulder-to-elbow surface; tips remain individually readable.
    for i in range(5):
        x=13+i*3.4
        feather('Inner secondary %s %d'%(label,i),prefix+('upper' if i<3 else 'outer'),
                pt(x,-18,14),pt(x+3,1+i*1.5,16),3.9,10 if i%2==0 else 9,s*.6)

    ends=[(30,7),(36,10),(43,12),(50,10),(56,6),(61,0),(64,-7),(64,-15)]
    for i,(ex,ey) in enumerate(ends):
        joint=prefix+'feather_'+str(i//2+1)
        x=24+i*4.2
        a=Vector(pt(x,-21-(i*.45),12+(i%2)*.55))
        b=Vector(pt(ex,ey,16+(i%3)*.65))
        damaged=label=='right' and i in (2,5)
        if damaged:
            b=a.lerp(b,.76)
        feather('Fanned primary %s %d'%(label,i),joint,a,b,4.7,
                11 if damaged else 9,s*(1.2 if i<5 else -.5))
        # Front and back coverts overlap primary roots; broad patches prevent picket-fence gaps.
        feather('Front covert %s %d'%(label,i),joint,a+Vector((-s*.7,-1,-2.0)),
                a.lerp(b,.61)+Vector((-s*.7,1,-2.2)),3.65,
                12 if i<3 else 10,s*.5)
        if i%2==0:
            feather('Dorsal covert %s %d'%(label,i),joint,a+Vector((s*.5,-1,2.0)),
                    a.lerp(b,.48)+Vector((s,0,2.4)),4.1,10,-s*.6)

    # The damaged right wing exposes two extra distal bone lengths, not uniformly shorter feathers.
    for i in range(2):
        x=41+i*10
        joint=prefix+'broken_'+str(i+1)
        end=pt(x+7,-1-i*4,14) if label=='right' else pt(x+5,-7-i*4,14)
        tube('Exposed broken quill %s %d'%(label,i),joint,
             [pt(x,-23,13),end],[.7,.12],2)

for i in range(3):
    joint='spine_tail_'+str(i+1)
    y=[2,11,19][i]
    width=[4.6,3.4,2.1][i]
    tube('Vertebral spindle %d'%i,joint,[(0,y-1,2),(0,y+8,2)],[width*.65,.85 if i<2 else .32],3)
    armor('Ventral ossuary segment %d'%i,joint,
          [(-width*.7,y-2),(width*.7,y-2),(width,y+.7),(width*.5,y+5),
           (0,y+8),(-width*.5,y+5),(-width,y+.7)],-2,1,13)
    armor('Dorsal vertebral plate %d'%i,joint,
          [(-width*.7,y-1),(width*.7,y-1),(width*.85,y+2),(0,y+7),(-width*.85,y+2)],3.6,5.2,13)
    for s in (-1,1):
        armor('Transverse ossuary flange %d %d'%(i,s),joint,
              [(s*x,yy) for x,yy in [(width*.4,y),(width+1.3,y-1),(width+1.5,y+1),
                                     (width*.7,y+3)]],-.6,2.5,14)
for s,label in [(1,'left'),(-1,'right')]:
    blade('Floating pelvic relic '+label,'lower_fragment_'+label,(s*5,1,2),(s*6,9,2),1.65,1,0,1)
    blade('Vestment pennant '+label,'cloth_fragment_'+label,(s*7,5,3),(s*8,21-(2 if s<0 else 0),4),2.15,7,0,.35)

def set_pose(name):
    for data in RIG['joints']:
        j = data['name']
        channel = RIG['poses'][name][j]
        loc = pivots[j] - (pivots[data['parent']] if data['parent'] else Vector((0,0,0)))
        joints[j].location = bv(loc+Vector(channel['offset']))
        rot = Euler(tuple(math.radians(v) for v in channel['rotation']),'XYZ').to_matrix()
        joints[j].rotation_mode = 'QUATERNION'
        joints[j].rotation_quaternion = (BASIS @ rot @ BASIS.transposed()).to_quaternion()
    bpy.context.view_layer.update()


bpy.context.scene['runtime_rig'] = json.dumps(RIG)
bpy.context.view_layer.update()
sys.path.insert(0,str(Path(__file__).parent))
sys.dont_write_bytecode = True
from export_blender import export
export(ROOT)
for frame,name in [(1,'phase_one'),(41,'phase_two'),(81,'death_reveal')]:
    set_pose(name)
    for obj in joints.values():
        obj.keyframe_insert('location',frame=frame)
        obj.keyframe_insert('rotation_quaternion',frame=frame)
    bpy.context.scene.timeline_markers.new(name,frame=frame)
bpy.context.scene.frame_end = 81
bpy.context.scene.frame_set(1)

# A neutral studio environment, kept outside the exported asset.
world = bpy.context.scene.world
world.use_nodes = True
world.node_tree.nodes.get('Background').inputs[0].default_value = (.075,.085,.11,1)
world.node_tree.nodes.get('Background').inputs[1].default_value = .65
def area(name,position,energy,size,color):
    data = bpy.data.lights.new(name,'AREA')
    data.energy = energy
    data.shape = 'DISK'
    data.size = size
    data.color = color
    obj = bpy.data.objects.new(name,data)
    bpy.context.collection.objects.link(obj)
    obj.location = position
    obj.rotation_euler = (Vector((0,0,10))-obj.location).to_track_quat('-Z','Y').to_euler()
area('Large cool key',(-45,-65,85),130000,70,(.83,.89,1))
area('Soft frontal fill',(55,-45,15),85000,60,(.88,.83,1))
area('Bone rim',(20,45,60),160000,45,(.69,.78,1))
camdata = bpy.data.cameras.new('Review camera')
camera = bpy.data.objects.new('Review camera',camdata)
bpy.context.collection.objects.link(camera)
bpy.context.scene.camera = camera
camdata.type = 'ORTHO'
camdata.ortho_scale = 141
def camera_view(position,target=(0,0,10),scale=141):
    camera.location = position
    camera.rotation_euler = (Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler()
    camdata.ortho_scale = scale
camera_view((90,-190,64))
for screen in bpy.data.screens:
    for ar in screen.areas:
        if ar.type=='VIEW_3D':
            ar.spaces.active.region_3d.view_perspective = 'CAMERA'
bpy.ops.wm.save_as_mainfile(filepath=str(ART/'first_vicissitude.blend'))

if '--no-render' not in sys.argv:
    views = [('hero',(65,-190,48),141),('front',(0,-210,10),141),
             ('side',(210,0,10),110),('back',(0,210,10),141)]
    if '--quick' in sys.argv:
        views = views[:1]
    for phase,frame in [('phase_one',1),('phase_two',41)]:
        bpy.context.scene.frame_set(frame)
        for view,pos,scale in views:
            camera_view(pos,scale=scale)
            bpy.context.scene.render.filepath = str(PREVIEW/(phase+'_'+view+'.png'))
            bpy.ops.render.render(write_still=True)
            if view in ('front','side','back'):
                shutil.copyfile(PREVIEW/(phase+'_'+view+'.png'), ART/'preview'/(phase+'_'+view+'.png'))
            print('RENDER_DONE',phase,view,flush=True)
