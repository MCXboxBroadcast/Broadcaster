import { formatTimestamp } from '../utils'

function Server ({ server, onClick }) {
  const formattedTimestamp = formatTimestamp(server.lastUpdated)

  return (
    <>
      <div className='px-8 pb-6 flex justify-center'>
        <div className={'bg-white rounded overflow-hidden max-w-2xl w-full shadow-lg flex flex-col md:flex-row' + (onClick ? ' cursor-pointer' : '')} onClick={onClick}>
          <div className='p-6'>
            <h3 className='text-3xl'>{server.hostname}:{server.port}<small className='text-base text-gray-400' title={formattedTimestamp.timestamp}> {formattedTimestamp.timestampAgo}</small></h3>
          </div>
        </div>
      </div>
    </>
  )
}

export default Server
