import { useEffect, useState } from "react";

import { Textarea } from "./components/Textarea";
import { Button } from "./components/Button";
import { NumberStat } from "./components/NumberStat";
import { Error } from "./components/Error";
import { Paragraph } from "./components/Paragraph";
import { Code } from "./components/Code";

import { ConversionResults, convert } from "./convert";

import { throttle } from "lodash-es";
import { saveAs } from "file-saver";
import { FileDrop } from "react-file-drop";

function _convert(
  value: string,
  setResults: (results: ConversionResults) => void
) {
  setResults(convert(value));
}
const throttledConvert = throttle(_convert, 100);

function downloadFile(
  contents: string | undefined,
  filename: string | undefined
) {
  if (!contents || !filename) return;
  const blob = new Blob([contents], { type: "text/plain" });
  const url = URL.createObjectURL(blob);
  saveAs(url, filename);
}

function readDroppedFile(
  file: File | undefined,
  setOriginal: (original: string) => void,
) {
  if (!file) return setOriginal("");

  const reader = new FileReader();
  reader.onload = (e) => {
    setOriginal(reader.result as string);
  }
  reader.readAsText(file);
}

export function Converter(): JSX.Element {
  const [original, setOriginal] = useState("");
  const [results, setResults] = useState<ConversionResults>({});
  const { contents, data, filename, error } = results;

  const handleChange = (value: string) => {
    setOriginal(value)
    throttledConvert(value, setResults);
  }

  useEffect(() => {
    handleChange("")
  }, []);

  return <>
    {/* Input */}
    <h1 className="text-lg font-medium mb-2">Original .3dm file</h1>
    <Paragraph className="mb-3">
      Drop a 3dm file, or paste the contents of your old OpenComputers
      3dm-format (Lua table) print here.
    </Paragraph>

    {/* Input textarea */}
    <FileDrop
      onDrop={files => readDroppedFile(files?.[0], handleChange)}
    >
      <Textarea
        className=""
        value={original} setValue={handleChange}
        placeholder="Drop or paste 3dm file here..."
      />
    </FileDrop>

    {/* Statistics */}
    {!error && data && <div className="flex flex-row gap-4">
      <div className="text-slate-400">
        Off shapes: <NumberStat value={data.shapesOff.length} limit={128} />
      </div>
      <div className="text-slate-400">
        On shapes: <NumberStat value={data.shapesOn.length} limit={128} />
      </div>
    </div>}

    {/* Conversion output */}
    <h1 className="text-lg font-medium mt-6 mb-2">Converted .3dj file</h1>
    {error
      ? <Error error={error} />
      : <>
        <Paragraph className="mb-3">
          The print in the new sc-peripherals 3dj-format (JSON file).
          Download the file to drag &amp; drop it into your ComputerCraft
          computer.
        </Paragraph>

        <Paragraph className="mb-3">
          Print the file by
          running <Code>{`print3d ${filename || "print.3dj"}`}</Code> in-game
          (click to copy).
        </Paragraph>

        {/* Conversion results text */}
        <Textarea className="mb-1" readOnly value={contents ?? ""} />

        {/* Download button */}
        <div className="flex flex-row gap-3 items-center">
          <Button
            disabled={!contents}
            onClick={() => downloadFile(contents, filename)}
          >
            Download file
          </Button>

          <span className="text-slate-500">
            <span className="text-green-500 font-medium">Tip! </span>
            Drag &amp; drop the file into ComputerCraft!
          </span>
        </div>
      </>}
  </>;
}
