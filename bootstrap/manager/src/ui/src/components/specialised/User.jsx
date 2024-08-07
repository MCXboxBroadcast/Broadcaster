import { KeyIcon, UserMinusIcon } from '@heroicons/react/16/solid'

import Button from '../basic/Button'

function User ({ user, disableDelete, refreshUsers, confirmDelete, changePassword }) {
  const callDelete = () => {
    confirmDelete((success) => {
      if (success) {
        fetch('/api/users/' + user.id, { method: 'DELETE' }).then(() => {
          refreshUsers()
        })
      }
    })
  }

  const callChangePassword = () => {
    changePassword(user.id)
  }

  return (
    <>
      <div className='flex hover:bg-slate-100 rounded p-2 gap-2'>
        <div className='grow content-center'>{user.username}</div>
        <Button title='Change password' color='green' onClick={() => callChangePassword()}><KeyIcon className='size-4' aria-hidden='true' /></Button>
        <Button title='Delete' color='red' disabled={disableDelete} onClick={() => !disableDelete && callDelete()}><UserMinusIcon className='size-4' aria-hidden='true' /></Button>
      </div>
    </>
  )
}

export default User
