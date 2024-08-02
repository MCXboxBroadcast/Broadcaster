import { Menu, MenuButton, MenuItem, MenuItems } from '@headlessui/react'
import { ChevronDownIcon } from '@heroicons/react/20/solid'

function classNames (...classes) {
  return classes.filter(Boolean).join(' ')
}

function Dropdown ({ label, title, color, options }) {
  return (
    <Menu as='div' className='relative inline-block text-left'>
      <div>
        <MenuButton title={title} className={'inline-flex w-full justify-center gap-x-1.5 rounded-md bg-' + color + '-700 hover:bg-' + color + '-800 px-3 py-2 text-sm font-semibold text-white'}>
          {label}
          <ChevronDownIcon className='-mr-1 h-5 w-5 text-gray-400' aria-hidden='true' />
        </MenuButton>
      </div>

      <MenuItems
        transition
        className='absolute right-0 z-10 mt-2 w-56 origin-top-right rounded-md bg-white shadow-lg ring-1 ring-black ring-opacity-5 transition focus:outline-none data-[closed]:scale-95 data-[closed]:transform data-[closed]:opacity-0 data-[enter]:duration-100 data-[leave]:duration-75 data-[enter]:ease-out data-[leave]:ease-in'
      >
        <div className='py-1'>
          {Object.entries(options).map(([label, onClick]) => (
            <MenuItem key={label}>
              {({ focus }) => (
                <button
                  onClick={onClick}
                  className={classNames(
                    focus ? 'bg-gray-100 text-gray-900' : 'text-gray-700',
                    'block w-full px-4 py-2 text-left text-sm'
                  )}
                >
                  {label}
                </button>
              )}
            </MenuItem>
          ))}
        </div>
      </MenuItems>
    </Menu>
  )
}

export default Dropdown
