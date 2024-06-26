import { useEffect, useState } from 'react'

import Modal from './Modal'
import Input from './Input'
import Banner from './Banner'

function ChangePasswordModal ({ user, open = false, onClose }) {
  const [formData, setFormData] = useState({
    password: ''
  })
  const [error, setError] = useState('')

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prevState => ({
      ...prevState,
      [name]: value
    }))
  }

  useEffect(() => {
    if (open) {
      setError('')
      setFormData({
        password: ''
      })
    }
  }, [open])

  const handleSubmit = (success) => {
    if (!success) return onClose(success)

    // Create the user
    fetch('/api/users/' + user, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(formData)
    }).then((res) => {
      if (res.ok) {
        onClose(true)
      } else {
        res.json().then((data) => {
          setError(data.error)
        }).catch(() => {
          setError('An unknown error occurred')
        }).finally(() => {
          setFormData({
            username: formData.username,
            password: ''
          })
        })
      }
    })
  }

  return (
    <Modal
      title='Change password'
      confirmText='Change'
      color='green'
      open={open}
      onClose={handleSubmit}
      content={
        <>
          <div className='flex flex-col mt-2'>
            {error !== '' && <Banner className='pb-2' color='red' width='full'>{error}</Banner>}
          </div>
          <form className='flex flex-col gap-4 mt-2'>
            <Input
              label='Password'
              type='password'
              id='password'
              name='password'
              value={formData.password}
              onChange={handleChange}
              required
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

export default ChangePasswordModal
