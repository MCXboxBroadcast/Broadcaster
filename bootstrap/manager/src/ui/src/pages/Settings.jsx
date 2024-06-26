import UserPanel from '../settings/UserPanel'

function Settings () {
  return (
    <>
      <div className='px-8 pb-12 flex items-center flex-col gap-5'>
        <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
          <h3 className='text-3xl text-center pb-4'>General</h3>
          <p>
            Not implemented yet
          </p>
        </div>
        <UserPanel />
      </div>
    </>
  )
}

export default Settings
