import classNames from "classnames";

interface Props {
  value?: string;
  setValue?: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  className?: string;
}

export function Textarea({
  value, setValue,
  placeholder, readOnly,
  className
}: Props): JSX.Element {
  return <textarea
    className={classNames("bg-slate-800 w-full rounded-md shadow-lg font-mono p-3 text-sm whitespace-pre", className)}
    rows={10}
    value={value}
    onChange={e => !readOnly && setValue?.(e.target.value)}
    placeholder={placeholder}
    readOnly={readOnly}
  />;
}
