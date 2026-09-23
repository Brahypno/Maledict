"""Original pixel plans for four primary vanes; coordinates follow each UV strip.

Ridge stations describe a tapering bone inlay, not extra mesh relief.
Cracks and shallow branches are intentionally sparse and individually placed.
"""
PALETTE = ('282334','403348','58415f','75547c','96749b',
           '74798e','9da9b9','c7d1da','e5e8e6')
PLANS = {
    'wing': dict(ridge=((0,5),(9,5),(19,3),(32,4),(49,2),(65,2),(79,1)),
                 seams=(((16,11),(20,9),(25,8),(29,5)),
                        ((45,11),(48,9),(54,8),(59,5))),
                 ribs=(((8,3),(13,5),(17,7)),((29,3),(35,5),(42,7)),
                       ((52,2),(58,4),(65,5)))),
    'wing_middle': dict(ridge=((0,4),(12,5),(25,3),(41,4),(56,2),(70,2),(79,1)),
                        seams=(((25,11),(29,9),(36,8),(41,6)),
                               ((59,11),(61,9),(67,7))),
                        ribs=(((10,3),(16,5),(23,7)),((37,3),(43,5),(49,7)),
                              ((59,2),(65,4),(70,5)))),
    'wing_low': dict(ridge=((0,5),(10,5),(28,3),(44,4),(60,2),(77,2),(87,1)),
                     seams=(((32,11),(37,9),(45,8),(50,6)),
                            ((67,11),(69,9),(76,7))),
                     ribs=(((9,3),(15,5),(22,7)),((36,3),(42,5),(49,7)),
                           ((62,2),(69,4),(76,5)))),
    'wing_trailing': dict(ridge=((0,4),(9,4),(22,2),(34,3),(48,2),(63,1)),
                          seams=(((19,9),(23,7),(29,6),(33,4)),
                                 ((45,9),(48,7),(53,6))),
                          ribs=(((7,2),(12,4),(18,6)),((30,2),(36,4),(42,5)))),
}

def path_pixels(points):
    pixels = set()
    for (x0,y0),(x1,y1) in zip(points,points[1:]):
        steps=max(abs(x1-x0),abs(y1-y0))
        for i in range(steps+1):
            t=i/max(steps,1)
            pixels.add((round(x0+(x1-x0)*t),round(y0+(y1-y0)*t)))
    return pixels

def primary_pixels(role, face, width, height):
    plan=PLANS[role]
    native_height=10 if role=='wing_trailing' else 12
    cracks=set().union(*(path_pixels(p) for p in plan['seams']))
    ribs=set().union(*(path_pixels(p) for p in plan['ribs']))
    pixels=[]
    for y in range(height):
        for x in range(width):
            if face.startswith('cap'):
                edge=min(x,y,width-1-x,height-1-y)
                color=(5,2,1)[min(edge,2)] if face=='cap0' else (7,5,2)[min(edge,2)]
            else:
                yy=round(y*(native_height-1)/max(height-1,1))
                ridge=1
                for (a,ra),(b,rb) in zip(plan['ridge'],plan['ridge'][1:]):
                    if a<=x<=b:
                        ridge=round(ra+(rb-ra)*(x-a)/(b-a))
                        break
                # Broad bone insertion narrowing into a violet mineral blade.
                color=3 if yy<8 else 2
                if yy==native_height-1: color=1
                if yy<ridge: color=7 if yy else 6
                if yy==max(0,ridge-1): color=8
                if yy==ridge: color=1
                if yy==ridge+1: color=4
                if (x,yy) in ribs and yy>ridge: color=5
                elif (x,yy-1) in ribs and yy>ridge+1: color=1
                if (x,yy) in cracks: color=0
                elif (x,yy+1) in cracks: color=4
                # Root socket/contact occlusion fades into the load-bearing plane.
                if x<3: color=1 if yy>=ridge else 5
                elif x<7 and color>=6: color-=1
                # Small, placed interruptions in the pale edge, not global noise.
                if (x,yy) in ((12,0),(13,0),(38,1),(39,1),(57,0)): color=5
                if face=='back':
                    color={8:6,7:5,6:5,5:3,4:3,3:2,2:1}.get(color,color)
                elif face=='inner':
                    color={8:7,7:6,6:5,5:3,4:3,3:2}.get(color,color)
            pixels.append(PALETTE[color])
    return pixels
