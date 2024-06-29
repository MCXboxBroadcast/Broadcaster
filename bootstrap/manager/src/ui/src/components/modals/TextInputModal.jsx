import { useEffect, useState } from 'react'

import Modal from './Modal'

function TextInputModal ({ title, message, confirmText = 'Submit', rows = 4, cols = 40, open = false, onClose }) {
  const [textData, setTextData] = useState('')

  useEffect(() => {
    if (open) {
      setTextData('')
    }
  }, [open])

  const handleSubmit = (success) => {
    if (!success) return onClose(success)

    onClose(true, textData)
  }

  return (
    <Modal
      title={title}
      confirmText={confirmText}
      color='green'
      open={open}
      onClose={handleSubmit}
      content={
        <>
          {message &&
            <p className='text-sm text-gray-500 pb-2'>
              {message}
            </p>}
          <form className='flex flex-col gap-4 mt-2'>
            <textarea
              id='text'
              name='text'
              value={textData}
              onChange={(e) => setTextData(e.target.value)}
              required
              rows={rows}
              cols={cols}
              className='ring-1 ring-inset ring-gray-300 rounded p-2'
            />
            <input
              type='submit'
              className='hidden'
              onClick={(e) => {
                e.preventDefault()
                handleSubmit(true)
              }}
            />
          </form>
        </>
      }
    />
  )
}

export default TextInputModal
