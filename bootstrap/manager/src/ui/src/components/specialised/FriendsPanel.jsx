import { useEffect, useState } from 'react'
import Friend from './Friend'
import Button from '../basic/Button'
import Input from '../basic/Input'
import { ChevronDoubleLeftIcon, ChevronDoubleRightIcon, ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/16/solid'

function FriendPanel ({ botId }) {
  const [friends, setFriends] = useState([])
  const [page, setPage] = useState(1)
  const [maxPage, setMaxPage] = useState(1)
  const [filteredFriends, setFilteredFriends] = useState([])
  const [query, setQuery] = useState('')

  const perPage = 10

  const updateData = () => {
    fetch('/api/bots/' + botId + '/friends').then((res) => res.json()).then((friendsData) => {
      setFriends(friendsData.filter(f => f.isFollowingCaller && f.isFollowedByCaller).sort((f1, f2) => f1.xuid - f2.xuid))
    })
  }

  useEffect(() => {
    updateData()
    const interval = setInterval(updateData, 2500) // Update every 2.5 seconds
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    setFilteredFriends(friends.filter(f => f.xuid.includes(query) || f.gamertag.toLowerCase().includes(query.toLowerCase())))
  }, [friends, query])

  useEffect(() => {
    changePage(0) // Make sure the page is still valid when friend data changes
  }, [filteredFriends])

  const changePage = (pageAdjustment) => {
    let newPage = page + pageAdjustment
    let maxPage = Math.floor(filteredFriends.length / perPage)

    // If there are any friends left over, add another page
    if (maxPage * perPage < filteredFriends.length) maxPage++

    // Make sure the page is within bounds
    if (newPage < 1) newPage = 1
    if (maxPage < 1) maxPage = 1
    if (newPage > maxPage) newPage = maxPage

    setPage(newPage)
    setMaxPage(maxPage)
  }

  return (
    <>
      <div className='bg-white p-6 rounded shadow-lg max-w-6xl w-full'>
        <h3 className='text-3xl text-center'>Friends</h3>
        <h4 className='text-xl text-center text-gray-400'>
          {friends.length}/1000
        </h4>
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder='Search'
          className='pb-4'
        />
        <div className='flex flex-col'>
          {filteredFriends.slice(perPage * (page - 1), perPage * page).map((friend, i) => (
            <Friend key={i} botId={botId} friend={friend} updateData={updateData} />
          ))}
          <div className='flex justify-center items-center gap-1'>
            <Button onClick={() => changePage(-maxPage)}><ChevronDoubleLeftIcon className='h-5 w-5' aria-hidden='true' /></Button>
            <Button onClick={() => changePage(-1)}><ChevronLeftIcon className='h-5 w-5' aria-hidden='true' /></Button>
            <div>
              {page} / {maxPage}
            </div>
            <Button onClick={() => changePage(1)}><ChevronRightIcon className='h-5 w-5' aria-hidden='true' /></Button>
            <Button onClick={() => changePage(maxPage)}><ChevronDoubleRightIcon className='h-5 w-5' aria-hidden='true' /></Button>
          </div>
        </div>
      </div>
    </>
  )
}

export default FriendPanel
