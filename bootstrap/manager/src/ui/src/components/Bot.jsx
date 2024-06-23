import { CheckCircleIcon, EllipsisHorizontalCircleIcon, QuestionMarkCircleIcon, StopCircleIcon } from '@heroicons/react/16/solid'

function Bot ({ bot, onClick }) {
  const statusIcon = (status) => {
    const classes = 'ml-6 size-8'
    switch (status.toLowerCase()) {
      case 'online':
        return <CheckCircleIcon className={classes + ' text-green-600'} aria-hidden='true' />
      case 'offline':
        return <StopCircleIcon className={classes + ' text-red-600'} aria-hidden='true' />
      case 'starting':
        return <EllipsisHorizontalCircleIcon className={classes + ' text-orange-600'} aria-hidden='true' />
      default:
        return <QuestionMarkCircleIcon className={classes + ' text-gray-600'} aria-hidden='true' />
    }
  }

  return (
    <>
      <div className='px-8 pb-6 flex justify-center'>
        <div className={'bg-white rounded overflow-hidden max-w-2xl w-full shadow-lg flex flex-col md:flex-row' + (onClick ? ' cursor-pointer' : '')} onClick={onClick}>
          <div className='flex items-center h-full relative'>
            {statusIcon(bot.status)}
            <div className='p-6'>
              <h3 className='text-3xl'>{bot.gamertag || 'Unknown'}<small className='text-base text-gray-400'> {bot.xid}</small></h3>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

export default Bot
