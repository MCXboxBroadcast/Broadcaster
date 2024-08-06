import { useEffect, useState } from 'react'

function AboutPanel () {
  const [info, setInfo] = useState({})
  const [updateAvailable, setUpdateAvailable] = useState(false)

  useEffect(() => {
    fetch('/api/info').then((res) => res.json()).then((data) => {
      setInfo(data)

      if (!data.version.startsWith('build')) return
      const versionNumber = data.version.split(' ')[1]

      // Check for updates
      fetch('https://api.github.com/repos/rtm516/MCXboxBroadcast/releases?per_page=1').then((res) => res.json()).then((data) => {
        setUpdateAvailable(data[0].tag_name !== versionNumber)
      })
    })

    // Update uptime every second
    const ticker = setInterval(() => {
      setInfo((prevInfo) => {
        return { ...prevInfo, uptime: prevInfo.uptime + 1 }
      })
    }, 1000)

    return () => clearInterval(ticker)
  }, [])

  function formatTime (dateString) {
    const timeDifference = dateString * 1000
    const days = Math.floor(timeDifference / (1000 * 60 * 60 * 24))
    const hours = Math.floor((timeDifference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((timeDifference % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((timeDifference % (1000 * 60)) / 1000)

    const timeSince = []
    if (days > 0) timeSince.push(`${days}d`)
    if (hours > 0) timeSince.push(`${hours}h`)
    if (minutes > 0) timeSince.push(`${minutes}m`)
    if (seconds > 0) timeSince.push(`${seconds}s`)

    return timeSince.join(' ')
  }

  return (
    <>
      <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
        <h3 className='text-3xl text-center pb-4'>About</h3>
        <div className='flex justify-between mb-4 items-center'>
          <div className='font-bold'>Version:</div>
          <div className='flex flex-col items-end'>
            <span>{info.version}</span>
            {updateAvailable && <a className='text-green-600' href='https://github.com/rtm516/MCXboxBroadcast/releases'>New version available!</a>}
          </div>
        </div>
        <div className='flex justify-between mb-4'>
          <div className='font-bold'>Up time:</div>
          <div>{formatTime(info.uptime)}</div>
        </div>
      </div>
    </>
  )
}

export default AboutPanel
