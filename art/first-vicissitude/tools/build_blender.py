"""Blender authoring, atlas, editable project and runtime mesh export. Run from repo root.

Java authoring coordinates: X left, Y down, Z back. Blender: (X, Z, -Y).
Every mesh is rigidly parented to its matching runtime joint; no subdivision-only detail.
"""
import bpy
import bmesh
import json
import math
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from preview_paths import DIAGNOSTICS, image_target
from mathutils import Vector, Matrix, Euler

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / 'art/first-vicissitude'
TEX = ROOT / 'src/main/resources/assets/maledict/textures/entity'
MESH = ROOT / 'src/main/resources/assets/maledict/models/entity'
RIG = json.loads((ROOT / 'build/rig-tool/blender-rig.json').read_text())
PREVIEW = DIAGNOSTICS
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

PALETTE = [
    ('Obsidian ritual shell', '272032', 0),
    ('Bruised violet ceramic', '624974', 0),
    ('Cold relic bone', 'CED3DF', 0),
    ('Recessed bone', '78839E', 0),
    ('Broken silver order', 'ABBCCE', 0),
    ('Cold star core', 'E6EDF5', .85),
    ('Muted fate seam', '7960A5', .55),
    ('Charcoal vestment', '211C2D', 0),
    ('Painted ossuary wing', '776185', 0),
    ('Umbral primary feather', '51485F', 0),
    ('Slate overlapping feather', '8794AC', 0),
    ('Broken slate feather', '8794AC', 0),
    ('Pale shoulder covert', 'B6C0CF', 0),
    ('Load bearing violet armor', '554262', 0),
    ('Ivory armor facing', 'C4CCDA', 0),
    ('Inscribed violet halo', '59436C', 0),
]

sys.path.insert(0,str(Path(__file__).parent))
sys.dont_write_bytecode = True
from atlas16 import PartAtlas
from surface_sample import SurfaceSample
part_atlas = PartAtlas(PALETTE)
part_atlas.used.update((x,y) for x in range(16) for y in range(7,16))
surface_sample = SurfaceSample(part_atlas)
base = bpy.data.images.new('first_vicissitude',width=256,height=256,alpha=True)
emission = bpy.data.images.new('first_vicissitude_emissive',width=256,height=256,alpha=True)
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
def mesh(name,joint,verts,faces,mat=0,uvs=None,topology=None):
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
    if surface_sample.apply(obj,verts,uvs,topology):
        meshes.append(obj)
        return obj
    region = part_atlas.allocate(name,mat)
    obj['atlas_region'] = region['name']
    layer = data.uv_layers.new(name='AtlasUV')
    lo=[min(v[k] for v in verts) for k in range(3)]
    span=[max(v[k] for v in verts)-lo[k] for k in range(3)]
    for poly in data.polygons:
        normal=jv(poly.normal)
        axes=[k for k in range(3) if k!=max(range(3),key=lambda k:abs(normal[k]))]
        for loop in poly.loop_indices:
            vi = data.loops[loop].vertex_index
            u,v = uvs[vi] if uvs else tuple((verts[vi][k]-lo[k])/max(span[k],.001) for k in axes)
            layer.data[loop].uv = ((region['x']+.5+u*(region['width']-1))/256,
                                        (region['y']+.5+v*(region['height']-1))/256)
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
    return mesh(name,joint,verts,faces,mat,uvs,('tube',sides,pts))

def blade(name,joint,start,end,width,mat=0,curve=2,thick=.65,ragged=False):
    a,b = Vector(start),Vector(end)
    direction = (b-a).normalized()
    side = Vector((-direction.y,direction.x,0)).normalized()
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
    """Six triangles; preserve both silhouette edges, paint the shaft instead of folding it."""
    a,b=Vector(start),Vector(end)
    direction=(b-a).normalized()
    side=Vector((-direction.y,direction.x,0)).normalized()
    verts,uvs,faces=[],[],[]
    for i,(t,w) in enumerate([(0,.38),(.34,1),(.77,.82),(1,.09)]):
        center=a.lerp(b,t)+side*(bend*(1-abs(2*t-1)))
        for lateral,depth,u in [(-width*w,0,0),(width*w,0,1)]:
            verts.append(center+side*lateral+Vector((0,0,depth)))
            uvs.append((u,t))
        if i:
            k=(i-1)*2
            faces.append((k,k+1,k+3,k+2))
    obj=mesh(name,joint,verts,faces,mat,uvs)
    obj['shed_delay']=8+abs(a.x)*.28+(2 if 'Dorsal' in name else 0)
    return obj

def arc(name,joint,center,rx,ry,start,end,thickness=1,mat=4,zshift=0):
    return relic_arc(name,joint,Vector(center)+Vector((0,0,zshift)),rx,ry,start,end,thickness,mat)

def block(name,joint,center,size,mat=1):
    c=Vector(center)
    verts=[c+Vector((x*size[0]/2,y*size[1]/2,z*size[2]/2))
           for z in (-1,1) for y in (-1,1) for x in (-1,1)]
    return mesh(name,joint,verts,[(0,1,3,2),(4,6,7,5),(0,4,5,1),
                                (2,3,7,6),(0,2,6,4),(1,5,7,3)],mat)

def armor(name,joint,outline,front,back,mat=1,relief=0):
    n=len(outline)
    verts=[(x,y,z) for z in (front,back) for x,y in outline]
    faces=[tuple(range(n)),tuple(range(n,2*n))]
    faces += [(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    if relief:
        # Shallow inset shoulder and convex face, confined to lower plates.
        cx=sum(p[0] for p in outline)/n
        cy=sum(p[1] for p in outline)/n
        verts += [(cx+(x-cx)*.78,cy+(y-cy)*.82,front-relief*.45) for x,y in outline]
        verts.append((cx,cy,front-relief))
        faces=faces[1:]
        faces += [(i,(i+1)%n,2*n+(i+1)%n,2*n+i) for i in range(n)]
        faces += [(2*n+i,2*n+(i+1)%n,3*n) for i in range(n)]
    return mesh(name,joint,verts,faces,mat)

def relic_arc(name,joint,center,rx,ry,start,end,width=1.3,mat=2,depth=.55):
    c=Vector(center)
    verts,faces,uvs=[],[],[]
    steps=max(3,math.ceil(abs(end-start)/15))
    for i in range(steps+1):
        t=i/steps
        a=math.radians(start+(end-start)*t)
        p=c+Vector((rx*math.cos(a),ry*math.sin(a),0))
        radial=Vector((math.cos(a),math.sin(a),0))
        w=width*(.84 if i in (0,steps) else 1)
        for dr,dz,u in [(-w,-depth,0),(w,-depth,1),(w,depth,1),(-w,depth,0)]:
            verts.append(p+radial*dr+Vector((0,0,dz)))
            uvs.append((u,t))
        if i:
            for k in range(4): faces.append(((i-1)*4+k,(i-1)*4+(k+1)%4,i*4+(k+1)%4,i*4+k))
    faces.extend([tuple(reversed(range(4))),tuple(range(steps*4,steps*4+4))])
    return mesh(name,joint,verts,faces,mat,uvs,('arc',))

# The cavity and all moving arcs share the runtime hub, not three guessed centers.
CX,CY=RIG['chest_hub']
SOCKET_RADIUS=6.4
RING_RADIUS=5.0
for side,label in [(1,'left'),(-1,'right')]:
    j='chest_shell_'+label
    tube('Thoracic load arch '+label,j,
         [(side*5,-22,3),(side*9.5,-17,3),(side*10,-9,3),(side*7,-2,3)],
         [2.3,2.8,2.4,1.6],3,4)
    tube('Clavicular bone arch '+label,'torso',
         [(side*.9,-23,0),(side*5,-23.5,-1),(side*10.5,-19.5,0)],
         [1.4,2.5,2.1],14,4)
    tube('Scapular wing load bridge '+label,'body',
         [(side*5,-22,4),(side*8,-21,7),(side*10,-18,9)],[2.8,3.4,2.8],3,4)
    armor('Broken dorsal scapula '+label,'chest_shell_back',
          [(side*x,y) for x,y in [(3,-23),(8,-22),(10,-17),(8,-13),(6,-15),(4,-14)]],5.3,8,0)
    armor('Thoracic remnant '+label,j,
          [(side*x,y) for x,y in [(7,-16.5),(10,-15),(10.5,-9),(8.6,-3),(6.8,-3.5),(7.4,-8)]],-3.5,5.5,0)
    armor('Pectoral remnant '+label,'torso',
          [(side*x,y) for x,y in [(.6,-22),(5,-23),(9,-20),(8,-17),(5.5,-16),
                                 (4.2,-17.2),(2,-15.7),(.7,-16.2)]],-4.3,2.8,13)
    tube('Pectoral bone ridge '+label,'torso',
         [(side*.9,-21,-4.7),(side*4.5,-20.8,-5),(side*8,-18,-3.8)],
         [.85,1.35,.65],14,4)
    tube('Cervical fork '+label,'body',[(0,-26,3),(side*5,-20,3)],[1.4,1.8],2)
    tube('Lumbar connection '+label,'lower_root',[(side*6,-2,3),(0,3,2)],[1.7,1.8],3)
    for i,y in enumerate((-15,-10,-5)):
        tube('Wrapping costal arch %s %d'%(label,i),j,
             [(side*7,y,6),(side*10,y+.2,3.5),(side*9.7,y+.7,-1.5),(side*7.2,y+1,-4.2)],
             [1.05,1.35,1.3,.75],14,4)
armor('Upper sternal keel','torso',[(-1,-23),(1,-23),(1.2,-17),(0,-15.8),(-1.2,-17)],-4.2,3,3)
armor('Lower ossuary bridge','torso',[(-6.5,-2.3),(-3,-2.7),(0,-1.8),(3,-2.7),(6.5,-2.3),
                                     (5,1.5),(0,3),(-5,1.5)],-3.6,5,0,relief=.7)
for i,(a,b) in enumerate([(-177,-96),(-87,-3),(6,84),(95,171)]):
    relic_arc('Ossuary socket seat %d'%i,'torso',(CX,CY,-3.8),SOCKET_RADIUS,SOCKET_RADIUS,a,b,.65,3,.9)
for i,(joint,a,b) in enumerate([('chest_ring_left',-83,22),('chest_ring_right',132,250),
                               ('chest_ring_bottom',43,108)]):
    center=(CX,CY,-6.0)
    relic_arc('Fate ring stock %d'%i,joint,center,RING_RADIUS,RING_RADIUS,a,b,.54,0,.62)

# Preserve the nested nonhuman crystal and shell joints used by the death reveal.
head_center=Vector((0,-32,0))
crystal_faces=[(0,2,4),(0,4,3),(0,3,5),(0,5,2),(1,4,2),(1,3,4),(1,5,3),(1,2,5)]
mesh('Nested star crystal core','head_core',
     [(0,-36.2,0),(0,-28.2,0),(-3.1,-32,0),(3.1,-32,0),(0,-32,-3.1),(0,-32,3.1)],crystal_faces,5)
shell_points=list(map(Vector,[(-.8,-41,0),(.8,-25.4,.2),(-7.5,-33.6,0),(7.3,-31.2,.3),(0,-32,-5.7),(0,-32,5.5)]))
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
    joint='halo_fragment_'+str(i)
    relic_arc('Fate arc %d'%i,joint,(0,-28,9),14.1,15,start,end,1.2,0,.85)
    rot=BASIS @ Euler((0,0,math.radians(-legacy[i-1]))).to_matrix() @ BASIS.transposed()
    for obj in meshes[before:]:
        for v in obj.data.vertices: v.co=rot @ v.co

for s,label in [(1,'left'),(-1,'right')]:
    p=Vector((s*10,-15,0))
    upper,fore,hand='upper_arm_'+label,'forearm_'+label,'hand_'+label
    tube('Humerus '+label,upper,[p,p+Vector((s*.4,5,0)),p+Vector((0,12,0))],[2.4,2.1,2],3,6)
    tube('Shoulder ossuary crown '+label,upper,
         [(s*8,-19,1),(s*11,-19.5,0),(s*14,-16,0),(s*12.5,-12,0)],
         [2.0,3.5,3.3,2.1],14,6)
    armor('Broken shoulder carapace '+label,upper,
          [(s*x,y) for x,y in [(10,-22),(13,-22.5),(16,-19),(15.7,-14),(13.8,-15.5),(12.4,-13),(11,-17)]],1,5.3,0)
    tube('Upper arm bone belly '+label,upper,
         [p+Vector((s*.3,1,-.8)),p+Vector((s*.9,4,-1)),p+Vector((0,9,-.5))],
         [2.65,3.05,1.9],14,6)
    tube('Elbow axle '+label,fore,[p+Vector((-2.3,12,0)),p+Vector((2.3,12,0))],[1.8,1.8],3,6)
    for side in (-1,1):
        block('Elbow cheek %s %d'%(label,side),upper,p+Vector((side*2.1,10.7,.2)),(1.1,3.1,3.5),13)
    for k in (-1,1):
        tube('Forearm paired strut %s %d'%(label,k),fore,
             [p+Vector((k*1.05,12,0)),p+Vector((k*1.1,22,0))],[1.05,.95],3,4)
    tube('Forearm radial crest '+label,fore,
         [p+Vector((-s*1.1,13,-.8)),p+Vector((-s*1.7,16,-1)),p+Vector((-s*.9,21,-.3))],
         [1.65,1.9,1.2],14,6)
    tube('Forearm ulnar crest '+label,fore,
         [p+Vector((s*1.3,13,.6)),p+Vector((s*1.8,17,.4)),p+Vector((s*.9,21,.1))],
         [1.55,1.8,1.15],3,6)
    armor('Broken outer bracer '+label,fore,
          [(p.x+s*x,p.y+y) for x,y in [(.4,13),(2.7,14),(3.1,17),(2.1,18.4),
                                      (2.5,20.2),(.7,21),(-.2,18),(.6,17)]],-.9,2.8,0)
    block('Continuous wrist cuff '+label,fore,p+Vector((0,21.4,0)),(4.3,1.6,4.3),3)
    block('Inset wrist bridge '+label,hand,p+Vector((0,22.3,0)),(3.3,2.5,3.4),0)
    block('Closed gauntlet palm '+label,hand,p+Vector((0,23.7,.65)),(4.8,3.8,3.5),13)
    for finger,x in enumerate((-1.55,0,1.55)):
        block('Closed finger knuckle %s %d'%(label,finger),hand,
              p+Vector((s*x,23.95,-1.9)),(1.43,3.0,1.65),14)
        block('Curled fingertip %s %d'%(label,finger),hand,
              p+Vector((s*x,25.15,-.65)),(1.43,1.0,2.8),14)
    armor('Opposed thumb plate '+label,hand,
          [(p.x+s*x,p.y+y) for x,y in [(-2.1,22.7),(-3.15,23.1),(-3.25,24.9),(-2.1,25.5),(-1.6,24.4)]],
          -1.6,1.7,14)

for s,label in [(1,'left'),(-1,'right')]:
    def pt(x,y,z=10): return (s*x,y,z)
    prefix='wing_'+label+'_'
    def crescent(name,controls,width,depth,steps=7):
        controls=[Vector((s*x,y,depth)) for x,y in controls]
        verts,faces,uvs=[],[],[]
        for k in range(steps+1):
            t=k/steps
            a,b,c,d=controls
            center=(1-t)**3*a+3*(1-t)**2*t*b+3*(1-t)*t*t*c+t**3*d
            tangent=3*(1-t)**2*(b-a)+6*(1-t)*t*(c-b)+3*t*t*(d-c)
            side=Vector((-tangent.y*s,tangent.x*s,0)).normalized()
            w=max(.008,width*(.06+.94*math.sin(math.pi*t**1.45)**.8)*(1-t)**.18)
            thickness=.025+.975*math.sin(math.pi*t)**.7
            verts.extend([center-side*w*.7,center+side*w*1.3,
                          center+Vector((0,0,.45*thickness))])
            uvs.extend([(0,t),(1,t),(.48,t)])
        # Collapse a section only if all three rails stay within .30 model units; tips and root never move.
        keep={0,steps}
        def preserve(a,b):
            worst,index=0,None
            for i in range(a+1,b):
                t=(i-a)/(b-a)
                error=max((verts[i*3+j]-verts[a*3+j].lerp(verts[b*3+j],t)).length for j in range(3))
                if error>worst: worst,index=error,i
            if worst>.30:
                keep.add(index)
                preserve(a,index)
                preserve(index,b)
        preserve(0,steps)
        indices=sorted(keep)
        verts=[verts[i*3+j] for i in indices for j in range(3)]
        uvs=[uvs[i*3+j] for i in indices for j in range(3)]
        for k in range(1,len(indices)):
            for lane in range(3):
                j=(k-1)*3+lane
                faces.append((j,(k-1)*3+(lane+1)%3,k*3+(lane+1)%3,k*3+lane))
        faces.extend([(2,1,0),tuple(range(len(verts)-3,len(verts)))])
        short=name in ('Root upper hook','Upper hooked spur','Middle returning spur',
                       'Low returning spur','Trailing inner spur')
        topology=None
        if short:
            # Solid first bay carries the joint; the distal blade is one two-sided sheet under entityCutoutNoCull.
            faces=[(2,1,0),(3,4,5)]
            for k in range(1,len(indices)):
                for lane in range(3 if k==1 else 1):
                    j=(k-1)*3+lane
                    faces.append((j,(k-1)*3+(lane+1)%3,k*3+(lane+1)%3,k*3+lane))
            centers=[(verts[k]+verts[k+1]+verts[k+2])/3 for k in range(0,len(verts),3)]
            distances=[0]
            for a,b in zip(centers,centers[1:]): distances.append(distances[-1]+(b-a).length)
            used=sorted({i for f in faces for i in f})
            remap={old:new for new,old in enumerate(used)}
            faces=[tuple(remap[i] for i in f) for f in faces]
            uvs=[(uvs[i][0],distances[i//3]/distances[-1]) for i in used]
            verts=[verts[i] for i in used]
            topology=('spur_sheet',used)
        obj=mesh(name+' '+label,prefix+'upper',verts,faces,8,uvs,topology)
        if short: obj['thin_spur']=True
        obj['deploy_scale']=.45
        return obj
    tube('Shared wing root '+label,prefix+'upper',
         [pt(9,-18,9),pt(14,-19,12),pt(20,-21,12)],[2.3,2,1.3],13,4)
    crescent('High crescent primary',[(13,-19),(33,-43),(64,-23),(82,-51)],4.6,12,12)
    crescent('Middle crescent primary',[(13,-18),(36,-19),(60,-31),(83,-23)],4.2,13.8,12)
    crescent('Low crescent primary',[(13,-18),(39,-7),(62,14),(84,6)],4.2,15.4,12)
    crescent('Trailing crescent primary',[(12,-17),(29,5),(45,5),(62,27)],3.2,16.2,10)
    # Hooks share their primary's parent joint so the fork cannot split.
    crescent('Root upper hook',[(20,-25),(26,-30),(22,-36),(32,-43)],1.7,12.2,6)
    crescent('Upper hooked spur',[(46,-34),(51,-38),(53,-41),(56,-47)],1.35,12.3,6)
    crescent('Middle returning spur',[(43,-24),(51,-25),(57,-20),(65,-19)],1.4,14,6)
    crescent('Low returning spur',[(38,-7),(43,-1),(49,2),(54,5)],1.25,15.6,6)
    crescent('Trailing inner spur',[(26,-3),(31,4),(27,9),(37,16)],1.2,16.4,6)
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
        feather('Front covert %s %d'%(label,i),joint,a+Vector((-s*.7,-1,-2.0)),
                a.lerp(b,.61)+Vector((-s*.7,1,-2.2)),3.65,
                12 if i<3 else 10,s*.5)
        if i%2==0:
            feather('Dorsal covert %s %d'%(label,i),joint,a+Vector((s*.5,-1,2.0)),
                    a.lerp(b,.48)+Vector((s,0,2.4)),4.1,10,-s*.6)


for i in range(3):
    joint='spine_tail_'+str(i+1)
    y=[2,11,19][i]
    width=[4.6,3.4,2.1][i]
    tube('Vertebral spindle %d'%i,joint,[(0,y-1,2),(0,y+8,2)],[width*.65,.85 if i<2 else .32],3)
    armor('Ventral ossuary segment %d'%i,joint,
          [(-width*.7,y-2),(width*.7,y-2),(width,y+.7),(width*.5,y+5),
           (0,y+8),(-width*.5,y+5),(-width,y+.7)],-2,1,13,relief=1.05 if i==0 else .8)
    armor('Dorsal vertebral plate %d'%i,joint,
          [(-width*.7,y-1),(width*.7,y-1),(width*.85,y+2),(0,y+7),(-width*.85,y+2)],3.6,5.2,13)
    for s in (-1,1):
        armor('Transverse ossuary flange %d %d'%(i,s),joint,
              [(s*x,yy) for x,yy in [(width*.4,y),(width+1.3,y-1),(width+1.5,y+1),
                                     (width*.7,y+3)]],-.6,2.5,14)
for s,label in [(1,'left'),(-1,'right')]:
    blade('Floating pelvic relic '+label,'lower_fragment_'+label,(s*5,1,2),(s*6,9,2),1.65,1,0,1)
    cloth=[]; cloth_uv=[]; cloth_faces=[]
    stations=[(0,(1.8,7.2,4.8),(-3.8,0,5.8)),
              (2,(1.8,7.8,4.8),(-4.7,0,5.8)),
              (9,(2.2,9.0,5.5),(-4.0,1.0,7.0)),
              (18,(2.8,10.5,6.5),(-2.0,3.0,9.0)),
              (28 if s>0 else 25,(3.7,11.6,7.3),(0,5,11))]
    for row,(y,xs,zs) in enumerate(stations):
        for col in range(5):
            a=col//2; b=min(2,a+1); f=(col%2)*.5
            x=xs[a]*(1-f)+xs[b]*f
            z=zs[a]*(1-f)+zs[b]*f
            if col in (1,3): z+=(-.8 if col==1 else .65)*min(1,row)
            cloth.append((s*x,y,z))
            cloth_uv.append((col/4,y/stations[-1][0]))
        if row:
            for col in range(4):
                a=(row-1)*5+col
                cloth_faces.append((a,a+1,a+6,a+5))
    mesh('Vestment pennant '+label,'cloth_fragment_'+label,cloth,cloth_faces,7,cloth_uv)

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


# Finish the atlas only after every part has allocated its own UV region.
from lower_surfaces import refine_lower_surfaces
refine_lower_surfaces(part_atlas,meshes)
bpy.context.scene['atlas_size']=part_atlas.size
for original,pixels,name in [(base,part_atlas.base,'first_vicissitude'),
                              (emission,part_atlas.emission,'first_vicissitude_emissive')]:
    original.scale(part_atlas.size,part_atlas.size)
    original.pixels.foreach_set(pixels)
    path=str(TEX/(name+'.png'))
    original.filepath_raw=path
    original.file_format='PNG'
    original.save()
    packed=bpy.data.images.load(path,check_existing=False)
    packed.pack()
    for material in materials:
        for node in material.node_tree.nodes:
            if node.type=='TEX_IMAGE' and node.image==original: node.image=packed
    bpy.data.images.remove(original)
(PREVIEW/'atlas-layout.json').write_text(json.dumps(part_atlas.report(),indent=2))
print('ATLAS_REPORT',len(part_atlas.regions),'regions',part_atlas.report()['allocated_pixels'],'pixels',flush=True)

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
    for obj in meshes:
        if 'deploy_scale' in obj:
            scale=obj['deploy_scale'] if name=='phase_one' else 1
            obj.scale=(scale,scale,scale)
            obj.keyframe_insert('scale',frame=frame)
        if 'shed_delay' in obj:
            obj.hide_render=name!='phase_one'
            obj.hide_viewport=name!='phase_one'
            obj.keyframe_insert('hide_render',frame=frame)
            obj.keyframe_insert('hide_viewport',frame=frame)
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
    views = [('hero',(65,-190,48),175),('front',(0,-210,10),175),
             ('side',(210,0,10),110),('back',(0,210,10),175)]
    if '--quick' in sys.argv:
        views = views[:1]
    for phase,frame in [('phase_one',1),('phase_two',41)]:
        bpy.context.scene.frame_set(frame)
        for view,pos,scale in views:
            camera_view(pos,scale=scale)
            bpy.context.scene.render.filepath = str(image_target(phase+'_'+view))
            bpy.ops.render.render(write_still=True)
            print('RENDER_DONE',phase,view,flush=True)
