import { UserCircleIcon, UserMinusIcon } from '@heroicons/react/16/solid'

import Button from '../basic/Button'
import { addNotification } from '../layout/NotificationContainer'
import { useState } from 'react'

function Friend ({ botId, friend, updateData }) {
  const [disabled, setDisabled] = useState(false)

  const callRemove = () => {
    setDisabled(true)
    fetch('/api/bots/' + botId + '/friends/' + friend.xuid, { method: 'DELETE' }).then((res) => {
      if (!res.ok) {
        return addNotification('Failed to remove friend', 'error')
      }
      updateData()
    }).finally(() => setDisabled(false))
  }

  return (
    <>
      <div className='flex hover:bg-slate-100 rounded p-2 gap-2 items-center'>
        <UserCircleIcon className={'w-4 h-4 ' + (friend.presenceState === 'Online' ? 'text-green-600' : 'text-red-600')} />
        <div className='grow content-center'>{friend.gamertag}<small className='text-xs text-gray-400'> {friend.xuid}</small></div>
        <Button title='Remove friend' color='red' onClick={() => callRemove()} disabled={disabled}><UserMinusIcon className='size-4' aria-hidden='true' /></Button>
      </div>
    </>
  )
}

export default Friend
