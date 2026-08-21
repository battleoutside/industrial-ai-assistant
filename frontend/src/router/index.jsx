import {createBrowserRouter} from 'react-router-dom';
import AppLayout from '../components/AppLayout';
import HomePage from '../pages/HomePage';
import QuotePage from '../pages/QuotePage';
import DesignGuidancePage from '../pages/DesignGuidancePage';
import IndustryIntelligencePage from '../pages/IndustryIntelligencePage';

/**
 * 前端路由配置。
 *
 * URL 与页面映射：
 * /                      -> 首页
 * /quote                 -> 智能辅助报价
 * /design-guidance       -> 工程设计指导
 * /industry-intelligence -> 行业信息咨询
 */
const router = createBrowserRouter([
    {
        path: '/',
        element: <AppLayout/>,
        children: [
            {index: true, element: <HomePage/>},
            {path: 'quote', element: <QuotePage/>},
            {path: 'design-guidance', element: <DesignGuidancePage/>},
            {path: 'industry-intelligence', element: <IndustryIntelligencePage/>},
        ],
    },
]);

export default router;
