# -*- coding: utf-8 -*-
"""生成 14 张技能卡的 16x16 占位贴图与物品模型 JSON（纯 zlib 写 PNG，无需 PIL）。"""
import json
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "skillcards")

CARDS = [
    # (id, 稀有度色RGB, 中心图案编号)
    ("xiangyu",  (0xB9, 0xE8, 0xC9), 0),   # 普通卡 绿玉
    ("shamu",    (0x9A, 0x9A, 0xA8), 1),   # 普通卡 黑雾
    ("zhanfang", (0xF3, 0xC5, 0xDD), 2),   # 普通卡 粉花
    ("shenfa",   (0xE8, 0xB4, 0xB4), 3),   # 普通卡 红十字
    ("hongxing", (0xA8, 0xC8, 0xF0), 4),   # 普通卡 蓝星
    ("dafuni",   (0xF0, 0xE6, 0xE6), 5),   # 稀有卡 兔耳
    ("qilin",    (0xBFD, 0xE8, 0xF8), 6),  # 稀有卡 雷电
    ("sanshi",   (0xC9, 0xA8, 0xE0), 7),   # 稀有卡 传送门
    ("mangdian", (0xE0, 0xE0, 0xD8), 8),   # 稀有卡 隐形
    ("haiwang",  (0x9FD, 0xDC, 0xF8), 9),  # 彩卡 翼
    ("yasiti",   (0xC0, 0xA0, 0xF0), 10),  # 彩卡 漩涡
    ("zuzhou",   (0x88, 0x68, 0x98), 11),  # 黑卡 诅咒火
    ("shengshu", (0xC8, 0xE0, 0xA0), 12),  # 黑卡 圣树
    ("chilin",   (0xE0, 0x9A, 0x9A), 13),  # 黑卡 赤鳞
]

RARITY_BORDER = {
    "common": (0x6E, 0x7A, 0x6E),
    "rare":   (0x2E, 0x8B, 0x9A),
    "rainbow":(0x8A, 0x2E, 0x8A),
    "black":  (0x40, 0x20, 0x28),
}
GRADES = ["common"] * 5 + ["rare"] * 4 + ["rainbow"] * 2 + ["black"] * 3


def write_png(path, pixels):
    """pixels: 16x16 行列表，每项 (r,g,b,a)。"""
    raw = b""
    for row in pixels:
        raw += b"\x00" + b"".join(struct.pack("4B", *p) for p in row)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def blend(base, accent, t):
    return tuple(int(base[i] * (1 - t) + accent[i] * t) for i in range(3))


def make_card_texture(rgba_accent, pattern):
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    base = (0xD8, 0xCF, 0xBE)  # 羊皮纸底色
    dark = (0x58, 0x50, 0x44)  # 描边
    border = RARITY_BORDER[GRADES[pattern]] if pattern < 14 else dark
    accent = tuple(min(255, c) for c in rgba_accent)

    def inside(x, y):
        return 1 <= x <= 14 and 1 <= y <= 14

    for y in range(16):
        for x in range(16):
            if not (1 <= x <= 14 and 1 <= y <= 14):
                continue  # 边缘透明
            if x in (1, 14) or y in (1, 14):
                px[y][x] = (*border, 255)  # 稀有度边框
                continue
            # 纸底 + 轻微噪点
            n = ((x * 7 + y * 13 + pattern * 5) % 5) - 2
            px[y][x] = (*blend(base, (255, 255, 255), (n + 2) / 12.0), 255)

    def put(x, y, color):
        if inside(x, y):
            px[y][x] = (*color, 255)

    # 内圈细框
    for i in range(3, 14):
        put(i, 3, blend(base, accent, 0.35))
        put(i, 12, blend(base, accent, 0.35))
        put(3, i, blend(base, accent, 0.35))
        put(12, i, blend(base, accent, 0.35))

    # 中心图案（每个卡一个小像素符号）
    a = accent
    c = 7  # 中心
    patterns = {
        0: [(0, -1), (0, 0), (0, 1), (1, 0), (-1, 0), (1, 1), (-1, -1)],          # 响玉：玉点
        1: [(-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (0, 1), (0, -1)],           # 纱幕：横雾
        2: [(0, -2), (-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1), (0, 2)],         # 绽放：花
        3: [(0, -2), (0, -1), (0, 0), (0, 1), (0, 2), (-2, 0), (-1, 0), (1, 0), (2, 0)],  # 神罚：十字
        4: [(0, -2), (-1, -1), (1, -1), (-2, 0), (2, 0), (0, 0), (-1, 1), (1, 1), (0, 2)],# 虹星：星
        5: [(-1, -2), (1, -2), (-1, -1), (1, -1), (0, 0), (0, 1), (-1, 2), (1, 2)],       # 达芙妮：兔耳
        6: [(1, -2), (0, -1), (1, 0), (0, 1), (-1, 1), (-1, 2)],                  # 麒麟：闪电
        7: [(-1, -1), (1, -1), (-1, 1), (1, 1), (0, -2), (0, 2), (-2, 0), (2, 0)],# 三世：传送门
        8: [(0, 0), (-1, -1), (1, 1), (-2, -2), (2, 2)],                          # 盲点：斜线隐身
        9: [(-2, 1), (-1, 0), (0, -1), (1, 0), (2, 1), (0, 0), (0, 1)],           # 海王：翼
        10: [(0, -2), (1, -1), (0, 0), (-1, 1), (0, 2)],                          # 亚丝缇：漩涡
        11: [(0, -1), (0, 0), (-1, 1), (1, 1), (0, 2)],                           # 诅咒：倒焰
        12: [(-1, -1), (1, -1), (0, 0), (-2, 1), (0, 1), (2, 1), (0, 2)],         # 圣树：树
        13: [(-1, -2), (0, -2), (1, -2), (-2, -1), (2, -1), (-1, 0), (0, 0), (1, 0), (-2, 1), (2, 1), (0, 2)],  # 赤鳞：鳞片
    }
    for dx, dy in patterns[pattern]:
        put(c + dx, c + dy, a)

    # 底部稀有度色条
    for i in range(5, 12):
        put(i, 10, blend(base, accent, 0.55))
    return px


def main():
    tex_dir = os.path.join(ASSETS, "textures", "item")
    items_dir = os.path.join(ASSETS, "items")
    models_dir = os.path.join(ASSETS, "models", "item")
    for d in (tex_dir, items_dir, models_dir):
        os.makedirs(d, exist_ok=True)

    for index, (card_id, accent, pattern) in enumerate(CARDS):
        write_png(os.path.join(tex_dir, card_id + ".png"), make_card_texture(accent, index))
        with open(os.path.join(items_dir, card_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"model": {"type": "minecraft:model", "model": "skillcards:item/" + card_id}}, f, indent=2)
        with open(os.path.join(models_dir, card_id + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": "skillcards:item/" + card_id}}, f, indent=2)
        print("generated:", card_id)


if __name__ == "__main__":
    main()
