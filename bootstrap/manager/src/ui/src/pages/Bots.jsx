/* global fetch */

import { useEffect, useState } from 'react'
import { PlusIcon } from '@heroicons/react/16/solid'
import { useLocation, useNavigate } from 'react-router-dom'

import Bot from '../components/Bot'
import Banner from '../components/Banner'

function Bots () {
  const [bots, setBots] = useState([])
  const navigate = useNavigate()
  const { state } = useLocation()

  useEffect(() => {
    fetch('/api/bots').then((res) => res.json()).then((data) => {
      setBots(data)
    })

    // Clear the error state when the component mounts
    if (state && state.error) {
      window.history.replaceState({}, '')
    }
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
          <button className='flex justify-center items-center rounded-md py-2 px-2 text-white font-bold bg-green-700 hover:bg-green-800 transition-colors duration-150' onClick={() => addBot()}>
            <PlusIcon className='size-4' aria-hidden='true' />
          </button>
        </div>
      </div>
      {bots.map((bot) => (
        <Bot key={bot.id} bot={bot} onClick={() => { navigate('/bots/' + bot.id) }} />
      ))}
    </>
  )
}

export default Bots
