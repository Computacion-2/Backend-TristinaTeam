import Button from '../components/Button.jsx'
import Feedback from '../components/Feedback.jsx'

export default function RemoteContent({ result, reload, loading, children }) {
  if (!result) return <Feedback kind="loading">{loading}</Feedback>
  if (result.error) {
    return (
      <>
        <Feedback kind="error">{result.error}</Feedback>
        <Button variant="secondary" onClick={reload}>Reintentar</Button>
      </>
    )
  }
  return children(result.data)
}
