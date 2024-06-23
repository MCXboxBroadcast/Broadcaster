import { Navigate, RouterProvider, createBrowserRouter } from 'react-router-dom'

import Error from './templates/Error'
import Layout from './templates/Layout'

import Todo from './pages/Todo'
import Login from './pages/Login'
import Bots from './pages/Bots'
import BotDetails from './pages/BotDetails'
import Servers from './pages/Servers'
import ServerDetails from './pages/ServerDetails'

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    errorElement: <Error />,
    children: [
      {
        index: true,
        element: <Navigate to='/bots' replace />,
        handle: {
          title: () => 'Bots'
        }
      },
      {
        path: 'bots',
        children: [
          {
            index: true,
            element: <Bots />,
            handle: {
              title: () => 'Bots'
            }
          },
          {
            path: ':botId',
            element: <BotDetails />,
            handle: {
              title: () => 'Bot info'
            }
          }
        ]
      },
      {
        path: 'servers',
        children: [
          {
            index: true,
            element: <Servers />,
            handle: {
              title: () => 'Servers'
            }
          },
          {
            path: ':serverId',
            element: <ServerDetails />,
            handle: {
              title: () => 'Server info'
            }
          }
        ]
      },
      {
        path: 'settings',
        element: <Todo />,
        handle: {
          title: () => 'Settings'
        }
      },
      {
        path: 'login',
        element: <Login />,
        handle: {
          title: () => 'Login',
          hideHeader: () => true,
          hideFooter: () => true
        }
      }
    ]
  }
])

function App () {
  return <RouterProvider router={router} fallbackElement={<p>Loading...</p>} />
}

export default App
