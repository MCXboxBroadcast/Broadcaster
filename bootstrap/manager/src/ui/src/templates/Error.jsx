import { useRouteError } from 'react-router-dom'

function Error () {
  const error = useRouteError()
  console.error(error)

  return (
    <div className='h-screen flex flex-col space-y-5 text-center items-center justify-center'>
      <h1 className='text-4xl font-bold'>Oops!</h1>
      <p>Sorry, an unexpected error has occurred.</p>
      <p className='text-gray-400'>
        <i>{error.statusText || error.message}</i>
      </p>
    </div>
  )
}

export default Error
