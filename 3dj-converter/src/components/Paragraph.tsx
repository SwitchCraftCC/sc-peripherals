import classNames from "classnames";
import { ReactNode } from "react";

interface Props {
  className?: string;
  children?: ReactNode;
}

export function Paragraph({ className, children }: Props): JSX.Element {
  return <p className={classNames("text-slate-200", className)}>
    {children}
  </p>;
}
