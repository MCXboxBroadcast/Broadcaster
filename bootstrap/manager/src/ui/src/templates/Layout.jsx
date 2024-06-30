import { useEffect } from 'react'
import { NavLink, Outlet, useMatches } from 'react-router-dom'
import { Disclosure, Transition } from '@headlessui/react'
import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline'

import { NotificationContainer } from '../components/NotificationContainer'
import Footer from '../components/Footer'

function getNavClass (isMobile, { isActive }) {
  let className = 'rounded-md px-3 py-2 font-medium'
  if (isMobile) {
    className = 'block text-base ' + className
  } else {
    className = 'text-sm ' + className
  }

  if (isActive) {
    className = 'bg-gray-900 text-white ' + className
  } else {
    className = 'text-gray-300 hover:bg-gray-700 hover:text-white ' + className
  }

  return className
}

function getNav (isMobile = false) {
  return (
    <>
      <NavLink to='/bots' className={(a) => getNavClass(isMobile, a)}>Bots</NavLink>
      <NavLink to='/servers' className={(a) => getNavClass(isMobile, a)}>Servers</NavLink>
      <NavLink to='/settings' className={(a) => getNavClass(isMobile, a)}>Settings</NavLink>
    </>
  )
}

function Layout () {
  const matches = useMatches()
  const { handle, data } = matches[matches.length - 1]
  const title = handle && handle.title ? handle.title(data) : ''
  const hideHeader = handle && handle.hideHeader ? handle.hideHeader(data) : false
  const hideFooter = handle && handle.hideFooter ? handle.hideFooter(data) : false

  useEffect(() => {
    if (title) {
      document.title = title + ' - MCXboxBroadcast Manager'
    } else {
      document.title = 'MCXboxBroadcast Manager'
    }
  }, [title])

  // TODO Restyle the header and footer
  return (
    <>
      {(!hideHeader &&
        <div className='bg-gray-800 text-white pb-32'>
          <Disclosure as='nav'>
            {({ open }) => (
              <>
                <div className='mx-auto max-w-7xl px-4 sm:px-6 lg:px-8'>
                  <div className={'flex h-16 items-center justify-between ' + (!hideHeader ? 'border-b border-gray-700' : '')}>
                    <div className='flex items-center'>
                      <h1 className='flex-shrink-0 flex items-center'>
                        MCXboxBroadcast Manager
                      </h1>
                    </div>

                    <div className='hidden md:block'>
                      <div className='ml-10 flex items-baseline space-x-4'>
                        {getNav()}
                      </div>
                    </div>

                    <div className='-mr-2 flex md:hidden'>
                      {/* Mobile menu button */}
                      <Disclosure.Button className='relative inline-flex items-center justify-center rounded-md bg-gray-800 p-2 text-gray-400 hover:bg-gray-700 hover:text-white focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800'>
                        <span className='absolute -inset-0.5' />
                        <span className='sr-only'>Open navigation menu</span>
                        {open
                          ? (
                            <XMarkIcon className='block h-6 w-6' aria-hidden='true' />
                            )
                          : (
                            <Bars3Icon className='block h-6 w-6' aria-hidden='true' />
                            )}
                      </Disclosure.Button>
                    </div>
                  </div>
                </div>

                {/* Mobile menu, show/hide based on menu state. */}
                <Transition
                  enter='transition duration-100 ease-out'
                  enterFrom='transform scale-95 opacity-0'
                  enterTo='transform scale-100 opacity-100'
                  leave='transition duration-75 ease-out'
                  leaveFrom='transform scale-100 opacity-100'
                  leaveTo='transform scale-95 opacity-0'
                >
                  <Disclosure.Panel className='md:hidden'>
                    <div className='space-y-1 px-2 pb-3 pt-2 sm:px-3'>
                      {getNav(true)}
                    </div>
                  </Disclosure.Panel>
                </Transition>
              </>
            )}
          </Disclosure>

          <header className='py-10 flex justify-center'>
            <h2 className='px-8 text-5xl'>
              {title}
            </h2>
          </header>
        </div>
    )}

      <main className={'flex-1 ' + (!hideHeader ? '-mt-32' : '')}>
        <NotificationContainer />
        <Outlet />
      </main>

      {(!hideFooter &&
        <Footer />
      )}
    </>
  )
}

export default Layout
