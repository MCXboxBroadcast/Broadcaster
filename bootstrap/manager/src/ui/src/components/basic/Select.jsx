function Select (props) {
  const newProps = { ...props }
  const { label, id } = props
  delete newProps.label
  delete newProps.id

  return (
    <>
      <div className='relative'>
        <select {...newProps} className='peer w-full h-full bg-transparent outline outline-0 transition-colors border-b border-blue-gray-200 focus:border-gray-900 text-sm pt-4 pb-1.5'>
          {props.options.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        <label htmlFor={id} className='flex w-full h-full select-none pointer-events-none absolute left-0 leading-tight transition-colors -top-2.5 text-sm text-gray-500 peer-focus:text-gray-900'>{label}</label>
      </div>
    </>
  )
}

export default Select
