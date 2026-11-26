import { useEffect, useState } from 'react'

export default function useRemoteData(load) {
  const [result, setResult] = useState(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setResult({ data })
      })
      .catch((error) => {
        if (!controller.signal.aborted) setResult({ error: error.message })
      })
    return () => controller.abort()
  }, [load, revision])

  function reload() {
    setResult(null)
    setRevision((current) => current + 1)
  }

  function replace(update) {
    setResult((current) => ({ data: update(current.data) }))
  }

  return { result, reload, replace }
}
