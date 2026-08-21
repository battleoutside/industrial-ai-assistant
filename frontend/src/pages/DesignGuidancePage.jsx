import { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  List,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import DemoSampleSelector from '../components/DemoSampleSelector';
import { analyzeDesignGuidance } from '../api/designGuidanceApi';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

const STATUS_CONFIG = {
  GUIDANCE_PROVIDED: {
    type: 'success',
    title: '分析完成',
    description: '已基于知识库证据生成工程指导建议，并完成引用与 Groundedness 校验。',
  },
  SUCCESS: {
    type: 'success',
    title: '分析完成',
    description: '已基于知识库证据生成工程指导建议，并完成引用与 Groundedness 校验。',
  },
  NEED_MORE_INFO: {
    type: 'warning',
    title: '需要补充信息',
    description: '当前证据可以提供排查方向，但关键测试条件仍不完整，请先补充下方信息。',
  },
  NO_RELEVANT_EVIDENCE: {
    type: 'info',
    title: '暂无相关证据',
    description: '知识库未检索到足够相关的工程资料，本次未调用大模型生成工程建议。',
  },
  MANUAL_REVIEW: {
    type: 'error',
    title: '需要人工复核',
    description: '自动生成或证据校验链路未满足安全要求，请转人工工程评审。',
  },
};

const DESIGN_SAMPLES = [
  {
    key: 'need-more-info',
    name: '样例1 · 信息不足辅助排查',
    focus: ['RAG', '需要补充信息', 'Missing Information'],
    description:
      '验证知识库能够给出标准化排查方向，同时在校准、参考平面和治具信息不足时主动要求补充条件。',
    values: {
      productDescription:
        'PCIe Gen 6 高速线缆组件，长度500mm，双端连接器，用于服务器高速互连。',
      engineeringStage: 'TEST',
      question:
        '高速线缆测试中出现插损异常，应该优先排查哪些环节？',
      knownConditions:
        '使用VNA进行测试，连接器已重新插拔，异常主要出现在高频段，目前尚未确认校准状态、参考平面和测试治具是否一致。',
    },
  },
  {
    key: 'grounded-guidance',
    name: '样例2 · 证据充分生成指导',
    focus: ['RAG', 'Citation', 'Claim Groundedness'],
    description:
      '该样例与当前知识库中的“机械结构对电气性能影响、连接稳定性”内容直接对应，用于验证完整的检索、生成、引用和 Groundedness 审核链路。',
    values: {
      productDescription:
        '高速差分线缆组件，双端连接器，用于服务器内部高速互连。',
      engineeringStage: 'TEST',
      question:
        '样品在弯折线缆、晃动连接器或重新装配后高频性能出现波动，应该优先检查哪些机械结构和连接稳定性因素？',
      knownConditions:
        '测试系统已完成校准，参考平面和测试治具保持一致；异常与弯折、晃动连接器或重新装配动作具有明显相关性。',
    },
  },
  {
    key: 'outside-knowledge',
    name: '样例3 · 无相关知识证据',
    focus: ['无相关证据', 'Fail-Closed', '不调用LLM'],
    description:
      '该样例故意超出当前高速线缆工程知识库范围。若系统快速返回“暂无相关证据”，属于预期行为，用于展示 Fail-Closed 边界。',
    values: {
      productDescription: '工业机器人视觉检测系统',
      engineeringStage: 'DESIGN',
      question:
        '如何选择目标检测模型并设计GPU推理集群？',
      knownConditions:
        '目前没有确定模型架构和算力平台。',
    },
  },
];

function DesignGuidancePage() {
  const [form] = Form.useForm();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSampleSelect = (sample) => {
    form.setFieldsValue(sample.values);
    setResult(null);
    setError('');
  };

  const handleAnalyze = async (values) => {
    setLoading(true);
    setError('');
    setResult(null);

    try {
      const data = await analyzeDesignGuidance({
        productDescription: values.productDescription.trim(),
        engineeringStage: values.engineeringStage,
        question: values.question.trim(),
        knownConditions: values.knownConditions?.trim() || '',
      });
      setResult(data);
    } catch {
      setError('工程设计指导请求失败，请检查输入内容或后端服务状态。');
    } finally {
      setLoading(false);
    }
  };

  const statusConfig =
    result && STATUS_CONFIG[result.status]
      ? STATUS_CONFIG[result.status]
      : {
          type: 'info',
          title: '未知状态',
          description: '后端返回了未识别的业务状态。',
        };

  const hasRecommendations = result?.recommendations?.length > 0;
  const hasRisks = result?.risks?.length > 0;
  const hasMissingInformation = result?.missingInformation?.length > 0;
  const hasSources = result?.sources?.length > 0;

  return (
    <div className="page-wrap">
      <Tag color="green">工程设计指导</Tag>
      <Title>工程设计指导</Title>
      <Paragraph type="secondary">
        输入产品背景、工程阶段和问题，系统将基于内部知识库检索并生成带证据约束的建议。
      </Paragraph>

      <DemoSampleSelector
        samples={DESIGN_SAMPLES}
        onSelect={handleSampleSelect}
      />

      <Card className="module-card business-form-card" variant="borderless">
        <Form
          form={form}
          layout="vertical"
          initialValues={{ engineeringStage: 'TEST' }}
          onFinish={handleAnalyze}
          requiredMark
        >
          <Form.Item
            label="产品描述"
            name="productDescription"
            rules={[
              { required: true, message: '请输入产品描述' },
              { max: 500, message: '产品描述不能超过500个字符' },
            ]}
            extra="例如：PCIe Gen 6 高速线缆组件，长度500mm"
          >
            <Input
              placeholder="请输入产品类型、结构、型号或关键规格"
              maxLength={500}
            />
          </Form.Item>

          <Form.Item
            label="工程阶段"
            name="engineeringStage"
            rules={[{ required: true, message: '请选择工程阶段' }]}
          >
            <Select
              placeholder="请选择当前工程阶段"
              options={[
                { value: 'DESIGN', label: '概念 / 架构设计阶段' },
                { value: 'PROTOTYPE', label: '样机 / 原型验证阶段' },
                { value: 'TEST', label: '测试 / 可靠性验证阶段' },
                { value: 'MASS_PRODUCTION', label: '量产导入阶段' },
              ]}
            />
          </Form.Item>

          <Form.Item
            label="工程问题"
            name="question"
            rules={[
              { required: true, message: '请输入工程问题' },
              { max: 1000, message: '工程问题不能超过1000个字符' },
            ]}
          >
            <TextArea
              placeholder="请输入需要解决的工程问题"
              autoSize={{ minRows: 3, maxRows: 6 }}
              maxLength={1000}
              showCount
            />
          </Form.Item>

          <Form.Item
            label="已知条件（可选）"
            name="knownConditions"
            rules={[{ max: 1000, message: '已知条件不能超过1000个字符' }]}
          >
            <TextArea
              placeholder="可填写材料、结构、测试条件或异常现象"
              autoSize={{ minRows: 3, maxRows: 6 }}
              maxLength={1000}
              showCount
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" loading={loading}>
            开始分析
          </Button>

          {error && (
            <div style={{ marginTop: 16 }}>
              <Alert type="error" showIcon title={error} />
            </div>
          )}
        </Form>
      </Card>

      <div style={{ marginTop: 20 }}>
        {loading && (
          <Card variant="borderless">
            <Spin />
            <Text style={{ marginLeft: 12 }}>
              正在检索知识库并生成工程建议...
            </Text>
          </Card>
        )}

        {!loading && result && (
          <Space orientation="vertical" size={18} style={{ width: '100%' }}>
            <Alert
              type={statusConfig.type}
              showIcon
              title={
                <span style={{ fontSize: 18, fontWeight: 700 }}>
                  {statusConfig.title}
                </span>
              }
              description={
                <span style={{ fontSize: 14 }}>
                  {statusConfig.description}
                </span>
              }
              style={{
                padding: '18px 20px',
                borderRadius: 14,
              }}
            />

            <Card variant="borderless">
              <Title level={3} style={{ marginTop: 0 }}>
                分析摘要
              </Title>
              <Paragraph style={{ marginBottom: 0 }}>
                {result.summary || '暂无摘要'}
              </Paragraph>
            </Card>

            {hasRecommendations && (
              <Card
                title={`指导建议（${result.recommendations.length}）`}
                variant="borderless"
              >
                <List
                  dataSource={result.recommendations}
                  renderItem={(item) => (
                    <List.Item>
                      <List.Item.Meta
                        title={`${item.step}. ${item.action}`}
                        description={
                          <Space wrap>
                            {(item.basisSourceIds || []).map((sourceId) => (
                              <Tag key={sourceId}>{sourceId}</Tag>
                            ))}
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              </Card>
            )}

            {hasRisks && (
              <Card
                title={`风险提示（${result.risks.length}）`}
                variant="borderless"
              >
                <List
                  dataSource={result.risks}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>{item}</Text>
                    </List.Item>
                  )}
                />
              </Card>
            )}

            {hasMissingInformation && (
              <Card
                title={`待补充信息（${result.missingInformation.length}）`}
                variant="borderless"
              >
                <List
                  dataSource={result.missingInformation}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>{item}</Text>
                    </List.Item>
                  )}
                />
              </Card>
            )}

            {hasSources && (
              <Card
                title={`证据来源（${result.evidenceCount || result.sources.length}）`}
                variant="borderless"
              >
                <List
                  dataSource={result.sources}
                  renderItem={(source) => (
                    <List.Item>
                      <Space orientation="vertical" size={4}>
                        <Space wrap>
                          <Tag>{source.sourceId}</Tag>
                          <Text strong>{source.title}</Text>
                        </Space>

                        {source.section && (
                          <Text type="secondary">
                            章节：{source.section}
                          </Text>
                        )}

                        <Paragraph style={{ margin: 0 }}>
                          {source.excerpt}
                        </Paragraph>
                      </Space>
                    </List.Item>
                  )}
                />
              </Card>
            )}

            {!hasRecommendations &&
              !hasRisks &&
              !hasMissingInformation &&
              !hasSources && (
                <Card variant="borderless">
                  <Empty description="本次没有生成可展示的工程建议或证据" />
                </Card>
              )}
          </Space>
        )}
      </div>
    </div>
  );
}

export default DesignGuidancePage;
