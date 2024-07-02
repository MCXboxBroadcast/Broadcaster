import { useEffect, useState } from 'react'
import { PlusIcon } from '@heroicons/react/16/solid'
import { useNavigate } from 'react-router-dom'

import Server from '../components/specialised/Server'
import Button from '../components/basic/Button'

function Servers () {
  const [servers, setServers] = useState([])
  const navigate = useNavigate()

  const updateServers = () => {
    fetch('/api/servers').then((res) => res.json()).then((data) => {
      setServers(data)
    })
  }

  useEffect(() => {
    updateServers()

    const interval = setInterval(updateServers, 2500) // Update every 2.5 seconds
    return () => clearInterval(interval)
  }, [])

  const addServer = () => {
    fetch('/api/servers/create', { method: 'POST' }).then((res) => res.json()).then((data) => {
      console.log(data)
      navigate('/servers/' + data)
    })
  }

  return (
    <>
      <div className='px-8 pb-6 flex justify-center'>
        <div className='max-w-2xl w-full flex flex-row-reverse'>
          <Button title='Create server' color='green' onClick={() => addServer()}><PlusIcon className='size-4' aria-hidden='true' /></Button>
        </div>
      </div>
      {servers.map((server) => (
        <Server key={server.id} server={server} onClick={() => { navigate('/servers/' + server.id) }} />
      ))}
    </>
  )
}

export default Servers
