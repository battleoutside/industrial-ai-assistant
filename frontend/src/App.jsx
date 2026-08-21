import { RouterProvider } from 'react-router-dom';
import router from './router';

/**
 * React 应用根组件。
 * 当前只负责挂载前端路由。
 */
function App() {
  return <RouterProvider router={router} />;
}

export default App;
