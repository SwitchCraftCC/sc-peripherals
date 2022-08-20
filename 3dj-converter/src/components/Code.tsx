import classNames from "classnames";

import copy from "clipboard-copy";

interface Props {
  className?: string;
  children?: string;
}

export function Code({ className, children }: Props): JSX.Element {
  return <code
    onClick={() => children ? copy(children) : () => {}}
    className={classNames(
      "text-slate-400 hover:text-slate-500 cursor-pointer",
      className
    )}
  >
    {children}
  </code>;
}
