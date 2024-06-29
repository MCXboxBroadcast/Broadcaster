import { Dialog, DialogBackdrop, DialogPanel, DialogTitle } from '@headlessui/react'

function Modal ({ title, confirmText = 'Confirm', color, Icon, content, open = false, onClose }) {
  return (
    <Dialog
      className='relative z-10'
      open={open}
      onClose={() => {
        onClose(false)
      }}
    >
      <DialogBackdrop
        transition
        className='fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity data-[closed]:opacity-0 data-[enter]:duration-300 data-[leave]:duration-200 data-[enter]:ease-out data-[leave]:ease-in'
      />

      <div className='fixed inset-0 z-10 w-screen overflow-y-auto'>
        <div className='flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0'>
          <DialogPanel
            transition
            className='relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all data-[closed]:translate-y-4 data-[closed]:opacity-0 data-[enter]:duration-300 data-[leave]:duration-200 data-[enter]:ease-out data-[leave]:ease-in sm:my-8 sm:w-full sm:max-w-lg data-[closed]:sm:translate-y-0 data-[closed]:sm:scale-95'
          >
            <div className='bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4'>
              <div className={Icon && 'sm:flex sm:items-start'}>
                {Icon &&
                  <div className={'mx-auto flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-' + color + '-100 sm:mx-0 sm:h-10 sm:w-10'}>
                    <Icon className={'h-6 w-6 text-' + color + '-600'} aria-hidden='true' />
                  </div>}
                <div className='mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left'>
                  <DialogTitle as='h3' className='text-base font-semibold leading-6 text-gray-900'>
                    {title}
                  </DialogTitle>
                  <div className='mt-2'>
                    {content}
                  </div>
                </div>
              </div>
            </div>
            <div className='bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6'>
              <button
                type='button'
                className={'inline-flex w-full justify-center rounded-md bg-' + color + '-700 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-' + color + '-800 sm:ml-3 sm:w-auto'}
                onClick={() => {
                  onClose(true)
                }}
              >
                {confirmText}
              </button>
              <button
                type='button'
                className='mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto'
                onClick={() => {
                  onClose(false)
                }}
                data-autofocus
              >
                Cancel
              </button>
            </div>
          </DialogPanel>
        </div>
      </div>
    </Dialog>
  )
}

export default Modal
