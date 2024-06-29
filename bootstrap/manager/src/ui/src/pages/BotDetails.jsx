import { DocumentCheckIcon, TrashIcon } from '@heroicons/react/16/solid'
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Select from '../components/Select'
import Button from '../components/Button'
import ConfirmModal from '../components/modals/ConfirmModal'
import { ExclamationTriangleIcon, QuestionMarkCircleIcon } from '@heroicons/react/24/outline'
import { addNotification } from '../components/NotificationContainer'

function BotDetails () {
  const { botId } = useParams()
  const navigate = useNavigate()

  const [info, setInfo] = useState({
    gamertag: '',
    xid: '',
    status: '',
    serverId: ''
  })

  const [servers, setServers] = useState([])
  const [currentServer, setCurrentServer] = useState({
    hostname: '',
    port: 0
  })

  const [logs, setLogs] = useState('')
  const logsRef = useRef(null)

  const [formData, setFormData] = useState({
    serverId: ''
  })

  const [deleteOpen, setDeleteOpen] = useState(false)
  const deleteCallback = useRef(() => {})

  // const [seenLoginCode, setSeenLoginCode] = useState(false)
  const [loginCodeOpen, setLoginCodeOpen] = useState(false)
  // const [loginCodeCallback, setLoginCodeCallback] = useState(() => {})
  const loginCodeCallback = useRef(() => {})
  const seenLoginCode = useRef(false)

  const updateData = () => {
    fetch('/api/bots/' + botId).then((res) => {
      if (!res.ok) {
        // Redirect to the bots page if an error occurs
        addNotification('Error loading bot: ' + res.statusText, 'red')
        return navigate('/bots')
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
        if (!seenLoginCode.current) {
          // Check if the second last line contains a link to login
          const secondLastLine = data.trim().split('\n').reverse()[1]
          if (secondLastLine.includes('https://www.microsoft.com/link')) {
            // Extract the code from the line using regex
            const code = secondLastLine.match(/ [A-Z0-9]+ /)[0].trim()

            // Ask the user if they want to open the page
            loginCodeCallback.current = (success) => {
              if (!success) return
              window.open('https://www.microsoft.com/link?otc=' + code, '_blank')
            }
            setLoginCodeOpen(true)

            seenLoginCode.current = true
          }
        }

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

  const callDelete = () => {
    fetch('/api/bots/' + botId, { method: 'DELETE' }).then((res) => res.json()).then((data) => {
      if (data.error) {
        console.error(data)
        addNotification('Failed to delete bot: ' + data.error, 'red')
        return
      }
      addNotification('Deleted bot', 'green')
      navigate('/bots')
    })
  }

  return (
    <>
      <ConfirmModal
        title='Open login'
        message='A login link has been detected. Do you wish to open it?'
        confirmText='Open'
        color='green'
        Icon={QuestionMarkCircleIcon}
        open={loginCodeOpen}
        onClose={(success) => {
          setLoginCodeOpen(false)
          loginCodeCallback.current(success)
        }}
      />
      <ConfirmModal
        title='Delete bot'
        message='Are you sure you want to delete this bot? This action cannot be undone.'
        confirmText='Delete'
        color='red'
        Icon={ExclamationTriangleIcon}
        open={deleteOpen}
        onClose={(success) => {
          setDeleteOpen(false)
          deleteCallback.current(success)
        }}
      />
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
              <button className='flex justify-center items-center grow rounded-md text-white font-bold bg-red-700 hover:bg-red-800 disabled:bg-gray-700 disabled:cursor-not-allowed transition-colors duration-150' disabled={info.status.toLowerCase() === 'offline'} onClick={() => callAction('stop')}>
                Stop
              </button>
            </div>
            <button className='flex justify-center items-center md:min-h-0 min-h-12 rounded-md text-white font-bold bg-orange-700 hover:bg-orange-800 transition-colors duration-150' disabled={info.status === ''} onClick={() => callAction('restart')}>
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
              options={servers.map((server) => ({ value: server.id, label: server.hostname + ':' + server.port }))}
            />
            <Button color='green' type='submit'>
              <div>
                Save
              </div>
              <DocumentCheckIcon className='size-4' aria-hidden='true' />
            </Button>
            <Button
              color='red'
              onClick={() => {
                deleteCallback.current = (success) => {
                  if (!success) return
                  callDelete()
                }
                setDeleteOpen(true)
              }}
            >
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
