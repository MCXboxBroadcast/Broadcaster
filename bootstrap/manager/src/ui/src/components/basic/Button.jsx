import { forwardRef } from 'react'

const Button = forwardRef(function (props, ref) {
  const newProps = { ...props }
  const { color = 'green', children, className } = props
  delete newProps.color
  delete newProps.children
  delete newProps.className

  return (
    <>
      <button {...newProps} className={'flex justify-center items-center gap-1 py-2 px-2 rounded-md text-white font-bold bg-' + color + '-700 hover:bg-' + color + '-800 disabled:bg-gray-700 disabled:cursor-not-allowed transition-colors duration-150 ' + className} ref={ref}>
        {children}
      </button>
    </>
  )
})

export default Button
