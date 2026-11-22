"""Grow the crystal-root relief from the Lightward boot's continuous surface.

Run with Blender's Python, then build_reference_models.py for atlases/previews.
Paths use an unfolded boot perimeter: front / outer / back / inner (6 units each).
The body layers share welded corner vertices; root ridges displace the skin.
"""
import json
import math
from pathlib import Path
from mathutils import Euler, Vector

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT/'src/main/resources/assets/maledict/models/armor/aufheben_lightward_boots.geometry.json'
SURFACE = 3.26
BODY_UV_SCALE = 4

def root_paths():
    # Four irregular rows grow through the original body material. Their
    # shared surface rises into crystal ridges, with smaller terminal roots.
    columns = (.4,2.25,4.35,6.45,8.15,10.45,12.4,14.1,16.5,18.3,20.25,22.35)
    levels = (
        (6.65,7.05,6.7,7.2,6.6,7.0,6.7,7.15,6.6,7.0,6.7,7.1),
        (8.35,8.0,8.65,8.2,8.45,8.7,8.25,8.6,8.1,8.55,8.2,8.65),
        (10.2,10.55,9.95,10.5,10.1,10.65,10.3,10.0,10.55,10.15,10.6,10.1),
        (11.85,12.3,11.7,12.25,11.8,12.4,11.9,12.3,11.75,12.15,11.8,12.35),
    )
    shifts = ((0,)*12, (.25,-.2,.2,-.25,.3,-.15,.25,-.2,.15,-.2,.2,-.15),
              (-.2,.15,-.15,.25,-.15,.2,-.1,.25,-.2,.1,-.15,.2),
              (-.15,.2,-.2,.15,-.1,.15,-.15,.2,-.15,.1,-.2,.15))
    nodes = [[(u+shifts[row][i],levels[row][i]) for i,u in enumerate(columns)] for row in range(4)]
    paths = []
    for row in range(4):
        for col in range(12):
            if row == 0 and col%3 == 2 or row == 3 and col%3 == 0:
                continue
            a,b = nodes[row][col],nodes[row][(col+1)%12]
            if col == 11:
                b = b[0]+24,b[1]
            paths.append((f'mesh_level_{row}_{col:02d}', 'tip' if row in (0,3) else 'branch', (a,b)))
    targets = ((0,2,1,3,5,4,6,7,9,8,11,10), (1,0,2,4,3,5,7,6,8,9,11,10),
               (0,2,1,3,5,4,6,8,7,9,11,10))
    for row in range(3):
        for col in range(12):
            a,b = nodes[row][col],nodes[row+1][targets[row][col]]
            tier = 'trunk' if col in (2,5,8) else 'branch'
            points = (a,b)
            if col%4 == 0:
                points = (a,((a[0]+b[0])/2+.22,(a[1]+b[1])/2-.10),b)
            paths.append((f'mesh_fork_{row}_{col:02d}',tier,points))
    return paths


PATHS = root_paths()
ROOT_WIDTH = {'trunk': .26, 'branch': .16, 'tip': .11}

# Junctions, not an applied cage: these are rooted beneath the original skin.
# (name, perimeter coordinate, height, heading of the emerging crystal root)
GROWTH_SITES = (
    ('front_low',2.40,10.55,-.48),
    ('front_outer',4.20,9.95,.42),
    ('outer_upper',8.45,8.45,-.58),
    ('outer_low',10.65,10.65,.46),
    ('rear_mid',14.35,10.00,-.35),
    ('rear_upper',16.65,8.10,.55),
    ('inner_upper',20.45,8.20,-.40),
    ('inner_low',22.55,10.10,.50),
)


def split_corners(a, b):
    cuts = [0., 1.]
    if a[0] != b[0]:
        low, high = sorted((a[0], b[0]))
        for corner in range(math.floor(low/6)+1, math.ceil(high/6)):
            fraction = (corner*6-a[0])/(b[0]-a[0])
            if 0 < fraction < 1:
                cuts.append(fraction)
    cuts.sort()
    points = [(a[0]+(b[0]-a[0])*t, a[1]+(b[1]-a[1])*t) for t in cuts]
    return list(zip(points, points[1:]))


def displacement(u,y,q):
    influence = 0
    for _,su,sy,heading in GROWTH_SITES:
        du = (u-su+12)%24-12
        dx,dy = du*SURFACE/3,y-sy
        across = dx*math.cos(heading)+dy*math.sin(heading)
        along = -dx*math.sin(heading)+dy*math.cos(heading)
        influence = max(influence,math.exp(-(across/.95)**2-(along/1.25)**2))
    ridge = 0
    for _,tier,points in PATHS:
        for a,b in zip(points,points[1:]):
            for shift in (-24,0,24):
                x = u+shift
                dx,dy = b[0]-a[0],b[1]-a[1]
                squared = dx*dx+dy*dy
                t = max(0,min(1,((x-a[0])*dx+(y-a[1])*dy)/squared))
                distance = math.hypot((x-a[0]-t*dx)*SURFACE/3,y-a[1]-t*dy)
                width = {'trunk':.43,'branch':.32,'tip':.23}[tier]
                strength = {'trunk':.56,'branch':.40,'tip':.22}[tier]
                ridge = max(ridge,strength*math.exp(-(distance/width)**2))
    fade = max(0,min(1,(y-(6-q))/.65,((13+q)-y)/.65))
    return (ridge*(.25+.75*influence)+.14*influence)*fade


def body_mesh(cube):
    q = cube['inflate']
    surface = 3+q
    columns,rows = 12,14
    vertices,faces,lookup = [],[],{}
    def vertex(point,u):
        key = tuple(round(value,6) for value in point)
        if key not in lookup:
            direction = Vector((math.copysign(1,point.x) if abs(abs(point.x)-surface)<1e-5 else 0,
                                0,math.copysign(1,point.z) if abs(abs(point.z)-surface)<1e-5 else 0))
            direction.normalize()
            point = point+direction*displacement(u,point.y,q)
            lookup[key] = len(vertices)
            vertices.append([round(v,6) for v in point])
        return lookup[key]
    def polygon(indices,uv,expected):
        a,b,c = (Vector(vertices[i]) for i in indices[:3])
        normal = (b-a).cross(c-a).normalized()
        if normal.dot(expected)<0:
            indices,uv = indices[::-1],uv[::-1]
            normal = -normal
        faces.append(dict(vertices=indices,uv=uv,normal=[round(v,6) for v in normal]))
    u0,v0 = cube['uv']
    scale = cube.get('uv_scale',1)
    uv_faces = ((u0+6,v0+6),(u0+12,v0+6),(u0+18,v0+6),(u0,v0+6))
    for face in range(4):
        frame = Euler((0,-face*math.pi/2,0),'XYZ').to_matrix()
        grid=[]
        for row in range(rows+1):
            y = 6-q+row/rows*(7+2*q)
            grid.append([vertex(frame@Vector(((-1+2*col/columns)*surface,y,-surface)),face*6+col/columns*6)
                         for col in range(columns+1)])
        uu,vv = uv_faces[face]
        for row in range(rows):
            for col in range(columns):
                indices=[grid[row][col],grid[row][col+1],grid[row+1][col+1],grid[row+1][col]]
                uv=[[(uu+(col+dx)/columns*6)*scale,(vv+(row+dy)/rows*7)*scale]
                    for dx,dy in ((0,0),(1,0),(1,1),(0,1))]
                polygon(indices,uv,frame@Vector((0,0,-1)))
    # Triangle fans reuse the same edge vertices, closing the original boot.
    for top in (True,False):
        y = 6-q if top else 13+q
        center = len(vertices)
        vertices.append([0,y,0])
        uv_u,uv_v = (u0+6 if top else u0+12)*scale,v0*scale
        def cap_uv(index):
            x,_,z=vertices[index]
            return [uv_u+(x/surface+1)*3*scale,uv_v+(1-z/surface)*3*scale]
        rim=[]
        for face in range(4):
            frame=Euler((0,-face*math.pi/2,0),'XYZ').to_matrix()
            for col in range(columns):
                point=frame@Vector(((-1+2*col/columns)*surface,y,-surface))
                rim.append(lookup[tuple(round(v,6) for v in point)])
        for i in range(len(rim)):
            pair=[rim[i],rim[(i+1)%len(rim)]]
            polygon([center,*pair],[[uv_u+3*scale,uv_v+3*scale],*[cap_uv(j) for j in pair]],
                    Vector((0,-1 if top else 1,0)))
    return dict(vertices=vertices,faces=faces,layer=0 if q<.1 else 1)


def build():
    parts={}
    reference=json.loads(Path(__file__).with_name('reference_geometry.json').read_text(encoding='utf-8'))
    for side in ('left','right'):
        cubes=[dict(cube,size=[int(v) for v in cube['size']]) for cube in reference['boots']['nodes'][side+'_boot']['cubes']]
        # The right UV mirror is encoded in its mesh UVs, not an extra overlay.
        meshes=[]
        for layer,cube in enumerate(cubes):
            cube['uv']=[0,14*layer]
            cube['uv_scale']=BODY_UV_SCALE
            mesh=body_mesh(cube)
            if cube.get('mirror'):
                for point in mesh['vertices']:
                    point[0]=-point[0]
                for face in mesh['faces']:
                    face['vertices'].reverse()
                    face['uv'].reverse()
                    face['normal'][0]=-face['normal'][0]
            meshes.append(mesh)
        parts['boot_core_'+side]=dict(parent=side+'_foot',cubes=cubes,meshes=meshes)
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    OUTPUT.write_text(json.dumps(parts,separators=(',',':'))+'\n',encoding='utf-8')
    print('Crystal roots are deformations of the two original boot surfaces: welded vertices, no applied root strips.')


if __name__ == '__main__':
    build()
