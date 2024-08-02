function Banner (props, ref) {
  const newProps = { ...props }
  const { color, width, children, className } = props
  delete newProps.color
  delete newProps.width
  delete newProps.children
  delete newProps.className

  return (
    <div {...newProps} className={'flex justify-center ' + className}>
      <div className={'max-w-' + width + ' w-full bg-' + color + '-300 p-2 rounded text-' + color + '-600'}>
        {children}
      </div>
    </div>
  )
}

export default Banner
