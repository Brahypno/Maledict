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

# Nine intentionally separated material islands in the agreed 256px atlas.
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
]

def atlas(emissive=False):
    name = 'first_vicissitude' + ('_emissive' if emissive else '')
    img = bpy.data.images.new(name, width=256, height=256, alpha=True)
    pixels = [0.0] * (256*256*4)
    for y in range(256):
        for x in range(256):
            idx = (y//64)*4+x//64
            idx = min(idx,8)
            _, color, glow = PALETTE[idx]
            u,v = (x%64)/63,(y%64)/63
            grain = math.sin(x*12.9898+y*78.233)*43758.5453 % 1
            # Longitudinal striations, bevel wear and fine interrupted transverse scoring.
            edge = max(0,1-min(u,1-u)*9)
            vein = max(0,math.cos((u*11+v*.6)*math.tau))**18
            scratch = .035 if (x*7+y*13)%137 == 0 else 0
            shade = .83 + .018*grain + .15*edge + .07*vein + scratch
            if idx in (0,1,7):
                # Broad painted tonal planes and an inset seam, legible at game distance.
                shade *= .75 if .43<u<.48 else 1
                shade += .12 if .06<u<.10 else 0
            rgb = [int(color[k:k+2],16)/255 for k in (0,2,4)]
            a = 1 if not emissive or glow else 0
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
    for poly in data.polygons:
        for loop in poly.loop_indices:
            vi = data.loops[loop].vertex_index
            u,v = uvs[vi] if uvs else ((vi%4)/3,(vi//4)%9/8)
            layer.data[loop].uv = (((mat%4)*64+3+u*57)/256,
                                        ((mat//4)*64+3+v*57)/256)
    meshes.append(obj)
    return obj

def tube(name,joint,points,radii,mat=2,sides=8):
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
    # Raised midrib, tapered tip and actual missing contour wedges.
    levels = [0,.13,.32,.51,.69,.83,.94,1]
    widths = [.23,.8,1,.87,.66,.43,.21,.012]
    verts,faces,uvs = [],[],[]
    for i,(t,w) in enumerate(zip(levels,widths)):
        center = a.lerp(b,t)+side*(math.sin(t*math.pi)*curve)
        notch = .74 if ragged and i in (3,5) else 1
        for offset,depth,u in [(-width*w*notch,0,0),(0,-thick*math.sin(.15+t*math.pi),.5),
                                (width*w,0,1),(0,thick*.4,.5)]:
            verts.append(center+side*offset+Vector((0,0,depth)))
            uvs.append((u,t))
        if i:
            for k in range(4):
                faces.append(((i-1)*4+k,(i-1)*4+(k+1)%4,i*4+(k+1)%4,i*4+k))
    faces.extend([(3,2,1,0),tuple(range(28,32))])
    return mesh(name,joint,verts,faces,mat,uvs)

def arc(name,joint,center,rx,ry,start,end,thickness=1,mat=4,zshift=0):
    c = Vector(center)
    steps = max(5,int(math.radians(abs(end-start))*max(rx,ry)/1.3))
    points = [c+Vector((rx*math.cos(math.radians(start+(end-start)*i/steps)),
                            ry*math.sin(math.radians(start+(end-start)*i/steps)),zshift))
              for i in range(steps+1)]
    return tube(name,joint,points,[thickness*(.65 if i in (0,steps) else 1) for i in range(steps+1)],mat)

def block(name,joint,center,size,mat=1):
    c=Vector(center)
    verts=[c+Vector((x*size[0]/2,y*size[1]/2,z*size[2]/2))
           for z in (-1,1) for y in (-1,1) for x in (-1,1)]
    return mesh(name,joint,verts,[(0,1,3,2),(4,6,7,5),(0,4,5,1),
                                (2,3,7,6),(0,2,6,4),(1,5,7,3)],mat)

def armor(name,joint,outline,front,back,mat=1):
    # A shaped solid with a raised ridge; broad facets replace rectangular armor boxes.
    n=len(outline)
    center=Vector((sum(x for x,y in outline)/n,sum(y for x,y in outline)/n,front-1.1))
    verts=[(x,y,z) for z in (front,back) for x,y in outline]+[center]
    faces=[(2*n,i,(i+1)%n) for i in range(n)]+[tuple(range(n,2*n))]
    faces += [(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    return mesh(name,joint,verts,faces,mat)

def relic_arc(name,joint,center,rx,ry,start,end,width=1.3,mat=2):
    # Flattened, tapering ossuary plate: a ridged cross section, not round metal stock.
    c=Vector(center)
    verts,faces,uvs=[],[],[]
    steps=max(6,int(abs(end-start)/7))
    for i in range(steps+1):
        t=i/steps
        a=math.radians(start+(end-start)*t)
        p=c+Vector((rx*math.cos(a),ry*math.sin(a),math.sin(t*math.pi)*.7))
        radial=Vector((math.cos(a),math.sin(a),0))
        w=width*(.12+.88*math.sin(t*math.pi)**.7)
        if i==steps-2: w*=.55
        for dr,dz,u in [(-w,0,0),(0,-.85,.5),(w,.2,1),(0,.65,.5)]:
            verts.append(p+radial*dr+Vector((0,0,dz)))
            uvs.append((u,t))
        if i:
            for k in range(4): faces.append(((i-1)*4+k,(i-1)*4+(k+1)%4,i*4+(k+1)%4,i*4+k))
    faces.extend([(3,2,1,0),tuple(range(steps*4,steps*4+4))])
    return mesh(name,joint,verts,faces,mat,uvs)

# Hollow rib reliquary: side ribs stop at the margin; nothing caps the front/back aperture.
for s,label in [(1,'left'),(-1,'right')]:
    j = 'chest_shell_'+label
    tube('Thoracic arch '+label,j,[(s*2,-20,2),(s*7,-18,1),(s*9,-11,1),(s*7,-3,2),(s*2,1,2)],
         [1.2,2.3,2.0,1.8,.8],2)
    blade('Lateral obsidian carapace '+label,j,(s*7,-20,1),(s*6,2,3),3.3,0,s*1.5,3.6,True)
    blade('Thoracic rear counterplate '+label,j,(s*7,-20,6),(s*6,0,7),3.0,1,s*1.4,2.8)
    for i in range(5):
        y = -18+i*3.8
        tube('Open rib %s %d'%(label,i),j,[(s*6.5,y,7),(s*9.5,y+.6,3),(s*8.5,y+1,-2.7),(s*5.1,y+1.8,-4.8)],
             [1.0,1.2,.95,.3],2 if i%2==0 else 3)
    blade('Clavicle crest '+label,'torso',(s*1.6,-22,1),(s*13,-18,2),2.5,2,s*.8,1.7)
    tube('Scapular wing load bridge '+label,'body',[(s*4,-22,4),(s*8,-20,6),(s*10,-18,9)],
         [2.5,3.2,2.8],3)
    blade('Shoulder mantle '+label,'body',(s*5,-25,4),(s*15,-17,7),3.6,1,s*1,2.8)
    # Reconstruction staples and dark backing shards at the sides only.
    for i in range(3):
        tube('Suture staple %s %d'%(label,i),j,[(s*5.8,-15+i*4,-2),(s*6.5,-14.6+i*4,-3),(s*7,-14+i*4,-1)],
             [.16,.16,.16],4,6)
# The empty destiny socket is surrounded by broken ossuary plates and interrupted star tracks.
# Broad pectoral, flank and abdominal masses frame a small aperture instead of an empty torso.
chest_outline=[(-7,-21),(0,-23),(7,-21),(8,-18),(4,-13.5),(0,-14),(-4,-13.5),(-8,-18)]
armor('Faceted thoracic mantle','torso',chest_outline,-4.8,5,1)
armor('Faceted abdominal body','torso',[(-5.5,-5.8),(0,-5.2),(5.5,-5.8),(6,-2),(2,1),(-2,1),(-6,-2)],-3.6,5,0)
for s,label in [(1,'left'),(-1,'right')]:
    armor('Faceted thoracic flank '+label,'chest_shell_'+label,
          [(s*x,y) for x,y in [(3.5,-13.8),(7.3,-15),(8,-11),(7.4,-6),(3.4,-5.8),(2.9,-9.5)]],-4.2,5.5,1)
    blade('Pectoral ivory facing '+label,'torso',(s*.6,-20,-5.5),(s*6.6,-15.1,-5.5),1.6,2,s*.4,.65)
    blade('Abdominal overlapping plate '+label,'torso',(s*3.5,-6,-4),(s*1.3,1,-3.6),2.7,1,s*.35,1.4)
for name,joint,center,rx,ry,a,b,w in [
    ('Ascending fate remnant','chest_ring_left',(.25,-10,-5.7),3.6,4.6,-62,24,.65),
    ('Displaced fate remnant','chest_ring_right',(-.3,-10,-6.1),3.8,4.5,134,214,.8),
    ('Fallen fate remnant','chest_ring_bottom',(.3,-10,-5.5),3.5,4.6,54,92,.8)]:
    relic_arc(name,joint,center,rx,ry,a,b,w,2)
    relic_arc(name+' fractured backing',joint,Vector(center)+Vector((0,0,.85)),rx+.65,ry+.65,a+9,b-6,w*1.1,0)
    arc(name+' interrupted violet track',joint,Vector(center)+Vector((0,0,-.7)),rx-.35,ry-.35,a+11,b-17,.11,6)
# Offset thin trajectories never close the socket and never fill its center.
arc('Lost orbit left','chest_ring_left',(.4,-10,-6.1),4.2,5.0,-50,7,.075,6)
arc('Lost orbit right','chest_ring_right',(-.3,-10,-6.6),4.3,4.9,150,185,.08,6)
for k,(x,y,z,j) in enumerate([(3.4,-12.3,-6.8,'chest_ring_left'),(-3.5,-8.7,-7,'chest_ring_right')]):
    blade('Fractured star ray long '+str(k),j,(x-.3,y-1.4,z),(x+.3,y+1,z),.22,5,0,.17)
    blade('Fractured star ray short '+str(k),j,(x-.8,y-.15,z),(x+.65,y+.1,z),.15,6,0,.15)
    blade('Orbit endpoint splinter '+str(k),j,(x+.7,y+2,z+.4),(x+1.2,y+3.5,z+.7),.38,2,.15,.35)
for s in (-1,1):
    blade('Dorsal split scapula','chest_shell_back',(s*5,-23,7),(s*7,-1,7),2.4,1,s*1,2.4)
    tube('Cervical fork','body',[(0,-26,3),(s*2,-23,3),(s*5,-20,3)], [1.4,1.8,1.8],2)
    tube('Lumbar connection','lower_root',[(s*5,-3,3),(s*3,0,3),(0,3,2)], [1.6,1.8,1.7],3)

# End-crystal-inspired nested core and broken geometric enclosure, with a sharp star silhouette.
head_center=Vector((0,-32,0))
core_points=[(0,-36.2,0),(0,-28.2,0),(-3.1,-32,0),(3.1,-32,0),(0,-32,-3.1),(0,-32,3.1)]
crystal_faces=[(0,2,4),(0,4,3),(0,3,5),(0,5,2),(1,4,2),(1,3,4),(1,5,3),(1,2,5)]
mesh('Nested star crystal core','head_core',core_points,crystal_faces,5)
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
    if i in (0,1,4,6):
        a,b=points[0],points[1]
        tube('Broken crystal bone edge %d'%i,joint,[a,a.lerp(b,.45),a.lerp(b,.78)], [.22,.36,.09],2,4)
# An interrupted diamond cage sits around the shell, not a complete square head or face.
for label,joint,points in [
    ('left','head_shell_left',[(-.7,-40.5,1.3),(-7.5,-32.8,1.3),(-3.3,-28,1.3)]),
    ('right','head_shell_right',[(3.2,-37.5,.8),(7.6,-31.9,.8),(1.1,-25,.8)])]:
    tube('Broken crystal enclosure '+label,joint,points,[.2,.55,.14],3,4)
blade('Upper star ray','head_shell_top',(-.3,-37.5,0),(-1.2,-42,1),.7,2,0,.65)
blade('Offset star splinter','head_shell_right',(5.5,-32,0),(8.2,-33.1,.4),.55,1,0,.55)

# Four unequal ring arcs. Counter-transform the legacy fragment bind rotations.
legacy = [-30,99,163,-108]
halo_objects = []
for i,(start,end) in enumerate([(-162,-74),(-57,19),(42,105),(126,158)],1):
    joint = 'halo_fragment_'+str(i)
    before = len(meshes)
    arc('Fate arc %d'%i,joint,(0,-28,9),14.1,15.0,start,end,1.03,1)
    arc('Silver inset %d'%i,joint,(0,-28,8.2),14.2,15.0,start+2,end-2,.30,4)
    arc('Recessed fate seam %d'%i,joint,(0,-28,9.75),14.0,14.9,start+6,end-6,.16,6)
    for a in range(start+7,end-3,11):
        r = math.radians(a)
        c = Vector((14.1*math.cos(r),-28+15*math.sin(r),7.9))
        dr = Vector((math.cos(r),math.sin(r),0))
        tube('Order incision',joint,[c-dr*.52,c+dr*.52],[.12,.12],4,4)
    rot = BASIS @ Euler((0,0,math.radians(-legacy[i-1]))).to_matrix() @ BASIS.transposed()
    for obj in meshes[before:]:
        for v in obj.data.vertices: v.co = rot @ v.co
        halo_objects.append(obj)

# Thick skeletal arms and block mitts, matching Minecraft's simplified hand vocabulary.
for s,label in [(1,'left'),(-1,'right')]:
    p = Vector((s*10,-15,0))
    upper = 'upper_arm_'+label
    fore = 'forearm_'+label
    hand = 'hand_'+label
    tube('Humerus '+label,upper,[p,p+Vector((s*.9,3,0)),p+Vector((0,8,0)),p+Vector((0,12,0))],
         [2.2,2.0,1.35,1.8],2)
    blade('Shoulder relic '+label,upper,p+Vector((-s*2,-4,1)),p+Vector((s*4,7,2)),3.5,0,s*.8,2.7)
    blade('Shoulder bone facing '+label,upper,p+Vector((0,-3,-1)),p+Vector((s*2,6,-1)),1.8,2,s*.6,1.1)
    for k in (-1,1):
        tube('Forearm split '+label,fore,[p+Vector((k*.65,12,0)),p+Vector((k*1.1,17,.2)),p+Vector((k*.55,22,0))],
             [1.0,.85,.95],2 if k==1 else 3)
    armor('Tapered forearm sleeve '+label,fore,
          [(p.x+x,p.y+y) for x,y in [(-1.8,13.5),(1.8,13.5),(2,17),(1.5,21.5),(-1.5,21.5),(-2,17)]],-1.5,1.75,1)
    blade('Forearm ivory facing '+label,fore,p+Vector((0,13.8,-2.0)),p+Vector((0,20.8,-2.0)),1.35,2,.15,.5)
    block('Continuous wrist cuff '+label,fore,p+Vector((0,21.4,0)),(3.5,1.8,3.5),3)
    block('Proportioned block hand '+label,hand,p+Vector((0,23.1,0)),(3.2,3.0,3.3),2)
    block('Hand dorsal inset '+label,hand,p+Vector((0,22.8,-1.7)),(2.2,1.5,.25),1)

# Great bone fans: a broad frontal silhouette with an exposed leading spar and stacked vanes.
for s,label in [(1,'left'),(-1,'right')]:
    def pt(x,y,z=10): return (s*x,y,z)
    prefix = 'wing_'+label+'_'
    tube('Wing root spar '+label,prefix+'upper',[pt(9,-18,9),pt(14,-21,10),pt(21,-22,10),pt(27,-20,11)],
         [3.2,2.8,2.1,2.2],2)
    tube('Wing outer spar '+label,prefix+'outer',[pt(27,-20,11),pt(32,-24,12),pt(38,-24,12),pt(41,-22,12)],
         [2.2,1.9,1.5,1.6],2)
    tube('Wing terminal spar '+label,prefix+'lower',[pt(41,-22,12),pt(48,-24,12),pt(55,-24,12),pt(59,-26,12)],
         [1.6,1.4,.85,.12],2)
    # A forked, three-dimensional load-bearing wing, rather than a single thin stick.
    tube('Ventral wing root strut '+label,prefix+'upper',
         [pt(9,-18,9),pt(15,-14,14),pt(22,-15,16),pt(27,-20,11)], [2.2,2.0,1.7,1.9],3)
    tube('Dorsal wing root strut '+label,prefix+'upper',
         [pt(10,-19,9),pt(16,-25,7),pt(22,-26,7),pt(27,-20,11)], [2.0,1.8,1.5,1.6],2)
    tube('Outer forked spar '+label,prefix+'outer',
         [pt(27,-20,11),pt(32,-17,17),pt(39,-18,17),pt(41,-22,12)], [1.8,1.6,1.3,1.2],3)
    tube('Terminal fork '+label,prefix+'lower',
         [pt(41,-22,12),pt(48,-19,17),pt(54,-21,17),pt(59,-26,12)], [1.4,1.2,.9,.1],2)
    for x,y,part in [(11,-19,'upper'),(27,-20,'outer'),(41,-22,'lower')]:
        relic_arc('Exposed wing socket '+label,prefix+part,pt(x,y,8),2.3,2.1,10,327,.7,3)
    # Curved dorsal lamellae connect the upright crests into a relic-like leading edge.
    for x,y,part in [(15,-22,'upper'),(29,-24,'outer'),(42,-24,'lower')]:
        tube('Dorsal ivory lamella '+label,prefix+part,
             [pt(x-2,y,10),pt(x,y-3,10),pt(x+4,y-4,10),pt(x+7,y-3,11)],
             [.4,.55,.38,.03],2,6)
    for x,y,j in [(13,-20,'upper'),(20,-22,'upper'),(29,-22,'outer'),(36,-24,'outer'),(44,-24,'lower')]:
        blade('Raised scapular shard '+label,prefix+j,pt(x-2,y+1,9),pt(x+2,y-7,10),1.4,1,s*1,.8,True)
        blade('Scapular worn ridge '+label,prefix+j,pt(x-1,y,8.6),pt(x+2,y-6.5,9.7),.3,2,s*.7,.4)
    # Inner bone web remains open between the forked spars.
    for i in range(7):
        x = 14+i*4.1
        joint = prefix+('upper' if i<3 else 'outer')
        tube('Exposed wing rib %s %d'%(label,i),joint,
             [pt(x,-21,11),pt(x+1,-14,14),pt(x+6,-8,17),pt(x+11,-6+i*.8,18)],
             [1.2,1.0,.75,.1],2)
        tube('Branching wing rib %s %d'%(label,i),joint,
             [pt(x+1,-14,14),pt(x+6,-13,10),pt(x+12,-15,9)], [.7,.6,.05],2)
        blade('Inner secondary %s %d'%(label,i),joint,
              pt(x+1,-16,16),pt(x+10,5+i*.7,19),2.9 if label=='left' else 2.4,1,s*2,1.5,True)
    for i in range(10):
        joint = prefix+'feather_'+str(min(4,i//3+1))
        x = 23+i*3.25
        length = [25,28,30,32,33,32,30,28,25,21][i] * (1 if label=='left' or i%3 else .85)
        start = Vector(pt(x,-21,11.2+(i%2)*1.1))
        end = Vector(pt(min(62,x+12),-21+length,16+(i%3)*1.5))
        blade('Primary vane %s %d'%(label,i),joint,start,end,3.0 if label=='left' else 2.65,
              0 if i%2==0 else 1,s*2.2,1.55,True)
        tube('Primary ivory rachis %s %d'%(label,i),joint,[start,start.lerp(end,.35)+Vector((s*1.5,0,-.85)),
             start.lerp(end,.78)+Vector((s*1.0,0,-.55)),end],[.72,.55,.32,.035],2,6)
        # Offset overlapping vane, shorter and chipped; these are geometry, not painted feathers.
        blade('Overlapping covert %s %d'%(label,i),joint,start+Vector((-s*1,-1,-3)),
              start.lerp(end,.70)+Vector((-s*2,0,-3)),2.25,8,s*1.8,1.0,True)
        blade('Dorsal feather layer %s %d'%(label,i),joint,start+Vector((s*.7,-3,4.5)),
              start.lerp(end,.87)+Vector((s*1.5,-2,5)),2.45,1,s*1.7,1.2,True)
        # Bone plaques ride over the vane bases, echoing the layered prototype silhouette.
        blade('Ivory feather root %s %d'%(label,i),joint,start+Vector((-s*.3,-1,-3.8)),
              start.lerp(end,.35)+Vector((s*1,-.5,-3.5)),1.1,2,s*.5,.7)
    for i in range(7):
        x=14+i*5.3
        joint=prefix+('upper' if i<3 else 'outer' if i<5 else 'lower')
        blade('Swept leading bone vane %s %d'%(label,i),joint,
              pt(x,-23,7),pt(min(61,x+14),-16+(i%2),8),2.1,2,s*1.7,1.3,True)
        blade('Leading obsidian underplate %s %d'%(label,i),joint,
              pt(x,-22,10),pt(min(60,x+13),-17,12),2.7,0,s*1.5,1.6)
    for i in range(2):
        joint = prefix+'broken_'+str(i+1)
        x = 40+i*10
        tube('Broken naked quill '+label,joint,[pt(x,-21,13),pt(x+4,-10,14),pt(x+7,-5+i*2,14)],
             [1.0,.7,.1],2)
        blade('Remaining torn barb '+label,joint,pt(x+2,-15,13),pt(x+7,-9,14),.9,1,s*.7,.4,True)

# Three separated vertebra/vestment segments. No paired legs or continuous ghost tail.
for i in range(3):
    joint = 'spine_tail_'+str(i+1)
    y = [2,11,19][i]
    width = [4.0,3.15,1.85][i]
    tube('Vertebral spindle %d'%i,joint,[(0,y-1,2),(0,y+2.5,2),(0,y+8,2)],
         [width*.65,width*.8,.85 if i<2 else .32],3)
    blade('Ventral ossuary segment %d'%i,joint,(0,y-2,-1),(0,y+8,-1),width,0,.6,2.2,True)
    blade('Dorsal ossuary segment %d'%i,joint,(0,y-1,5),(0,y+7,5),width*.9,1,-.5,1.8)
    for s in (-1,1):
        blade('Transverse vertebral process',joint,(0,y+1,.5),(s*(width+1.7),y-1,2),.7,2,s*.5,.4)
        blade('Vertebral bone edge',joint,(s*width*.7,y,-1),(0,y+6,-1),.33,2,s*.3,.4)
for s,label in [(1,'left'),(-1,'right')]:
    blade('Floating pelvic relic '+label,'lower_fragment_'+label,(s*5,1,2),(s*6,9,2),1.65,1,s*1,1,True)
    blade('Vestment pennant '+label,'cloth_fragment_'+label,(s*7,5,3),(s*8,21-(2 if s<0 else 0),4),2.15,7,s*2,.35,True)
    blade('Torn vestment hem '+label,'cloth_fragment_'+label,(s*7,8,2.6),(s*9,17,3.5),.25,8,s*.5,.2)

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
