/* global fetch */

import { DocumentCheckIcon, TrashIcon } from '@heroicons/react/16/solid'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Select from '../components/Select'
import Button from '../components/Button'

function BotDetails () {
  const { botId } = useParams()
  const [info, setInfo] = useState({
    gamertag: '',
    xid: '',
    status: '',
    serverId: ''
  })
  const [currentServer, setCurrentServer] = useState({
    hostname: '',
    port: 0
  })
  const [servers, setServers] = useState([])
  const [logs, setLogs] = useState('')
  const logsRef = useRef(null)
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    serverId: ''
  })

  const updateData = () => {
    fetch('/api/bots/' + botId).then((res) => {
      if (!res.ok) {
        // Redirect to the bots page if an error occurs
        return navigate('/bots', { state: { error: res.statusText } })
      }
      return res.json()
    }).then((data) => {
      setInfo(data)

      fetch('/api/servers').then((res) => res.json()).then((serverData) => {
        setServers(serverData)

        const server = serverData.find((server) => server.id === data.serverId)
        if (server) {
          setCurrentServer({
            hostname: server.hostname,
            port: server.port
          })
        }
      })

      fetch('/api/bots/' + botId + '/logs').then((res) => res.text()).then((data) => {
        setLogs(data)
      })
    })
  }

  useEffect(() => {
    updateData()
    const interval = setInterval(updateData, 2500) // Update every 2.5 seconds
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    logsRef.current.scrollTop = logsRef.current.scrollHeight
  }, [logs.length])

  // Set the form data to the current hostname and port if it's empty
  useEffect(() => {
    if (formData.serverId === '' && info.serverId !== undefined) {
      setFormData({
        serverId: info.serverId
      })
    }
  }, [info])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prevState => ({
      ...prevState,
      [name]: value
    }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()

    fetch('/api/bots/' + botId, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(formData) }).then((res) => res.json()).then((data) => {
      if (data !== '') {
        return console.error(data)
      }
      updateData()
    })
  }

  const callAction = (action) => {
    fetch('/api/bots/' + botId + '/' + action, { method: 'POST' }).then((res) => res.json()).then((data) => {
      if (data !== '') {
        return console.error(data)
      }
      updateData()
    })
  }

  const callDelete = (action) => {
    // TODO Add confirmation
    fetch('/api/bots/' + botId, { method: 'DELETE' }).then((res) => res.text()).then((data) => {
      if (data !== '') {
        return console.error(data)
      }
      navigate('/bots')
    })
  }

  return (
    <>
      <div className='px-8 pb-12 flex items-center flex-col gap-5'>
        <div className='max-w-6xl w-full grid md:grid-cols-2 gap-5'>
          <div className='bg-white rounded shadow-lg p-6 w-full'>
            <div className='flex justify-between mb-4'>
              <div className='font-bold'>Gamertag:</div>
              <div>{info.gamertag}</div>
            </div>
            <div className='flex justify-between mb-4'>
              <div className='font-bold'>Xid:</div>
              <div>{info.xid}</div>
            </div>
            <div className='flex justify-between mb-4'>
              <div className='font-bold'>Server:</div>
              <div>{currentServer.hostname}:{currentServer.port}</div>
            </div>
            <div className='flex justify-between'>
              <div className='font-bold'>Status:</div>
              <div>{info.status}</div>
            </div>
          </div>

          <div className='bg-white rounded shadow-lg p-6 w-full grid grid-rows-2 gap-2'>
            <div className='flex md:min-h-0 min-h-12 gap-2'>
              <button className='flex justify-center items-center grow rounded-md text-white font-bold bg-green-700 hover:bg-green-800 disabled:bg-gray-700 disabled:cursor-not-allowed transition-colors duration-150' disabled={info.status.toLowerCase() !== 'offline'} onClick={() => callAction('start')}>
                Start
              </button>
              <button className='flex justify-center items-center grow rounded-md text-white font-bold bg-red-700 hover:bg-red-800 disabled:bg-gray-700 disabled:cursor-not-allowed transition-colors duration-150' disabled={info.status.toLowerCase() === 'offline'}  onClick={() => callAction('stop')}>
                Stop
              </button>
            </div>
            <button className='flex justify-center items-center md:min-h-0 min-h-12 rounded-md text-white font-bold bg-orange-700 hover:bg-orange-800 transition-colors duration-150' disabled={info.status === ''}  onClick={() => callAction('restart')}>
              Restart
            </button>
          </div>
        </div>
        <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
          <h3 className='text-3xl text-center pb-4'>Logs</h3>
          <div className='bg-black text-white overflow-x-auto text-base h-[30rem] font-mono whitespace-pre' ref={logsRef}>
            {logs}
          </div>
        </div>
        <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
          <h3 className='text-3xl text-center pb-4'>Settings</h3>
          <form className='flex flex-col gap-4' onSubmit={handleSubmit}>
            <Select
              label='Server'
              type='text'
              id='serverId'
              name='serverId'
              value={formData.serverId}
              onChange={handleChange}
              required
              options={servers.map((server) => ({ value: server.id, label: server.hostname + ':' + server.port}))}
            />
            <Button color='green' type='submit'>
              <div>
                Save
              </div>
              <DocumentCheckIcon className='size-4' aria-hidden='true' />
            </Button>
            <Button color='red' onClick={() => callDelete()}>
              <div>
                Delete
              </div>
              <TrashIcon className='size-4' aria-hidden='true' />
            </Button>
          </form>
        </div>
      </div>
    </>
  )
}

export default BotDetails
