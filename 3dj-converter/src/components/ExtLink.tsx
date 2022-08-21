import { FC, HTMLProps } from "react";

export const ExtLink: FC<HTMLProps<HTMLAnchorElement>> = ({ children, ...props }) => {
  return <a
    {...props}
    target="_blank"
    rel="noopener noreferrer"
    className="text-indigo-500"
  >{children}</a>;
};
