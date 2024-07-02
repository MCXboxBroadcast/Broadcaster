import { useEffect, useState } from 'react'

function Footer () {
  const [info, setInfo] = useState({})

  useEffect(() => {
    fetch('/api/info').then((res) => res.json()).then((data) => {
      setInfo(data)
    })
  }, [])

  return (
    <footer className='bg-gray-800 text-white px-8 py-4 flex md:flex-row md:justify-between flex-col items-center'>
      <div>
        Copyright &copy; rtm516 2024-{new Date().getFullYear()}
      </div>
      <div>
        {info.version}
      </div>
    </footer>
  )
}

export default Footer
