import { useEffect, useState } from 'react'
import { NavLink, Outlet, useMatches } from 'react-router-dom'
import { Disclosure, DisclosureButton, DisclosurePanel, Menu, MenuButton, MenuItem, MenuItems, Transition } from '@headlessui/react'
import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline'

import { NotificationContainer } from '../components/layout/NotificationContainer'
import Footer from '../components/layout/Footer'

function getNavClass (isMobile, { isActive } = { isActive: false }) {
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

function getNav (userInfo, isMobile = false) {
  return (
    <>
      <NavLink to='/bots' className={(a) => getNavClass(isMobile, a)}>Bots</NavLink>
      <NavLink to='/servers' className={(a) => getNavClass(isMobile, a)}>Servers</NavLink>
      <NavLink to='/settings' className={(a) => getNavClass(isMobile, a)}>Settings</NavLink>
      {userInfo && userInfo.id &&
        <>
          {!isMobile && <div className='border-r border-gray-700 w-0'>&nbsp;</div>}
          <Menu as='div' className='relative'>
            <div>
              <MenuButton as='a' href='' className={getNavClass(isMobile)}>
                <span className='absolute -inset-1.5' />
                <span className='sr-only'>Open user menu</span>
                {userInfo.username}
              </MenuButton>
            </div>
            <MenuItems
              transition
              className='absolute right-0 z-10 mt-2 w-48 origin-top-right rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 transition focus:outline-none data-[closed]:scale-95 data-[closed]:transform data-[closed]:opacity-0 data-[enter]:duration-100 data-[leave]:duration-75 data-[enter]:ease-out data-[leave]:ease-in'
            >
              <MenuItem>
                <a href='/logout' className='block px-4 py-2 text-sm text-gray-700 data-[focus]:bg-gray-100'>
                  Logout
                </a>
              </MenuItem>
            </MenuItems>
          </Menu>
        </>}
    </>
  )
}

function Layout () {
  const matches = useMatches()
  const { handle, data } = matches[matches.length - 1]
  const title = handle && handle.title ? handle.title(data) : ''
  const hideHeader = handle && handle.hideHeader ? handle.hideHeader(data) : false
  const hideFooter = handle && handle.hideFooter ? handle.hideFooter(data) : false

  const [userInfo, setUserInfo] = useState({})

  useEffect(() => {
    fetch('/api/user').then((res) => res.json()).then((data) => {
      setUserInfo(data)
    }).catch(() => {
      // Not logged in or auth disabled
    })
  }, [])

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
                        {getNav(userInfo)}
                      </div>
                    </div>

                    <div className='-mr-2 flex md:hidden'>
                      {/* Mobile menu button */}
                      <DisclosureButton className='relative inline-flex items-center justify-center rounded-md bg-gray-800 p-2 text-gray-400 hover:bg-gray-700 hover:text-white focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800'>
                        <span className='absolute -inset-0.5' />
                        <span className='sr-only'>Open navigation menu</span>
                        {open
                          ? (
                            <XMarkIcon className='block h-6 w-6' aria-hidden='true' />
                            )
                          : (
                            <Bars3Icon className='block h-6 w-6' aria-hidden='true' />
                            )}
                      </DisclosureButton>
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
                  <DisclosurePanel className='md:hidden'>
                    <div className='space-y-1 px-2 pb-3 pt-2 sm:px-3'>
                      {getNav(userInfo, true)}
                    </div>
                  </DisclosurePanel>
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
        <Outlet context={{ currentUserInfo: userInfo }} />
      </main>

      {(!hideFooter &&
        <Footer />
      )}
    </>
  )
}

export default Layout
