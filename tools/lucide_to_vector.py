#!/usr/bin/env python3
"""Convert Lucide SVG icons (24x24, stroke-based) to Android vector drawables.

Usage:
    python3 tools/lucide_to_vector.py <input.svg> <output.xml>

Reads <path>/<circle>/<ellipse>/<line>/<rect>/<polygon>/<polyline>
elements and writes a <vector> with viewportWidth/Height 24,
strokeWidth 2, strokeColor ?attr/colorOnSurface and fillColor
@android:color/transparent. Lucide icons are ISC licensed
(https://github.com/lucide-icons/lucide/blob/main/LICENSE).
"""
import sys
import xml.etree.ElementTree as ET

NS = "{http://www.w3.org/2000/svg}"


def _num(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def circle_to_path(el):
    cx, cy, r = _num(el.get("cx")), _num(el.get("cy")), _num(el.get("r"))
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0"


def ellipse_to_path(el):
    cx, cy = _num(el.get("cx")), _num(el.get("cy"))
    rx, ry = _num(el.get("rx")), _num(el.get("ry"))
    return f"M{cx - rx},{cy} a{rx},{ry} 0 1,0 {2 * rx},0 a{rx},{ry} 0 1,0 {-2 * rx},0"


def line_to_path(el):
    return (f"M{_num(el.get('x1'))},{_num(el.get('y1'))} "
            f"L{_num(el.get('x2'))},{_num(el.get('y2'))}")


def rect_to_path(el):
    x, y = _num(el.get("x")), _num(el.get("y"))
    w, h = _num(el.get("width")), _num(el.get("height"))
    return f"M{x},{y} h{w} v{h} h{-w} Z"


def points_to_path(el, close):
    pts = el.get("points", "").replace(",", " ").split()
    coords = [float(p) for p in pts]
    d = ""
    for i in range(0, len(coords) - 1, 2):
        d += ("M" if i == 0 else " L") + f"{coords[i]},{coords[i + 1]}"
    return d + (" Z" if close else "")


def convert(svg_path):
    tree = ET.parse(svg_path)
    root = tree.getroot()
    paths = []
    for el in root:
        tag = el.tag.replace(NS, "")
        if tag == "path":
            paths.append(el.get("d", ""))
        elif tag == "circle":
            paths.append(circle_to_path(el))
        elif tag == "ellipse":
            paths.append(ellipse_to_path(el))
        elif tag == "line":
            paths.append(line_to_path(el))
        elif tag == "rect":
            paths.append(rect_to_path(el))
        elif tag == "polygon":
            paths.append(points_to_path(el, close=True))
        elif tag == "polyline":
            paths.append(points_to_path(el, close=False))
        else:
            print(f"WARNING: unsupported element <{tag}> skipped in {svg_path}",
                  file=sys.stderr)
    if not paths:
        raise ValueError(f"no drawable paths found in {svg_path}")
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<!-- Lucide icon (ISC license) converted from SVG: '
        'stroke-based, 24x24 viewport. Source: '
        'https://github.com/lucide-icons/lucide -->',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="24dp"',
        '    android:height="24dp"',
        '    android:viewportWidth="24"',
        '    android:viewportHeight="24">',
    ]
    for d in paths:
        lines.append('    <path')
        lines.append(f'        android:pathData="{d}"')
        lines.append('        android:fillColor="@android:color/transparent"')
        lines.append('        android:strokeColor="?attr/colorOnSurface"')
        lines.append('        android:strokeWidth="2"')
        lines.append('        android:strokeLineCap="round"')
        lines.append('        android:strokeLineJoin="round" />')
    lines.append('</vector>')
    return "\n".join(lines) + "\n"


def main():
    if len(sys.argv) != 3:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    out = convert(sys.argv[1])
    with open(sys.argv[2], "w", encoding="utf-8") as f:
        f.write(out)
    print(f"wrote {sys.argv[2]}")


if __name__ == "__main__":
    main()
