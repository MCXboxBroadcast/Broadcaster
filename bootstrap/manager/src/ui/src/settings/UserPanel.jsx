import { useEffect, useRef, useState } from 'react'
import { ExclamationTriangleIcon } from '@heroicons/react/24/outline'
import { PlusIcon } from '@heroicons/react/16/solid'

import User from '../components/specialised/User'
import Button from '../components/basic/Button'
import ConfirmModal from '../components/modals/ConfirmModal'
import CreateUserModal from '../components/modals/CreateUserModal'
import ChangePasswordModal from '../components/modals/ChangePasswordModal'
import { useOutletContext } from 'react-router-dom'

function UserPanel () {
  const [users, setUsers] = useState([])

  const [deleteOpen, setDeleteOpen] = useState(false)
  const deleteCallback = useRef(() => {})

  const [createUserOpen, setCreateUserOpen] = useState(false)

  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
  const [changePasswordUser, setChangePasswordUser] = useState('')

  const { currentUserInfo } = useOutletContext()

  useEffect(() => {
    refreshUsers()
  }, [])

  const refreshUsers = () => {
    fetch('/api/users').then((res) => res.json()).then((data) => {
      setUsers(data)
    })
  }

  return (
    <>
      <ConfirmModal
        title='Delete account'
        message='Are you sure you want to delete this account? This action cannot be undone.'
        confirmText='Delete'
        color='red'
        Icon={ExclamationTriangleIcon}
        open={deleteOpen}
        onClose={(success) => {
          setDeleteOpen(false)
          deleteCallback.current(success)
        }}
      />
      <CreateUserModal
        refreshUsers={refreshUsers}
        open={createUserOpen}
        onClose={() => setCreateUserOpen(false)}
      />
      <ChangePasswordModal
        open={changePasswordOpen}
        onClose={() => setChangePasswordOpen(false)}
        user={changePasswordUser}
      />
      <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
        <h3 className='text-3xl text-center pb-4'>Users</h3>
        <div className='w-full flex flex-row-reverse px-2'>
          <Button title='Create user' color='green' onClick={() => setCreateUserOpen(true)}><PlusIcon className='size-4' aria-hidden='true' /></Button>
        </div>
        <div className='flex flex-col'>
          {users.map((user) => (
            <User
              key={user.id}
              user={user}
              disableDelete={currentUserInfo && user.id === currentUserInfo.id}
              refreshUsers={refreshUsers}
              changePassword={(userId) => {
                setChangePasswordUser(userId)
                setChangePasswordOpen(true)
              }}
              confirmDelete={(callback) => {
                deleteCallback.current = callback
                setDeleteOpen(true)
              }}
            />
          ))}
        </div>
      </div>
    </>
  )
}

export default UserPanel
