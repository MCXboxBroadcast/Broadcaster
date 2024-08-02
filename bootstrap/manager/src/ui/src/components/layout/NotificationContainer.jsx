import { useState } from 'react'

let addNotification = () => {
  console.error('NotificationContainer not initialized')
}

function NotificationContainer () {
  const [notifications, setNotifications] = useState([])

  addNotification = (message, color = 'green') => {
    const id = Math.random()
    setNotifications([...notifications, { message, color, id }])
    setTimeout(() => {
      removeNotification(id)
    }, 5 * 1000)
  }

  function removeNotification (id) {
    setNotifications((notifications) => notifications.filter((notif) => notif.id !== id))
  }

  return (
    <>
      <div className='flex flex-col gap-2 fixed bottom-4 right-4'>
        {notifications.map(({ message, color }, i) => (
          <div className='flex justify-end' key={i}>
            <div className={'p-4 bg-' + color + '-300 text-' + color + '-600 border border-' + color + '-600 rounded-md shadow-lg'}>
              {message}
            </div>
          </div>
        ))}
      </div>
    </>
  )
}

export {
  NotificationContainer,
  addNotification
}
