import json
import os
import re
import shutil
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "misanthropy", "slabs", "Slabs.java")
OUT = os.path.join(ROOT, "src", "generated", "resources")
JAR = os.path.expanduser(r"~\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar")

jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return json.loads(jar.read(path))


def lists():
    src = open(JAVA).read()
    def names(field):
        body = re.search(r"List<String> %s = List\.of\((.*?)\);" % field, src, re.S).group(1)
        return re.findall(r'"([a-z_]+)"', body)
    return names("SNOWY"), names("PLAIN")


def slab(source):
    return source.replace("_block", "") + "_slab"


def textures(model):
    tex = {}
    while model:
        m = vanilla("assets/minecraft/models/%s.json" % model.split(":")[-1])
        for k, v in m.get("textures", {}).items():
            tex.setdefault(k, v)
        model = m.get("parent")
    if "all" in tex:
        return {"bottom": tex["all"], "top": tex["all"], "side": tex["all"]}
    if "end" in tex:
        return {"bottom": tex["end"], "top": tex["end"], "side": tex["side"]}
    return {"bottom": tex["bottom"], "top": tex["top"], "side": tex["side"]}


def default_variants(source):
    variants = vanilla("assets/minecraft/blockstates/%s.json" % source)["variants"]
    for key in ("", "axis=y", "snowy=false"):
        if key in variants:
            v = variants[key]
            return v if isinstance(v, list) else [v]
    raise KeyError(source)


def spun(variants, model):
    seen = []
    for v in variants:
        y = v.get("y", 0) if "x" not in v else 0
        if y not in seen:
            seen.append(y)
    return [dict({"model": model}, **({"y": y} if y else {})) for y in seen]


def write(path, data):
    path = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def fringe(name, tex, top, tint):
    lo, hi = (8, 16) if top else (0, 8)
    def face(t, uv, cull, tinted):
        f = {"uv": uv, "texture": t}
        if cull:
            f["cullface"] = cull
        if tinted and tint:
            f["tintindex"] = 0
        return f
    sides = {d: face("#side", [0, 0, 16, 8], d, False) for d in ("north", "south", "west", "east")}
    base = {"from": [0, lo, 0], "to": [16, hi, 16], "faces": dict(
        down=face("#bottom", [0, 0, 16, 16], None if top else "down", False),
        up=face("#top", [0, 0, 16, 16], "up" if top else None, True), **sides)}
    elements = [base]
    if tint:
        elements.append({"from": [0, lo, 0], "to": [16, hi, 16], "faces": {
            d: face("#overlay", [0, 0, 16, 8], d, True) for d in ("north", "south", "west", "east")}})
    model = {"parent": "minecraft:block/block", "textures": dict(tex, particle=tex["bottom"] if tint else tex["side"]), "elements": elements}
    if tint:
        model["render_type"] = "minecraft:cutout_mipped"
    write("assets/slabs/models/block/%s.json" % name, model)


def plain_models(id, tex):
    write("assets/slabs/models/block/%s.json" % id, {"parent": "minecraft:block/slab", "textures": tex})
    write("assets/slabs/models/block/%s_top.json" % id, {"parent": "minecraft:block/slab_top", "textures": tex})


def snowy_models(source, id):
    tex = textures("minecraft:block/" + source)
    grass = source == "grass_block"
    if grass:
        tex = {"bottom": "minecraft:block/dirt", "top": "minecraft:block/grass_block_top",
               "side": "minecraft:block/grass_block_side", "overlay": "minecraft:block/grass_block_side_overlay"}
    fringe(id, tex, False, grass)
    fringe(id + "_top", tex, True, grass)
    snow = textures("minecraft:block/grass_block_snow")
    fringe(id + "_snow", snow, False, False)
    fringe(id + "_snow_top", snow, True, False)


def loot(id, source, snowy, tags):
    count = [{"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [
        {"condition": "minecraft:block_state_property", "block": "slabs:" + id, "properties": {"type": "double"}}]},
        {"function": "minecraft:explosion_decay"}]
    silk = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
    own = {"type": "minecraft:item", "name": "slabs:" + id, "functions": count}
    if snowy:
        entry = {"type": "minecraft:alternatives", "children": [
            dict(own, conditions=[silk]), {"type": "minecraft:item", "name": "slabs:dirt_slab", "functions": count}]}
    elif source in tags["silk"]:
        entry = dict(own, conditions=[silk])
    else:
        entry = own
    write("data/slabs/loot_tables/blocks/%s.json" % id, {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}]})


def recipes(id, source, stone):
    write("data/slabs/recipes/%s.json" % id, {"type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["###"], "key": {"#": {"item": "minecraft:" + source}}, "result": {"item": "slabs:" + id, "count": 6}})
    if stone:
        write("data/slabs/recipes/%s_from_stonecutting.json" % id, {"type": "minecraft:stonecutting",
            "ingredient": {"item": "minecraft:" + source}, "result": "slabs:" + id, "count": 2})


def main():
    snowy, plain = lists()
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    names = json.loads(jar.read("assets/minecraft/lang/en_us.json"))
    tools = {}
    for tool in ("pickaxe", "shovel", "axe", "hoe"):
        tools[tool] = vanilla("data/minecraft/tags/blocks/mineable/%s.json" % tool)["values"]
    tags = {"silk": {"packed_ice", "blue_ice"}}
    lang, mined = {}, {t: [] for t in tools}
    ids = []
    for source in snowy + plain:
        id = slab(source)
        ids.append("slabs:" + id)
        base = default_variants(source)
        if source in snowy:
            snowy_models(source, id)
            states = {}
            for s, suffix, double in (("false", "", base), ("true", "_snow", [{"model": "minecraft:block/grass_block_snow"}])):
                states["type=bottom,snowy=" + s] = spun(base, "slabs:block/%s%s" % (id, suffix))
                states["type=top,snowy=" + s] = spun(base, "slabs:block/%s%s_top" % (id, suffix))
                states["type=double,snowy=" + s] = double
        else:
            plain_models(id, textures(base[0]["model"]))
            states = {"type=bottom": spun(base, "slabs:block/" + id), "type=top": spun(base, "slabs:block/%s_top" % id),
                      "type=double": base}
        write("assets/slabs/blockstates/%s.json" % id, {"variants": states})
        write("assets/slabs/models/item/%s.json" % id, {"parent": "slabs:block/" + id})
        loot(id, source, source in snowy, tags)
        stone = False
        for tool, values in tools.items():
            if "minecraft:" + source in values:
                mined[tool].append("slabs:" + id)
                stone = stone or tool == "pickaxe"
        recipes(id, source, stone)
        lang["block.slabs." + id] = names["block.minecraft." + source].replace(" Block", "") + " Slab"
    for tool, values in mined.items():
        if values:
            write("data/minecraft/tags/blocks/mineable/%s.json" % tool, {"replace": False, "values": values})
    write("data/minecraft/tags/blocks/slabs.json", {"replace": False, "values": ids})
    write("data/minecraft/tags/items/slabs.json", {"replace": False, "values": ids})
    write("assets/slabs/lang/en_us.json", lang)
    write("data/slabs/worldgen/configured_feature/lips.json", {"type": "slabs:lips", "config": {}})
    write("data/slabs/worldgen/placed_feature/lips.json", {"feature": "slabs:lips", "placement": []})
    write("data/slabs/forge/biome_modifier/lips.json", {"type": "forge:add_features", "biomes": "#minecraft:is_overworld",
        "features": "slabs:lips", "step": "top_layer_modification"})
    print(len(ids), "slabs", {t: len(v) for t, v in mined.items()})


if __name__ == "__main__":
    main()
