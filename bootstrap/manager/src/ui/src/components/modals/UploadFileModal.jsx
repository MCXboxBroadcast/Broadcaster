import { useEffect, useState } from 'react'

import Modal from './Modal'

function UploadFileModal ({ title, message, accept = '', open = false, onClose }) {
  const [fileData, setFileData] = useState(null)

  useEffect(() => {
    if (open) {
      setFileData(null)
    }
  }, [open])

  const handleSubmit = (success) => {
    if (!success) return onClose(success)

    onClose(true, fileData)
  }

  return (
    <Modal
      title={title}
      confirmText='Upload'
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
            {/* TODO Style the button */}
            <input
              type='file'
              id='file'
              name='file'
              onChange={(e) => setFileData(e.target.files[0])}
              required
              accept={accept}
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

export default UploadFileModal
