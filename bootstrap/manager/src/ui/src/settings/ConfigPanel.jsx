import { useEffect, useState } from 'react'

function ConfigPanel () {
  const [settings, setSettings] = useState({})

  useEffect(() => {
    fetch('/api/settings').then((res) => res.json()).then((settings) => {
      setSettings(settings)
    })
  }, [])

  return (
    <>
      <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
        <h3 className='text-3xl text-center'>Config</h3>
        <h4 className='text-sm text-center text-gray-400 pb-4'>
          This can be changed in the application.yaml on the server
        </h4>
        <div className='flex flex-col gap-4'>
          {Object.keys(settings).map((key) => (
            <div key={key} className='flex justify-between'>
              <div className='font-bold'>{key}</div>
              <div>{settings[key]}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}

export default ConfigPanel
