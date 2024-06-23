function Input (props) {
  const newProps = { ...props }
  const { color, children } = props
  delete newProps.color
  delete newProps.children

  return (
    <>
      <button {...newProps} className={'flex justify-center items-center gap-1 py-2 px-2 rounded-md text-white font-bold bg-' + color + '-700 hover:bg-' + color + '-800 transition-colors duration-150'}>
        {children}
      </button>
    </>
  )
}

export default Input
