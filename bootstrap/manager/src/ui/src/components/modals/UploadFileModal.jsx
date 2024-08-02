import { useEffect, useRef, useState } from 'react'

import Modal from './Modal'

function UploadFileModal ({ title, message, accept = '', open = false, onClose }) {
  const [fileData, setFileData] = useState(null)
  const fileRef = useRef()

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
            <button
              type='button'
              className='mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto'
              onClick={() => {
                fileRef.current.click()
              }}
            >
              Choose File {fileData ? ` - ${fileData.name}` : ''}
            </button>
            <input
              type='file'
              id='file'
              name='file'
              onChange={(e) => e.target.files.length > 0 && setFileData(e.target.files[0])}
              required
              accept={accept}
              ref={fileRef}
              className='hidden'
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
