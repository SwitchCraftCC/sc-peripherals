import { FC, HTMLProps } from "react";

export const ExtLink: FC<HTMLProps<HTMLAnchorElement>> = ({ children, ...props }) => {
  return <a
    {...props}
    target="_blank"
    rel="noopener noreferrer"
    className="text-slate-400"
  >{children}</a>;
};
