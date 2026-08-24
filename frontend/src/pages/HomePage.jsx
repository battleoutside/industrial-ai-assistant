import {
  ArrowRightOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  Col,
  Collapse,
  Row,
  Space,
  Tag,
  Typography,
} from 'antd';
import { useNavigate } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

/**
 * 工作台首页。
 * 首页只保留品牌、简短定位和三大模块入口，
 * 具体技术能力放到各模块卡片中展示。
 */
function HomePage() {
  const navigate = useNavigate();

  const modules = [
    {
      key: 'quote',
      title: '智能辅助报价',
      englishTitle: 'Quote Assistance',
      description: '基于历史案例与规则生成三阶梯报价。',
      path: '/quote',
      tags: ['Workflow', 'LLM Analysis', 'Java Guard'],
      detail:
        '模型用于识别需求变化，Java 负责确定性成本计算、报价边界与降级控制，最终输出样品、小批量和量产三档结果。',
      accentClass: 'accent-blue',
    },
    {
      key: 'guidance',
      title: '工程设计指导',
      englishTitle: 'Engineering Design Guidance',
      description: '基于内部知识库提供可追溯的工程建议。',
      path: '/design-guidance',
      tags: ['RAG', 'Claim Groundedness', 'Fail-Closed'],
      detail:
        '先检索内部工程资料，再生成指导建议，并通过引用校验与 Claim Groundedness 审核控制事实边界。',
      accentClass: 'accent-cyan',
    },
    {
      key: 'industry',
      title: '行业信息咨询',
      englishTitle: 'Industry Intelligence',
      description: '聚合高速互连领域公开信息，快速掌握企业、产品、技术与产业动态。',
      path: '/industry-intelligence',
      tags: ['Agent', 'MCP', 'Tool Calling'],
      detail:
        'Agent 根据用户的信息需求选择搜索方向，通过 MCP Web Search 获取公开信息，并输出带来源的结构化信息汇总。',
      accentClass: 'accent-violet',
    },
  ];

  return (
    <div className="page-wrap home-page">
      <section className="home-hero">
        <div className="hero-glow hero-glow-one" />
        <div className="hero-glow hero-glow-two" />

        <div className="hero-content">
          <Title className="hero-title">
            工业 AI 助手
          </Title>

          <Text className="hero-product-en">
            Industrial AI Assistant
          </Text>

          <Paragraph className="hero-description">
            报价、工程指导与行业信息，一站式辅助决策。
          </Paragraph>
        </div>
      </section>

      <div className="section-header">
        <div>
          <Text className="section-kicker">CAPABILITIES</Text>
          <Title level={2} className="section-title">
            核心模块
          </Title>
        </div>

        <Text className="section-helper">
          选择一个模块开始工作
        </Text>
      </div>

      <Row gutter={[20, 20]}>
        {modules.map((module) => (
          <Col xs={24} xl={8} key={module.key}>
            <Card
              className={`module-card ${module.accentClass}`}
              variant="borderless"
            >
              <div className="module-top">
                <div>
                  <Text className="module-english-title">
                    {module.englishTitle}
                  </Text>

                  <Title level={3} className="module-title">
                    {module.title}
                  </Title>
                </div>

                <div className="module-status-dot" />
              </div>

              <Paragraph className="module-description">
                {module.description}
              </Paragraph>

              <Space wrap size={[7, 7]} className="module-tags">
                {module.tags.map((tag) => (
                  <Tag key={tag}>{tag}</Tag>
                ))}
              </Space>

              <div className="module-actions">
                <Button
                  type="primary"
                  onClick={() => navigate(module.path)}
                >
                  进入模块
                  <ArrowRightOutlined />
                </Button>

                <Collapse
                  ghost
                  className="module-collapse"
                  items={[
                    {
                      key: 'detail',
                      label: (
                        <span className="detail-label">
                          <InfoCircleOutlined />
                          查看说明
                        </span>
                      ),
                      children: (
                        <Paragraph className="detail-text">
                          {module.detail}
                        </Paragraph>
                      ),
                    },
                  ]}
                />
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}

export default HomePage;
