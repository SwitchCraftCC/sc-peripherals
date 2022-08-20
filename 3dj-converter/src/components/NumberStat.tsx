import classNames from "classnames";

interface Props {
  value: number;
  limit: number;
}

export function NumberStat({ value, limit }: Props): JSX.Element {
  const className = classNames("text-slate-300", {
    "font-bold text-red-500": value >= limit,
  });

  return <span className={className}>{value} / {limit}</span>;
}
