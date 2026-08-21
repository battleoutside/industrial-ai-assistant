import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  List,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import DemoSampleSelector from '../components/DemoSampleSelector';
import { analyzeIndustryIntelligence } from '../api/industryIntelligenceApi';

const { Title, Paragraph, Text, Link } = Typography;
const { TextArea } = Input;

const STATUS_TEXT = {
  SUCCESS: '信息收集完成',
  NO_RELEVANT_INFORMATION: '暂无相关信息',
  SEARCH_FAILED: '检索失败',
};

const INDUSTRY_SAMPLES = [
  {
    key: 'industry-overview',
    name: '样例1 · 宽泛行业检索边界',
    focus: ['Broad Query', 'MCP Boundary', 'Agent Loop', 'Fail-Closed', 'Source Validation'],
    description:
      '验证宽泛行业信息需求下的 MCP 检索边界。该场景不要求固定成功：若搜索结果足以形成可核验信息，则正常汇总；若证据不足，则应返回“暂无相关信息”，展示 Agent 搜索、来源校验与安全降级能力。',
    values: {
      question:
        '汇总近期高速线缆、高速连接器与高速互连领域值得关注的公开动态，重点关注主要厂商的新产品、技术发布和合作动态。',
    },
  },
  {
    key: 'company-product-news',
    name: '样例2 · 企业与产品动态收集',
    focus: ['企业动态', '产品发布', '应用方向', '来源引用'],
    description:
      '验证系统能围绕明确厂商收集其高速线缆、高速连接器及高速互连解决方案的新品、合作、客户应用和技术发布。',
    values: {
      question:
        '汇总近期安费诺、Molex、TE Connectivity在AI服务器和数据中心高速互连领域的公开动态，重点关注新品、合作、应用方向和技术发布。',
    },
  },
  {
    key: 'technology-standard-news',
    name: '样例3 · 技术与标准动态收集',
    focus: ['高速线缆', '接口标准', '技术资料', '厂商方案'],
    description:
      '验证系统能围绕高速线缆及高速互连技术收集标准演进、接口规范、技术文章和厂商方案，体现技术信息获取能力。',
    values: {
      question:
        '收集近期PCIe、CXL、SAS、USB4等高速接口相关的线缆、连接器和互连技术公开动态，包括标准进展、技术资料和厂商方案。',
    },
  },
  {
    key: 'industry-chain-news',
    name: '样例4 · 产业链动态收集',
    focus: ['产业链', '扩产', '合作并购', '产能布局', '公开来源'],
    description:
      '验证系统能围绕高速线缆与高速连接器产业链，收集相关企业的扩产、新建产线、合作、并购和产能布局等公开动态，为后续人工判断提供产业信息底稿。',
    values: {
      question:
        '汇总近期高速线缆、高速连接器产业链相关企业的扩产、新建产线、合作、并购和产能布局等公开动态。',
    },
  },
];

function IndustryIntelligencePage() {
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeSampleKey, setActiveSampleKey] = useState(null);

  const handleSampleSelect = (sample) => {
    setQuestion(sample.values.question);
    setActiveSampleKey(sample.key);
    setResult(null);
    setError('');
  };

  const handleAnalyze = async () => {
    if (!question.trim()) {
      setError('请输入需要收集的行业信息。');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const data = await analyzeIndustryIntelligence(question.trim());
      setResult(data);
    } catch {
      setError('行业信息咨询请求失败，请确认后端服务已启动。');
    } finally {
      setLoading(false);
    }
  };

  const renderStatus = (status) => {
    const colorMap = {
      SUCCESS: 'success',
      NO_RELEVANT_INFORMATION: 'warning',
      SEARCH_FAILED: 'error',
    };

    return (
      <Tag color={colorMap[status]}>
        {STATUS_TEXT[status] || '未知状态'}
      </Tag>
    );
  };

  return (
    <div className="page-wrap">
      <Tag color="purple">行业信息咨询</Tag>
      <Title>行业信息咨询</Title>

      <Paragraph type="secondary">
        聚合高速线缆与高速互连领域公开信息，快速掌握企业、产品、技术与产业动态。
      </Paragraph>

      <DemoSampleSelector
        samples={INDUSTRY_SAMPLES}
        onSelect={handleSampleSelect}
      />

      <Card className="module-card business-form-card" variant="borderless">
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <TextArea
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="例如：汇总近期高速线缆与高速互连行业值得关注的企业、产品、技术和产业动态。"
            autoSize={{ minRows: 4, maxRows: 8 }}
          />

          <Button
            type="primary"
            loading={loading}
            onClick={handleAnalyze}
          >
            开始收集
          </Button>

          {error && (
            <Alert
              type="error"
              showIcon
              title={error}
            />
          )}
        </Space>
      </Card>

      <div style={{ marginTop: 20 }}>
        {loading && (
          <Card variant="borderless">
            <Spin />
            <Text style={{ marginLeft: 12 }}>
              Agent 正在分步搜索并汇总公开行业信息...
            </Text>
          </Card>
        )}

        {!loading && result && (
          <Space
            orientation="vertical"
            size={18}
            style={{ width: '100%' }}
          >
            {activeSampleKey === 'industry-overview'
              && result.status === 'NO_RELEVANT_INFORMATION' && (
                <Alert
                  type="warning"
                  showIcon
                  title="宽泛检索边界已触发"
                  description="Agent 已尝试通过 MCP Web Search 获取公开信息，但最终结果不足以通过来源与引用校验。系统因此停止生成行业结论并安全降级，而不是使用模型记忆补全事实。"
                />
              )}

            <Card variant="borderless">
              <Space orientation="vertical" size={10}>
                {renderStatus(result.status)}

                <Title level={3} style={{ margin: 0 }}>
                  信息概览
                </Title>

                <Paragraph style={{ margin: 0 }}>
                  {result.summary || '暂无概览'}
                </Paragraph>
              </Space>
            </Card>

            <Card
              title={`信息汇总（${result.findings?.length || 0}）`}
              variant="borderless"
            >
              {result.findings?.length ? (
                <List
                  dataSource={result.findings}
                  renderItem={(item) => (
                    <List.Item>
                      <List.Item.Meta
                        title={item.title}
                        description={
                          <Space
                            orientation="vertical"
                            size={8}
                            style={{ width: '100%' }}
                          >
                            <Text>{item.content}</Text>

                            <Space wrap>
                              {(item.sourceIds || []).map((sourceId) => (
                                <Tag key={sourceId}>
                                  {sourceId}
                                </Tag>
                              ))}
                            </Space>
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              ) : (
                <Empty description="本次未形成通过来源校验的有效信息条目" />
              )}
            </Card>

            <Card
              title={`公开来源（${result.sourceCount || 0}）`}
              variant="borderless"
            >
              {result.sources?.length ? (
                <List
                  dataSource={result.sources}
                  renderItem={(source) => (
                    <List.Item>
                      <Space orientation="vertical" size={4}>
                        <Space wrap>
                          <Tag>{source.sourceId}</Tag>
                          <Text strong>{source.title}</Text>
                        </Space>

                        <Link
                          href={source.url}
                          target="_blank"
                          rel="noreferrer"
                        >
                          {source.url}
                        </Link>

                        {source.publishedAt && (
                          <Text type="secondary">
                            发布时间：{source.publishedAt}
                          </Text>
                        )}
                      </Space>
                    </List.Item>
                  )}
                />
              ) : (
                <Empty description="暂无可用来源" />
              )}
            </Card>
          </Space>
        )}
      </div>
    </div>
  );
}

export default IndustryIntelligencePage;
