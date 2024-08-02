import Modal from './Modal'

function ConfirmModal ({ title, message, confirmText = 'Confirm', color, Icon, open = false, onClose }) {
  return (
    <Modal
      title={title}
      confirmText={confirmText}
      color={color}
      Icon={Icon}
      open={open}
      onClose={onClose}
      content={
        <p className='text-sm text-gray-500'>
          {message}
        </p>
      }
    />
  )
}

export default ConfirmModal
