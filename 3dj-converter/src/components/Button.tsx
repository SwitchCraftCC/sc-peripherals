import { ReactNode } from "react";
import classNames from "classnames";

interface Props {
  className?: string;
  disabled?: boolean;
  onClick?: () => void;
  children?: ReactNode;
}

export function Button({ className, disabled, onClick, children }: Props) {
  return <button
    type="button"
    disabled={disabled}
    className={classNames(
      "bg-indigo-500 hover:bg-indigo-600 first-line:text-white px-3 py-2 rounded-md shadow-md",
      {
        "opacity-50": disabled,
      },
      className
    )}
    onClick={onClick}
  >
    {children}
  </button>;
}
