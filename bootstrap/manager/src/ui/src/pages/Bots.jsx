import { useEffect, useState } from 'react'
import { ArrowUpTrayIcon, PlusIcon } from '@heroicons/react/16/solid'
import { useLocation, useNavigate } from 'react-router-dom'

import Bot from '../components/Bot'
import Banner from '../components/Banner'
import Button from '../components/Button'
import Dropdown from '../components/Dropdown'
import UploadFileModal from '../components/modals/UploadFileModal'
import TextInputModal from '../components/modals/TextInputModal'

function Bots () {
  const navigate = useNavigate()
  const { state } = useLocation()

  const [bots, setBots] = useState([])
  const [importLegacyOpen, setImportLegacyOpen] = useState(false)
  const [importCredentialsOpen, setImportCredentialsOpen] = useState(false)

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

  const importLegacy = (file) => {
    const formData = new FormData()
    formData.append('file', file)

    fetch('/api/bots/import/legacy', { method: 'POST', body: formData }).then((res) => res.json()).then((data) => {
      console.log(data)
      updateBots()
    })
  }

  const importCredentials = (data) => {
    fetch('/api/bots/import/credentials', { method: 'POST', body: data }).then((res) => res.json()).then((data) => {
      console.log(data)
      updateBots()
    })
  }

  return (
    <>
      {state && state.error && <Banner className='px-8 pb-6' color='red' width='2xl'>Error loading bot: {state.error}</Banner>}
      <UploadFileModal
        title='Import bots from legacy'
        message='Select the bots file to import from the legacy system. Must be a .zip file of the cache folder.'
        accept='.zip'
        open={importLegacyOpen}
        onClose={(success, file) => {
          setImportLegacyOpen(false)
          if (!success) return
          importLegacy(file)
        }}
      />
      <TextInputModal
        title='Import bots from credentials'
        message='Enter the credentials for the bots to import. Must be in the format: email:password'
        open={importCredentialsOpen}
        onClose={(success, data) => {
          setImportCredentialsOpen(false)
          if (!success) return
          console.log(data)
          importCredentials(data)
        }}
      />
      <div className='px-8 pb-6 flex justify-center'>
        <div className='max-w-2xl w-full flex flex-row justify-end gap-1'>
          <Dropdown
            label={<ArrowUpTrayIcon className='size-4' aria-hidden='true' />}
            title='Import bots'
            color='blue'
            options={{
              'From legacy': () => {
                setImportLegacyOpen(true)
              },
              'From credentials': () => {
                setImportCredentialsOpen(true)
              }
            }}
          />
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
