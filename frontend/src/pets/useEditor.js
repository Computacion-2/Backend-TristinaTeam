import { useState } from 'react'

export default function useEditor() {
  const [edit, setEdit] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  function open(mode, record) {
    setError('')
    setNotice('')
    setEdit({ mode, record })
  }

  function close() {
    setError('')
    setEdit(null)
  }

  async function submit(action, message, onSuccess) {
    if (busy) return
    setError('')
    setBusy(true)
    try {
      const saved = await action()
      setEdit(null)
      setNotice(message)
      onSuccess(saved)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setBusy(false)
    }
  }

  return { edit, busy, error, notice, open, close, submit }
}
