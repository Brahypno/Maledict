"""Extend phase-two bone blades while preserving their shoulder attachment and original UVs."""
from pathlib import Path
import json
import shutil
import sys
sys.dont_write_bytecode = True

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

BONE_REACH_SCALE = 1.2


def expanded_local(point):
    """Blender-local X/Z are the wing plane; leave the proximal eight units and depth intact."""
    point = Vector(point)
    t = max(0.0, min(1.0, (abs(point.x) - 8.0) / 16.0))
    scale = 1.0 + (BONE_REACH_SCALE - 1.0) * t * t * (3.0 - 2.0 * t)
    return Vector((point.x * scale, point.y, point.z * scale))


def render(scene, path, position=(65, -190, 48), scale=220, frame=41):
    scene.frame_set(frame)
    for obj in scene.objects:
        if obj.type == 'MESH' and 'deploy_scale' in obj:
            obj.hide_render = frame == 1
    camera = scene.camera
    camera.location = position
    camera.rotation_euler = (Vector((0, 0, 10)) - camera.location).to_track_quat('-Z', 'Y').to_euler()
    camera.data.ortho_scale = scale
    scene.render.filepath = str(path)
    bpy.ops.render.render(write_still=True)


def head_overlaps(scene, blades):
    """Compare exact posed triangle meshes against the head and halo, rather than broad boxes."""
    scene.frame_set(41)
    bpy.context.view_layer.update()
    objects = [obj for obj in scene.objects if obj.type == 'MESH' and
               str(obj.get('runtime_joint', '')).startswith(('head_', 'halo_'))]
    def tree(obj):
        return BVHTree.FromPolygons([obj.matrix_world @ vertex.co for vertex in obj.data.vertices],
                                   [tuple(poly.vertices) for poly in obj.data.polygons])
    heads = [(obj.name, tree(obj)) for obj in objects]
    return sorted((blade.name, name) for blade in blades for name, head in heads
                  if tree(blade).overlap(head))


def main():
    root = Path(__file__).resolve().parents[3]
    art = root / 'art/first-vicissitude'
    out = root / 'build/first-vicissitude-review/bone-wing-size'
    out.mkdir(parents=True, exist_ok=True)
    public = art / 'preview'
    source = art / 'first_vicissitude.blend'
    scene = bpy.context.scene
    blades = [obj for obj in scene.objects if obj.type == 'MESH' and 'deploy_scale' in obj]
    if any(obj.get('bone_reach_scale', 1.0) != 1.0 for obj in blades):
        raise RuntimeError('This saved model already has the bone-wing enlargement applied')
    shutil.copy2(source, out / 'before.blend')
    shutil.copy2(root / 'src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json',
                 out / 'before.mesh.json')
    scene.render.resolution_x = 900
    scene.render.resolution_y = 900
    scene.render.resolution_percentage = 100
    scene.cycles.samples = 12
    scene.cycles.seed = 17
    scene.cycles.use_denoising = True
    before_overlap = head_overlaps(scene, blades)
    render(scene, public / '03-lower-before.png')
    views = [('front', (0, -210, 10), 220), ('back', (0, 210, 10), 220),
             ('side', (210, 0, 10), 110)]
    for name, position, scale in views:
        render(scene, out / ('before-' + name + '.png'), position, scale)
    for obj in blades:
        for vertex in obj.data.vertices:
            vertex.co = expanded_local(vertex.co)
        obj.data.update()
        obj['bone_reach_scale'] = BONE_REACH_SCALE
        for frame in (1, 41, 81):
            obj.hide_render = frame == 1
            obj.hide_viewport = frame == 1
            obj.keyframe_insert('hide_render', frame=frame)
            obj.keyframe_insert('hide_viewport', frame=frame)
    after_overlap = head_overlaps(scene, blades)
    if set(after_overlap) - set(before_overlap):
        raise RuntimeError('Enlargement introduced a head/halo intersection: ' + str(after_overlap))
    sys.path.insert(0, str(Path(__file__).parent))
    from export_blender import export
    export(root)
    scene.frame_set(1)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.wm.save_as_mainfile(filepath=str(source))
    render(scene, public / '04-lower-after.png')
    shutil.copy2(public / '04-lower-after.png', public / '02-phase-two.png')
    render(scene, public / '01-phase-one.png', scale=175, frame=1)
    for name, position, scale in views:
        render(scene, out / ('after-' + name + '.png'), position, scale)
    # Texture-only checks retain the same camera and original pixel artwork.
    for material in bpy.data.materials:
        if not material.use_nodes:
            continue
        nodes = material.node_tree.nodes
        texture = next((node for node in nodes if node.type == 'TEX_IMAGE' and node.image
                        and 'emissive' not in node.image.name), None)
        if texture is None:
            continue
        output = next(node for node in nodes if node.type == 'OUTPUT_MATERIAL')
        emission = nodes.new('ShaderNodeEmission')
        material.node_tree.links.new(texture.outputs['Color'], emission.inputs['Color'])
        material.node_tree.links.new(emission.outputs[0], output.inputs['Surface'])
    scene.view_settings.view_transform = 'Standard'
    render(scene, out / 'after-unlit.png', (0, -210, 10), 220)
    (out / 'comparison.json').write_text(json.dumps({
        'distal_reach_scale': BONE_REACH_SCALE, 'proximal_units_unchanged': 8,
        'blade_meshes': len(blades), 'head_halo_pairs_before': before_overlap,
        'head_halo_pairs_after': after_overlap, 'preview_is_offline': True}, indent=2))
    print('BONE_WING_SIZE', BONE_REACH_SCALE, 'meshes', len(blades), 'head/halo overlaps', after_overlap, flush=True)


if __name__ == '__main__':
    main()
