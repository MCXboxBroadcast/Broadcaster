import GeneralPanel from '../settings/GeneralPanel'
import UserPanel from '../settings/UserPanel'
import AboutPanel from '../settings/AboutPanel'

function Settings () {
  return (
    <>
      <div className='px-8 pb-12 flex items-center flex-col gap-5'>
        <GeneralPanel />
        <UserPanel />
        <AboutPanel />
      </div>
    </>
  )
}

export default Settings
