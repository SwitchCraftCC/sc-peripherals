#!/usr/bin/env zx

// Run with `zx generateTextureMap.mjs <oldDir> <newDir>`
// Specify paths to an extracted Minecraft assets directory:
// 1.12: `assets/minecraft/textures/blocks`
// 1.19: `assets/minecraft/textures/block`

const mapping = {
  "opencomputers:blocks/white": "sc-peripherals:block/white",
};

const manualMapping = {
  "anvil_base": "anvil",
  "anvil_top_damaged_0": "anvil_top",
  "anvil_top_damaged_1": "chipped_anvil_top",
  "anvil_top_damaged_2": "damaged_anvil_top",
  "brick": "bricks",
  "comparator_off": "comparator",
  "deadbush": "dead_bush",
  "dispenser_front_horizontal": "dispenser_front",
  "dropper_front_horizontal": "dropper_front",
  "end_bricks": "end_stone_bricks",
  "farmland_dry": "farmland",
  "farmland_wet": "farmland_moist",
  "flower_houstonia": "azure_bluet",
  "flower_paeonia": "pink_tulip",
  "flower_pot": "flower_pot",
  "flower_rose": "poppy",
  "furnace_front_off": "furnace_front",
  "furnace_front_on": "furnace_front",
  "glass_pane_top": "glass_pane_top",
  "grass_side_overlay": "grass_block_side_overlay",
  "grass_side_snowed": "grass_block_snow",
  "grass_side": "grass_block_side",
  "grass_top": "grass_block_top",
  "hardened_clay": "terracotta",
  "ice_packed": "packed_ice",
  "itemframe_background": "item_frame",
  "mob_spawner": "spawner",
  "mushroom_block_inside": "mushroom_block_inside",
  "mushroom_block_skin_stem": "mushroom_stem",
  "nether_brick": "nether_bricks",
  "noteblock": "note_block",
  "observer_back_lit": "observer_back_on",
  "piston_top_normal": "piston_top",
  "portal": "nether_portal",
  "prismarine_dark": "dark_prismarine",
  "prismarine_rough": "prismarine",
  "pumpkin_face_off": "carved_pumpkin",
  "pumpkin_face_on": "jack_o_lantern",
  "quartz_block_chiseled_top": "chiseled_quartz_block_top",
  "quartz_block_chiseled": "chiseled_quartz_block",
  "quartz_block_lines_top": "quartz_pillar_top",
  "quartz_block_lines": "quartz_pillar",
  "quartz_ore": "nether_quartz_ore",
  "rail_normal_turned": "rail_corner",
  "rail_normal": "rail",
  "red_nether_brick": "red_nether_bricks",
  "red_sandstone_carved": "chiseled_red_sandstone",
  "red_sandstone_normal": "red_sandstone",
  "red_sandstone_smooth": "cut_red_sandstone",
  "redstone_lamp_off": "redstone_lamp",
  "redstone_torch_on": "redstone_torch",
  "reeds": "sugar_cane",
  "repeater_off": "repeater",
  "sandstone_carved": "chiseled_sandstone",
  "sandstone_normal": "sandstone",
  "sandstone_smooth": "cut_sandstone",
  "slime": "slime_block",
  "sponge_wet": "wet_sponge",
  "stone_slab_side": "smooth_stone_slab_side",
  "stone_slab_top": "smooth_stone",
  "stonebrick": "stone_bricks",
  "tallgrass": "grass",
  "torch_on": "torch",
  "trapdoor": "oak_trapdoor",
  "trip_wire_source": "tripwire_hook",
  "trip_wire": "tripwire",
  "waterlily": "lily_pad",
  "web": "cobweb",
};

// Regular expressions for bulk mappings. A little bit order sensitive, be
// careful
const regexes = [
  [/silver/g, "light_gray"],
  [/(?:big|roofed)_oak/g, "dark_oak"],
  [/carved/g, "chiseled"],
  [/^(concrete(?:_powder)?)_(\w+)$/, "$2_$1"],
  [/^glazed_terracotta_(\w+)$/, "$1_glazed_terracotta"],
  [/^hardened_clay_stained_(\w+)$/, "$1_terracotta"],
  [/^shulker_top_(\w+)$/, "$1_shulker_box"],
  [/^wool_colored_(\w+)$/, "$1_wool"],
  [/^glass_pane_top_(\w+)$/, "$1_stained_glass_pane_top"],
  [/^glass_(\w+)$/, "$1_stained_glass"],
  [/^door_wood(\w+)$/, "door_oak$1"],
  [/^door_(\w+)_upper$/, "$1_door_top"],
  [/^door_(\w+)_lower$/, "$1_door_bottom"],
  [/^log_(\w+)_top$/, "$1_log_top"],
  [/^log_(\w+)$/, "$1_log"],
  [/^planks_(\w+)$/, "$1_planks"],
  [/^leaves_(\w+)$/, "$1_leaves"],
  [/^(\w+)_stage_(\d+)$/, "$1_stage$2"],
  [/^destroy_stage(\d+)$/, "destroy_stage_$1"],
  [/^stonebrick_(\w+)$/, "$1_stone_bricks"],
  [/^(\w+)_mossy$/, "mossy_$1"],
  [/^stone_(\w+)$/, "$1"],
  [/^(\w+)_smooth$/, "polished_$1"],
  [/^sapling_(\w+)$/, "$1_sapling"],
  [/^dirt_podzol_(\w+)$/, "podzol_$1"],
  [/^endframe_(\w+)$/, "end_portal_frame_$1"],
  [/^fire_layer_(\w+)$/, "fire_$1"],
  [/^flower_tulip_(\w+)$/, "$1_tulip"],
  [/^flower_(\w+)$/, "$1"],
  [/^grass_path_(\w+)$/, "dirt_path_$1"],
  [/^(\w+)_stem_connected$/, "attached_$1_stem"],
  [/^(\w+)_stem_disconnected$/, "$1_stem"],
  [/^mushroom_block_skin_(\w+)$/, "$1_mushroom_block"],
  [/^mushroom_(\w+)$/, "$1_mushroom"],
  [/^rail_(\w+)_powered$/, "$1_rail_on"],
  [/^rail_(\w+)$/, "$1_rail"],
  [/^golden_rail(.*)$/, "powered_rail$1"],
  [/^double_plant_fern_(\w+)$/, "large_fern_$1"],
  [/^double_plant_grass_(\w+)$/, "tall_grass_$1"],
  [/^double_plant_paeonia_(\w+)$/, "peony_$1"],
  [/^double_plant_rose_(\w+)$/, "rose_bush_$1"],
  [/^double_plant_sunflower_(\w+)$/, "sunflower_$1"],
  [/^double_plant_syringa_(\w+)$/, "lilac_$1"],
]

const oldDir = argv._[0]; if (!oldDir) throw new Error("No oldDir specified");
const newDir = argv._[1]; if (!newDir) throw new Error("No newDir specified");

const filename = p => path.basename(p, ".png");
const oldFiles = (await glob([oldDir.replace(/\\/g, "/") + "/*.png"])).map(filename);
const newFiles = (await glob([newDir.replace(/\\/g, "/") + "/*.png"])).map(filename);

function map(name) {
  if (manualMapping[name]) {
    return "minecraft:block/" + manualMapping[name];
  }

  for (const re of regexes) {
    name = name.replace(re[0], re[1]);
  }

  return "minecraft:block/" + name;
}

for (const name of oldFiles) {
  mapping["minecraft:blocks/" + name] = map(name);
}

await fs.writeJson("../src/assets/texture-mapping.debug.json", {
  oldFiles, newFiles, mapping
}, { spaces: 2 });

await fs.writeJson("../src/assets/texture-mapping.json", mapping);
