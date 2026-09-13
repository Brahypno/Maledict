"""Export an edited .blend without rebuilding its geometry. Apply mesh modifiers before export."""
import bpy
import json
import uuid
import base64
from pathlib import Path
from mathutils import Vector, Matrix
BASIS = Matrix(((1,0,0),(0,0,1),(0,-1,0)))
def jv(v): return BASIS.transposed() @ Vector(v)

def export(root):

    ROOT = Path(root)
    ART = ROOT / 'art/first-vicissitude'
    TEX = ROOT / 'src/main/resources/assets/maledict/textures/entity'
    MESH = ROOT / 'src/main/resources/assets/maledict/models/entity'
    PREVIEW = ART / 'preview/blender'
    RIG = json.loads(bpy.context.scene['runtime_rig'])
    joints = {d['name']: bpy.data.objects[d['name']] for d in RIG['joints']}
    pivots = {d['name']: Vector(d['pivot']) for d in RIG['joints']}
    meshes = sorted((o for o in bpy.context.scene.objects if o.type == 'MESH' and 'runtime_joint' in o), key=lambda o:o.name)
    result = {'format':1,'texture_size':256,'parts':[]}
    elements = []
    children = {j:[] for j in joints}
    triangles = 0
    wing_bounds = {}
    for obj in meshes:
        data = obj.data
        data.calc_loop_triangles()
        joint = obj['runtime_joint']
        local = obj.matrix_parent_inverse @ obj.matrix_basis
        verts = [[round(c,5) for c in jv(local @ v.co)] for v in data.vertices]
        if joint.startswith('wing_'):
            wing_bounds.setdefault(joint,[]).extend(verts)
        faces = []
        for tri in data.loop_triangles:
            corners = []
            for vi,li in zip(tri.vertices,tri.loops):
                uv = data.uv_layers.active.data[li].uv
                corners.append([*verts[vi],round(uv.x,6),round(1-uv.y,6)])
            n = jv(local.to_3x3().inverted().transposed() @ tri.normal).normalized()
            faces.append({'n':[round(c,5) for c in n],'v':corners})
        result['parts'].append({'joint':joint,'name':obj.name,'triangles':faces})
        triangles += len(faces)
        uid = str(uuid.uuid5(uuid.NAMESPACE_URL,obj.name))
        children[joint].append(uid)
        bbverts = {str(i):[v[0]+pivots[joint].x,-v[1]-pivots[joint].y,v[2]+pivots[joint].z]
                   for i,v in enumerate(verts)}
        bbfaces = {}
        for i,tri in enumerate(data.loop_triangles):
            bbfaces[str(i)] = {'vertices':[str(v) for v in reversed(tri.vertices)],
                'uv':{str(v):[data.uv_layers.active.data[l].uv.x*256,(1-data.uv_layers.active.data[l].uv.y)*256]
                      for v,l in zip(tri.vertices,tri.loops)},'texture':0}
        elements.append({'name':obj.name,'uuid':uid,'type':'mesh','origin':[0,0,0],
                         'vertices':bbverts,'faces':bbfaces,'visibility':True,'color':obj['material_index']})
    (MESH/'first_vicissitude.mesh.json').write_text(json.dumps(result,separators=(',',':')))
    bounds = []
    for joint,points in wing_bounds.items():
        values = [min(p[k] for p in points) for k in range(3)] + [max(p[k] for p in points) for k in range(3)]
        bounds.append('            new Bounds(Joint.'+joint.upper()+', '+', '.join('%.5fF'%v for v in values)+')')
    java = '''package org.brahypno.maledict.rig;

import java.util.List;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

/** Blender-exported wing bounds; regenerate with art/first-vicissitude/tools/build_blender.py. */
public final class VicissitudeMeshGeometry {
    public record Bounds(Joint joint, float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ) {}
    public static final List<Bounds> WINGS = List.of(
'''+',\n'.join(bounds)+''');
    private VicissitudeMeshGeometry() {}
}
'''
    (ROOT/'src/main/java/org/brahypno/maledict/rig/VicissitudeMeshGeometry.java').write_text(java)
    def group(data):
        name = data['name']
        p = pivots[name]
        return {'name':name,'uuid':str(uuid.uuid5(uuid.NAMESPACE_DNS,name)),
                'origin':[p.x,-p.y,p.z],
                'rotation':[-RIG['poses']['phase_one'][name]['rotation'][0],
                            RIG['poses']['phase_one'][name]['rotation'][1],
                            -RIG['poses']['phase_one'][name]['rotation'][2]],'children':children[name]+[
                    group(c) for c in RIG['joints'] if c['parent']==name]}
    bb = {'meta':{'format_version':'4.10','model_format':'free','box_uv':False},
          'name':'First Vicissitude — Blender mesh','resolution':{'width':256,'height':256},
          'elements':elements,'outliner':[group(RIG['joints'][0])],
          'textures':[{'name':'first_vicissitude.png','id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'first_vicissitude_atlas')),
                       'width':256,'height':256,'uv_width':256,'uv_height':256,
                       'source':'data:image/png;base64,'+base64.b64encode((TEX/'first_vicissitude.png').read_bytes()).decode()}]}
    (ART/'first_vicissitude.bbmodel').write_text(json.dumps(bb,separators=(',',':')))
    report = {'objects':len(meshes),'triangles':triangles,'joints':len(joints),
              'source':'Blender mesh, same geometry exported to runtime and Blockbench',
              'texture_size':[256,256]}
    (PREVIEW/'mesh-report.json').write_text(json.dumps(report,indent=2))
    print('MESH_REPORT',json.dumps(report),flush=True)

if __name__ == '__main__':
    export(Path(__file__).resolve().parents[3])
