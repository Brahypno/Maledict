"""Keep the public preview to four named images; diagnostics belong in build."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
PUBLIC = ROOT / 'art/first-vicissitude/preview'
DIAGNOSTICS = ROOT / 'build/first-vicissitude-review'


def image_target(name, comparison=None):
    public_name = {
        ('phase_one_hero', None): '01-phase-one.png',
        ('phase_two_hero', None): '02-phase-two.png',
        ('hook_close', 'before'): '03-lower-before.png',
        ('hook_close', 'after'): '04-lower-after.png',
    }.get((name, comparison))
    if public_name:
        target = PUBLIC / public_name
    else:
        folder = DIAGNOSTICS / 'comparison' / comparison if comparison else DIAGNOSTICS
        target = folder / (name + '.png')
    target.parent.mkdir(parents=True, exist_ok=True)
    return target
