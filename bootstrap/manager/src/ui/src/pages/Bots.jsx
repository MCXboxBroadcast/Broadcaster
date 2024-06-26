/* global fetch */

import { useEffect, useState } from 'react'
import { PlusIcon } from '@heroicons/react/16/solid'
import { useLocation, useNavigate } from 'react-router-dom'

import Bot from '../components/Bot'
import Banner from '../components/Banner'
import Button from '../components/Button'

function Bots () {
  const [bots, setBots] = useState([])
  const navigate = useNavigate()
  const { state } = useLocation()

  const updateBots = () => {
    fetch('/api/bots').then((res) => res.json()).then((data) => {
      setBots(data)
    })
  }

  useEffect(() => {
    updateBots()

    // Clear the error state when the component mounts
    if (state && state.error) {
      window.history.replaceState({}, '')
    }

    const interval = setInterval(updateBots, 2500) // Update every 2.5 seconds
    return () => clearInterval(interval)
  }, [])

  const addBot = () => {
    fetch('/api/bots/create', { method: 'POST' }).then((res) => res.json()).then((data) => {
      console.log(data)
      navigate('/bots/' + data)
    })
  }

  return (
    <>
      {state && state.error && <Banner className='px-8 pb-6' color='red' width='2xl'>Error loading bot: {state.error}</Banner>}
      <div className='px-8 pb-6 flex justify-center'>
        <div className='max-w-2xl w-full flex flex-row-reverse'>
          <Button title='Create bot' color='green' onClick={() => addBot()}><PlusIcon className='size-4' aria-hidden='true' /></Button>
        </div>
      </div>
      {bots.map((bot) => (
        <Bot key={bot.id} bot={bot} onClick={() => { navigate('/bots/' + bot.id) }} />
      ))}
    </>
  )
}

export default Bots
