import { useEffect, useRef, useState } from 'react'
import Input from '../components/basic/Input'
import Button from '../components/basic/Button'
import { useNavigate } from 'react-router-dom'

function Login () {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  })
  const [message, setMessage] = useState({
    color: '',
    message: ''
  })
  const submitRef = useRef(null)

  useEffect(() => {
    if (window.location.search === '?logout') {
      setMessage({
        color: 'green',
        message: 'Successfully logged out'
      })
    }
  }, [])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prevState => ({
      ...prevState,
      [name]: value
    }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    submitRef.current.disabled = true

    fetch('/login', { method: 'POST', body: new URLSearchParams(formData) }).then((res) => res.json()).then((data) => {
      if (data.success) {
        return navigate('/')
      } else {
        setMessage({
          color: 'red',
          message: 'Invalid username or password'
        })
        setFormData({
          username: formData.username,
          password: ''
        })
        submitRef.current.disabled = false
      }
    }).catch((err) => {
      setMessage({
        color: 'red',
        message: err.message
      })
      setFormData({
        username: formData.username,
        password: ''
      })
      submitRef.current.disabled = false
    })
  }

  return (
    <>
      <div className='p-8 flex items-center flex-col gap-5 h-full'>
        <div className='bg-white p-6 rounded shadow-lg max-w-xl w-full'>
          <h3 className='text-3xl text-center pb-3'>Login</h3>
          {message.message && <div className={'text-center pb-3 text-' + message.color + '-600'}>{message.message}</div>}
          <form className='flex flex-col gap-4' onSubmit={handleSubmit}>
            <Input
              label='Username'
              type='text'
              id='username'
              name='username'
              value={formData.username}
              onChange={handleChange}
              required
            />
            <Input
              label='Password'
              type='password'
              id='password'
              name='password'
              value={formData.password}
              onChange={handleChange}
              required
            />
            <Button type='submit' color='green' className='disabled:cursor-wait' ref={submitRef}>Login</Button>
          </form>
        </div>
      </div>
    </>
  )
}

export default Login
