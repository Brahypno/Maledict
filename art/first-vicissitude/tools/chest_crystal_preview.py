"""Offline counterpart of the chest RenderLayer; excluded from the body mesh export.

Uses installed dependency textures unchanged. Runtime resolves the original resource
locations, so no external textures are copied into the mod's assets.
"""
import bpy
import math
import re
import zipfile
from pathlib import Path
from mathutils import Matrix, Quaternion, Vector

ROOT=Path(__file__).resolve().parents[3]
BASIS=Matrix(((1,0,0),(0,0,1),(0,-1,0)))


def add_preview():
    for obj in list(bpy.data.objects):
        if obj.get('render_layer_preview'): bpy.data.objects.remove(obj,do_unlink=True)
    source=(ROOT/'src/main/java/org/brahypno/maledict/client/FirstVicissitudeChestCrystalLayer.java').read_text()
    scale=float(re.search(r'SCALE = ([\d.]+)F',source)[1])
    depth=float(re.search(r'DEPTH = (-?[\d.]+)F',source)[1])
    vertical=float(re.search(r'VERTICAL_OFFSET = (-?[\d.]+)F',source)[1])
    cache=Path.home()/'.gradle/caches'
    jars=[cache/'forge_gradle/minecraft_repo/versions/1.20.1/client.jar',
          next((cache/'modules-2/files-2.1/curse.maven/malum-484064/6646111').glob('*/*.jar'))]
    resources=['assets/minecraft/textures/entity/end_crystal/end_crystal.png',
               'assets/malum/textures/block/storage_blocks/block_of_cthonic_gold.png']
    folder=ROOT/'build/chest-crystal-review'
    folder.mkdir(parents=True,exist_ok=True)
    materials=[]
    for jar,resource in zip(jars,resources):
        target=folder/Path(resource).name
        with zipfile.ZipFile(jar) as archive: target.write_bytes(archive.read(resource))
        image=bpy.data.images.load(str(target),check_existing=True)
        image.pack()
        material=bpy.data.materials.new('Chest crystal '+target.stem)
        material.use_nodes=True
        nodes=material.node_tree.nodes
        shader=nodes.get('Principled BSDF')
        shader.inputs['Roughness'].default_value=.72
        texture=nodes.new('ShaderNodeTexImage')
        texture.image=image
        texture.interpolation='Closest'
        material.node_tree.links.new(texture.outputs['Color'],shader.inputs['Base Color'])
        threshold=nodes.new('ShaderNodeMath')
        threshold.operation='GREATER_THAN'
        threshold.inputs[1].default_value=.1
        material.node_tree.links.new(texture.outputs['Alpha'],threshold.inputs[0])
        material.node_tree.links.new(threshold.outputs[0],shader.inputs['Alpha'])
        materials.append(material)
    verts=[(-4,-4,-4),(4,-4,-4),(4,4,-4),(-4,4,-4),
           (-4,-4,4),(4,-4,4),(4,4,4),(-4,4,4)]
    faces=[(5,4,0,1),(2,3,7,6),(0,4,7,3),(1,0,3,2),(5,1,2,6),(4,5,6,7)]
    rectangles=[(8,0,16,8),(16,8,24,0),(0,8,8,16),(8,8,16,16),(16,8,24,16),(24,8,32,16)]
    parts=[]
    for level in range(3):
        data=bpy.data.meshes.new('Chest crystal layer '+str(level))
        data.from_pydata([BASIS@Vector(v)*(scale*.875**level) for v in verts],[],faces)
        data.update()
        obj=bpy.data.objects.new(data.name,data)
        bpy.context.collection.objects.link(obj)
        obj.parent=bpy.data.objects['torso']
        obj.location=BASIS@Vector((0,1+vertical,depth))
        obj['render_layer_preview']=True
        data.materials.append(materials[0 if level<2 else 1])
        uv=data.uv_layers.new(name='Original resource UV')
        for poly,rect in zip(data.polygons,rectangles):
            x0,y0,x1,y1=rect if level<2 else (0,0,16,16)
            w,h=(64,32) if level<2 else (16,16)
            for loop,(u,v) in zip(poly.loop_indices,[(x1,y0),(x0,y0),(x0,y1),(x1,y1)]):
                uv.data[loop].uv=(u/w,1-v/h)
        parts.append(obj)
    tilt=Quaternion(Vector((math.sqrt(.5),0,math.sqrt(.5))),math.pi/3).to_matrix()
    for frame in range(1,82):
        turn=Matrix.Rotation(math.radians((frame-1)*3),3,'Y')
        rotation=turn@tilt
        for i,obj in enumerate(parts):
            if i: rotation=rotation@tilt@turn
            obj.rotation_mode='QUATERNION'
            obj.rotation_quaternion=(BASIS@rotation@BASIS.transposed()).to_quaternion()
            obj.keyframe_insert('rotation_quaternion',frame=frame)
    bpy.context.scene.frame_set(1)
    bpy.context.view_layer.update()
    return parts


if __name__=='__main__':
    add_preview()
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'art/first-vicissitude/first_vicissitude.blend'))
