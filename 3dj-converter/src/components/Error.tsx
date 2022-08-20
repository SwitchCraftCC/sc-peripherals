interface Props {
  error: string;
}

export function Error({ error }: Props): JSX.Element {
  return <div className="border-red-600 border-2 rounded-md px-4 py-3">
    <h3 className="font-medium">Error</h3>
    <p className="whitespace-pre">{error}</p>
  </div>
}
