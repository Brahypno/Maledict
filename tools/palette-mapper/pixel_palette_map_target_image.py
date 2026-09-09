from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image


RGB = tuple[int, int, int]
RGBA = tuple[int, int, int, int]
MAX_RGB_DISTANCE = math.sqrt(3 * 255**2)


def parse_hex_color(text: str) -> RGB:
    value = text.strip().removeprefix("#")
    if len(value) == 3:
        value = "".join(character * 2 for character in value)
    if len(value) != 6:
        raise argparse.ArgumentTypeError("HEX must be #RGB or #RRGGBB")
    try:
        return tuple(int(value[index : index + 2], 16) for index in (0, 2, 4))
    except ValueError as error:
        raise argparse.ArgumentTypeError(
            "HEX contains a non-hexadecimal character"
        ) from error


def parse_percentage(text: str) -> float:
    value = text.strip().removesuffix("%")
    try:
        percentage = float(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError(
            "percentage must be a number such as 15 or 15%"
        ) from error
    if not 0 <= percentage <= 100:
        raise argparse.ArgumentTypeError("percentage must be between 0 and 100")
    return percentage


def rgb_to_hex(rgb: RGB) -> str:
    return "#" + "".join(f"{channel:02X}" for channel in rgb)


def rgb_distance(left: RGB, right: RGB) -> float:
    """Return ordinary Euclidean distance in the 8-bit RGB cube."""
    return math.sqrt(sum((a - b) ** 2 for a, b in zip(left, right, strict=True)))


def normalized_rgb_distance_percent(left: RGB, right: RGB) -> float:
    """Return RGB distance as a deterministic percentage of the RGB-cube diagonal."""
    return rgb_distance(left, right) / MAX_RGB_DISTANCE * 100.0


def color_matches(pixel_rgb: RGB, center_rgb: RGB, color_range: float) -> bool:
    return normalized_rgb_distance_percent(pixel_rgb, center_rgb) <= color_range


def srgb_to_linear(channel: int) -> float:
    value = channel / 255.0
    if value <= 0.04045:
        return value / 12.92
    return ((value + 0.055) / 1.055) ** 2.4


def perceptual_lightness(rgb: RGB) -> float:
    """Return CIE L* derived from the color's relative luminance."""
    red, green, blue = (srgb_to_linear(channel) for channel in rgb)
    luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    epsilon = 216 / 24389
    kappa = 24389 / 27
    f_y = (
        luminance ** (1 / 3)
        if luminance > epsilon
        else (kappa * luminance + 16) / 116
    )
    return 116 * f_y - 16


def collect_matching_palette(
    image: Image.Image,
    center_rgb: RGB,
    color_range: float,
) -> tuple[set[RGB], int]:
    """Inspect each coordinate and collect every exact RGB found inside the range."""
    palette: set[RGB] = set()
    matched_pixels = 0

    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = image.getpixel((x, y))
            pixel_rgb = (red, green, blue)
            if alpha > 0 and color_matches(pixel_rgb, center_rgb, color_range):
                palette.add(pixel_rgb)
                matched_pixels += 1

    return palette, matched_pixels


def create_tone_mapping(
    source_palette: set[RGB],
    target_palette: set[RGB],
) -> dict[RGB, RGB]:
    """Map source tones to actual target-image colors in perceptual-lightness order."""
    if not source_palette:
        raise ValueError("no visible pixels fall inside the requested source-color range")
    if not target_palette:
        raise ValueError("no visible pixels fall inside the requested target-color range")

    ordered_source = sorted(source_palette, key=perceptual_lightness)
    ordered_target = sorted(target_palette, key=perceptual_lightness)

    mapping: dict[RGB, RGB] = {}
    source_steps = len(ordered_source)
    target_steps = len(ordered_target)
    for source_index, source_color in enumerate(ordered_source):
        if source_steps == 1:
            target_index = (target_steps - 1) // 2
        else:
            position = source_index / (source_steps - 1)
            target_index = round(position * (target_steps - 1))
        mapping[source_color] = ordered_target[target_index]

    return mapping


def default_output_path(
    source_image_path: Path,
    source_rgb: RGB,
    source_range: float,
    target_rgb: RGB,
    target_range: float,
) -> Path:
    source_text = rgb_to_hex(source_rgb)[1:].lower()
    target_text = rgb_to_hex(target_rgb)[1:].lower()
    source_range_text = f"{source_range:g}".replace(".", "_")
    target_range_text = f"{target_range:g}".replace(".", "_")
    name = (
        f"{source_image_path.stem}_from_{source_text}_r{source_range_text}"
        f"_to_{target_text}_r{target_range_text}.png"
    )
    return source_image_path.with_name(name)


def recolor_pixel_by_pixel(
    source_image: Image.Image,
    output_path: Path,
    mapping: dict[RGB, RGB],
) -> tuple[int, int, tuple[int, int]]:
    if output_path.suffix.lower() != ".png":
        raise ValueError("output must use the lossless .png format")

    output_image = Image.new("RGBA", source_image.size)
    replaced = 0
    unchanged = 0

    # Strict processing: read and write exactly one coordinate at a time.
    for y in range(source_image.height):
        for x in range(source_image.width):
            source_pixel: RGBA = source_image.getpixel((x, y))
            red, green, blue, alpha = source_pixel
            replacement = mapping.get((red, green, blue)) if alpha > 0 else None

            if replacement is None:
                output_pixel = source_pixel
                unchanged += 1
            else:
                output_pixel = (*replacement, alpha)
                replaced += 1

            output_image.putpixel((x, y), output_pixel)

            if output_image.getpixel((x, y)) != output_pixel:
                raise RuntimeError(f"pixel verification failed at ({x}, {y})")

    if replaced == 0:
        raise ValueError("no pixels were replaced")

    output_image.save(output_path, format="PNG", optimize=False)
    return replaced, unchanged, source_image.size


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Strictly replace a configurable color range in a source image, using "
            "only colors found inside a configurable range in a target image. Both "
            "ranges are normalized Euclidean RGB distance percentages."
        )
    )
    parser.add_argument("source_image", type=Path, help="image to recolor")
    parser.add_argument(
        "target_image",
        type=Path,
        help="reference image from which replacement colors are sampled",
    )
    parser.add_argument(
        "--source",
        required=True,
        type=parse_hex_color,
        help='center HEX of the colors to replace, e.g. "#80B98A"',
    )
    parser.add_argument(
        "--source-range",
        required=True,
        type=parse_percentage,
        metavar="PERCENT",
        help="maximum normalized RGB distance from --source; 0 means exact HEX only",
    )
    parser.add_argument(
        "--target",
        required=True,
        type=parse_hex_color,
        help='center HEX of colors to sample in the target image, e.g. "#594353"',
    )
    parser.add_argument(
        "--target-range",
        required=True,
        type=parse_percentage,
        metavar="PERCENT",
        help="maximum normalized RGB distance from --target; 0 means exact HEX only",
    )
    parser.add_argument("-o", "--output", type=Path, help="output PNG path")
    return parser


def main() -> None:
    arguments = build_parser().parse_args()
    source_image = Image.open(arguments.source_image).convert("RGBA")
    target_image = Image.open(arguments.target_image).convert("RGBA")
    source_palette, matched_pixels = collect_matching_palette(
        source_image,
        arguments.source,
        arguments.source_range,
    )
    target_palette, target_matched_pixels = collect_matching_palette(
        target_image,
        arguments.target,
        arguments.target_range,
    )
    mapping = create_tone_mapping(source_palette, target_palette)
    output_path = arguments.output or default_output_path(
        arguments.source_image,
        arguments.source,
        arguments.source_range,
        arguments.target,
        arguments.target_range,
    )
    replaced, unchanged, size = recolor_pixel_by_pixel(
        source_image,
        output_path,
        mapping,
    )

    source_radius = arguments.source_range / 100.0 * MAX_RGB_DISTANCE
    target_radius = arguments.target_range / 100.0 * MAX_RGB_DISTANCE
    print(f"Source center:       {rgb_to_hex(arguments.source)}")
    print(
        f"Source range:        {arguments.source_range:g}% "
        f"(RGB distance <= {source_radius:.3f})"
    )
    print(f"Source colors found: {len(source_palette)} exact RGB values")
    print(f"Source pixels found: {matched_pixels}")
    print(f"Target center:       {rgb_to_hex(arguments.target)}")
    print(
        f"Target range:        {arguments.target_range:g}% "
        f"(RGB distance <= {target_radius:.3f})"
    )
    print(f"Target colors found: {len(target_palette)} exact RGB values")
    print(f"Target pixels found: {target_matched_pixels}")
    print(f"Target colors used:  {len(set(mapping.values()))}")
    print("Exact source -> target-image mapping:")
    for source_color in sorted(mapping, key=perceptual_lightness):
        distance = normalized_rgb_distance_percent(source_color, arguments.source)
        print(
            f"  {rgb_to_hex(source_color)} -> {rgb_to_hex(mapping[source_color])} "
            f"(source distance {distance:.3f}%)"
        )
    print(f"Processed:           {size[0] * size[1]} pixels ({size[0]}x{size[1]})")
    print(f"Replaced:            {replaced}")
    print(f"Unchanged:           {unchanged}")
    print(f"Saved:               {output_path}")


if __name__ == "__main__":
    main()

