# sc-peripherals

Fabric 1.19 [CC: Tweaked](https://github.com/cc-tweaked/cc-tweaked) peripheral mod for the SwitchCraft server. 
The main feature currently is 3D Printers in the style of 
[OpenComputers](https://github.com/MightyPirates/OpenComputers).

![sc-peripherals](img/header.png)

## Modpacks

Modpack use: **allowed**

This mod (`sc-peripherals`) was created primarily for use by the SwitchCraft server, but you are **allowed** to use the 
mod in your own modpack.

Please note that each custom SwitchCraft mod has its own license, so check the license of each mod before using it in 
your modpack.

## Differences from OpenComputers

- 3D printers, ink cartridges are simpler to craft
  - The ink cartridge recipe is crafted as a filled cartridge to begin with, but a refill recipe is still provided
- 3D printers do not require the use of energy
- 3D printers have externally-visible progress bars for chamelium and ink levels
- First-class ComputerCraft peripheral API support
- `print3d` program included in the ROM by default via a data pack in the mod
  - Printing can be safely terminated by the user, and it will automatically stop after the current item is finished
  - `print3d stop` command to stop a printer via the CLI
  - Uses [3dj format](#3dj-format) instead of 3dm (converter here: https://3dj.lem.sh/)
- Default maximum shape count (configurable) for each state of a print raised from 24 to 128
- The default maximum light level in printed models is now 7 instead of 8 - this is now equivalent to a redstone torch
  - Prints can still be crafted with 8 pieces of glowstone dust to reach maximum brightness level
- 3D prints are waterloggable
- The included "white" texture name changed from `opencomputers:blocks/white` to `sc-peripherals:block/white`
- Print models are more efficiently cached, so chunks with lots of identical prints will only bake the print model once
  - Note that this is not the be-all and end-all of performance, there may still be a lot of vertex data to upload to
    the chunk

## .3dj format

The 3dj format was created as a more versatile alternative for processing and storing 3D models compared to the old
OpenComputers 3dm format. It uses JSON instead of Lua tables, so it is easier to work with programmatically.

There is an online .3dm to .3dj converter here: https://3dj.lem.sh/

#### Example

All arguments except for `shapesOff`, `shapesOn`, `bounds` and `texture` are optional.

```json5
{
  "label": "...",
  "tooltip": "...",
  "isButton": false,
  "collideWhenOn": true,
  "collideWhenOff": true,
  "lightLevel": 0,
  "redstoneLevel": 0,
  "shapesOff": [
    { bounds: [0, 0, 0, 16, 16, 16], texture: "", tint: "FFFFFF" },
    { bounds: [0, 0, 0, 16, 16, 16], texture: "", tint: "FFFFFF" },
    { bounds: [0, 0, 0, 16, 16, 16], texture: "", tint: "FFFFFF" }
  ],
  "shapesOn": [
    { bounds: [0, 0, 0, 16, 16, 16], texture: "", tint: "FFFFFF" }  
  ]
}
```

#### Fields

- `label`: (optional, string) The name of the 3D print, maximum 48 characters.
- `tooltip`: (optional, string) The tooltip of the 3D print in the inventory, maximum 256 characters.
- `isButton`: (optional, boolean) Whether the 3D print acts as a button when right-clicked. If true, the print will
  automatically switch to the 'off' state after 20 ticks when right-clicked. If false, right-clicking will toggle the 
  state.
- `collideWhenOff`: (optional, boolean) Whether the 3D print is collidable when in the 'off' state.
- `collideWhenOn`: (optional, boolean) Whether the 3D print is collidable when in the 'on' state.
- `lightLevel`: (optional, number) The light level of the 3D print. Must be between 0 or 15, but values above 7 will
  be clamped to 7 unless the print is later crafted with glowstone dust.
- `redstoneLevel`: (optional, number) The redstone level of the 3D print. Must be between 0 or 15.
- `shapesOff`: (**required**, array of objects) The shapes of the 3D print when in the 'off' state. Each object in the array
  must have a `bounds` property with the bounds of the shape, a `texture` property with the texture of the shape, and an
  optional `tint` property with the tint of the shape, which may be a number or a hex string (`RRGGBB`).
  - `bounds`: (**required**, array of numbers) The bounds of the shape, in the format
    `[minX, minY, minZ, maxX, maxY, maxZ]`. Numbers must be between 0 and 16 inclusive (16 is the edge of the block).
  - `texture`: (**required**, string) The texture of the shape, including the namespace. For example,
    `minecraft:block/stone` or `sc-peripherals:block/white`. Use the texture analyzer item to find the texture of a
    block in the world. The `sc-peripherals:block/white` texture is available as a blank texture for tinting.
  - `tint`: (optional, number or string) The tint of the shape, as a hex string in the format `RRGGBB`, or a single
    decimal value.
- `shapesOn`: (**required**, array of objects) Same as `shapesOff`, but for the 'on' state. To disallow state changes
  and have no 'on' state, pass an empty array.

#### Differences from 3dm

- Uses JSON instead of Lua tables
- `emitRedstone` renamed to `redstoneLevel`
- `collidable` array is now two separate `collideWhenOff`, `collideWhenOn` boolean fields (default to true)
- `buttonMode` renamed to `isButton`
- `shapes` array has been separated into `shapesOff`, `shapesOn`
  - `state` field removed 
  - `tint` in a shape may now be a number or a hex string (`RRGGBB`)
  
## License

This repository is licensed under the [MIT license](LICENSE.md).
