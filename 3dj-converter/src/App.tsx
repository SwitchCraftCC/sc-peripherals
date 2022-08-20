import { Paragraph } from "./components/Paragraph";
import { Code } from "./components/Code";

import { Converter } from "./Converter";
import { ExtLink } from "./components/ExtLink";

export function App(): JSX.Element {
  return <div className="container mx-auto px-4 py-8">
    <h1 className="text-4xl font-medium mb-4">.3dm to .3dj converter</h1>
    <Paragraph className="mb-3">
      This tool converts OpenComputers 3dm-format (Lua table) print files to
      3dj-format (JSON) print files for the sc-peripherals mod.
    </Paragraph>

    <Paragraph className="mb-3">
      Texture names will automatically be converted for vanilla textures from
      their 1.12.2 names to their equivalent 1.19.2 names. The texture
      name <Code>opencomputers:blocks/white</Code> will also be converted
      to <Code>sc-peripherals:block/white</Code>.
    </Paragraph>

    <Converter />

    <Paragraph className="mt-6 mb-3 text-center text-slate-500">
      Made by <ExtLink href="https://github.com/Lemmmy">Lemmmy</ExtLink>
    </Paragraph>
  </div>;
}
