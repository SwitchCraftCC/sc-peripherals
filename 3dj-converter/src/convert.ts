import { parse } from "lua-json";

import { clamp, isArray } from "lodash-es";
import rawTextureMap from "./assets/texture-mapping.json";
import sanitizeFilename from "sanitize-filename";

const textureMap = rawTextureMap as Record<string, string>;

interface PrintData {
  label?: string;
  tooltip?: string;
  isButton?: boolean;
  collideWhenOff?: boolean;
  collideWhenOn?: boolean;
  lightLevel?: number;
  redstoneLevel?: number;
  shapesOff: Shape[];
  shapesOn: Shape[];
}

interface Shape {
  bounds: [number, number, number, number, number, number];
  tint?: number | string;
  texture: string | null;
}

export interface ConversionResults {
  contents?: string;
  data?: PrintData;
  filename?: string;
  error?: string;
}

const CLEANUP_RE = /    (\{[\s\S]+?\})/g;

export function convert(original: string): ConversionResults {
  try {
    // Ensure the original begins with `return ...`
    if (!original.startsWith("return ")) {
      original = "return " + original;
    }

    const json: unknown = parse(original);
    if (typeof json !== "object" || json === null) {
      throw new Error("Not a valid lua table");
    }

    const collidable = arr<boolean>(json, "collidable");

    const output: PrintData = {
      label  : str(json, "label")?.substring(0, 48),
      tooltip: str(json, "tooltip")?.substring(0, 256),

      isButton      : bool(json, "buttonMode"),
      collideWhenOff: collidable?.[0] ?? true,
      collideWhenOn : collidable?.[1] ?? true,

      lightLevel   : Math.min(int(json, "lightLevel") || 0, 15),
      redstoneLevel: Math.min(int(json, "emitRedstone") || 0, 15),

      shapesOff: [],
      shapesOn : []
    };

    const shapes = arr<Array<number | Array<string | number>>>(json, "shapes");
    if (shapes) {
      for (const shape of shapes) {
        let texture = findKey(shape, "texture")
        if (texture === "") {
          texture = null;
        } else if (typeof texture !== "string") {
          texture = "sc-peripherals:block/white";
        }

        // Attempts to convert vanilla 1.12 texture names to 1.13+ texture names
        // (primarily targets 1.19)
        texture = texture ? tryFindTexture(texture) : null;

        let tint = findKey(shape, "tint")
        if (typeof tint !== "number") tint = 0xFFFFFF

        // Convert tint values to hex strings
        // TODO: Config option for this?
        tint = (tint & 0xFFFFFF).toString(16).padStart(6, "0").toUpperCase();

        let state = findKey(shape, "state");
        if (typeof state !== "boolean") state = false;

        const outShape: Shape = {
          bounds: [
            bound(shape, 0), bound(shape, 1), bound(shape, 2),
            bound(shape, 3), bound(shape, 4), bound(shape, 5)
          ],
          tint,
          texture
        }

        const outArr = state ? output.shapesOn : output.shapesOff;
        outArr.push(outShape);
      }
    }

    // Prettify the JSON, but ensure that each shape is on a single line
    const finalOut = JSON.stringify(output, null, 2)
      .replaceAll(CLEANUP_RE, s => "    " + s.replaceAll(/\s/g, ""));
    const filename = sanitizeFilename(output.label ?? "print")
      .replaceAll(/\s/g, "-") + ".3dj";

    return { contents: finalOut, data: output, filename };
  } catch (err: unknown) {
    console.error(err);

    const message = (err instanceof Error) ? err.message : String(err);
    return { error: `Failed to convert: ${message}\nSee console for more details.` };
  }
}

const str = (obj: any, key: string): string | undefined =>
  key in obj && typeof obj[key] === "string" ? obj[key] : undefined;
const bool = (obj: any, key: string): boolean | undefined =>
  key in obj && typeof obj[key] === "boolean" ? obj[key] : undefined;
const int = (obj: any, key: string): number | undefined =>
  key in obj && typeof obj[key] === "number" ? obj[key] : undefined;
const arr = <T> (obj: any, key: string): Array<T> | undefined =>
  key in obj && isArray(obj[key]) ? obj[key] : undefined;

function bound(arr: Array<number | Array<string | number>>, idx: number): number {
  const val = arr[idx];
  if (typeof val === "number") return clamp(val, 0, 16);
  return 0;
}

function findKey(
  arr: Array<number | Array<string | number>>,
  key: string
): string | number | boolean | undefined | null {
  const def = arr.find(a => isArray(a) && a[0] === key);
  if (!def || !isArray(def) || def.length < 2) return

  const val = def[1]
  const type = typeof val;
  if (type === "number" || type === "string" || type === "boolean") return val;
}

function tryFindTexture(name: string): string {
  return textureMap[name] ??
    textureMap["minecraft:" + name] ??
    textureMap["minecraft:blocks/" + name] ??
    textureMap["minecraft:blocks/" + name.replace(/^minecraft:/, "")] ??
    name;
}
