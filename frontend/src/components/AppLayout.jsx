import {
  AppstoreOutlined,
  HomeOutlined,
  LineChartOutlined,
  SettingOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Typography } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const { Sider, Header, Content } = Layout;
const { Text } = Typography;

/**
 * 应用公共布局。
 * 品牌名称统一使用“中文主名称 + 英文副名称”的层级。
 */
function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: '工作台首页',
    },
    {
      key: '/quote',
      icon: <LineChartOutlined />,
      label: '智能辅助报价',
    },
    {
      key: '/design-guidance',
      icon: <ToolOutlined />,
      label: '工程设计指导',
    },
    {
      key: '/industry-intelligence',
      icon: <AppstoreOutlined />,
      label: '行业信息咨询',
    },
  ];

  const pageNameMap = {
    '/': '工作台首页',
    '/quote': '智能辅助报价',
    '/design-guidance': '工程设计指导',
    '/industry-intelligence': '行业信息咨询',
  };

  const currentPage = pageNameMap[location.pathname] ?? '工作台';
  const selectedKey =
    menuItems.find((item) => item.key === location.pathname)?.key ?? '/';

  return (
    <Layout className="app-shell">
      <Sider
        width={232}
        breakpoint="lg"
        collapsedWidth="0"
        className="app-sider"
      >
        <div className="brand">
          <div className="brand-icon">
            <SettingOutlined />
          </div>

          <div className="brand-copy">
            <div className="brand-title">工业 AI 助手</div>
            <div className="brand-subtitle">Industrial AI Assistant</div>
          </div>
        </div>

        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          className="app-menu"
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      <Layout className="main-layout">
        <Header className="app-header">
          <div className="breadcrumb-wrap">
            <Text className="breadcrumb-root">工业 AI 助手</Text>
            <span className="breadcrumb-divider">/</span>
            <Text className="breadcrumb-current">{currentPage}</Text>
          </div>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default AppLayout;
