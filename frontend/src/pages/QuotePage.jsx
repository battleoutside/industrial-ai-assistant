import {useState} from 'react';
import {
    Alert,
    Button,
    Card,
    Col,
    Descriptions,
    Empty,
    Form,
    Input,
    InputNumber,
    List,
    Row,
    Select,
    Space,
    Spin,
    Tag,
    Typography,
} from 'antd';
import DemoSampleSelector from '../components/DemoSampleSelector';
import {generateQuote} from '../api/quoteApi';

const {Title, Paragraph, Text} = Typography;
const {TextArea} = Input;

const DECISION_TEXT = {
    DIRECT_REFERENCE: '可直接参考',
    ADJUSTED_REFERENCE: '调整后参考',
    MANUAL_REVIEW: '需要人工复核',
    NEED_MORE_INFO: '需要补充信息',
};

const CHANGE_LEVEL_TEXT = {
    NONE: '无明显变化',
    MINOR: '轻微变化',
    MEDIUM: '中等变化',
    MAJOR: '重大变化',
};

const CONFIDENCE_TEXT = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
};

const DIFFICULTY_TEXT = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难',
};

const QUOTE_SAMPLES = [
    {
        key: 'direct-reference',
        name: '样例1 · 历史案例直接报价',
        focus: ['历史检索', '直接参考', '三阶梯报价', 'Java Guard'],
        description:
            '验证系统能自动检索历史报价快照，并在物料方案无明显变化时完成自动报价。',
        values: {
            productModel: '高速互连线缆A型',
            productLengthMm: 500,
            manufacturingDifficulty: 'MEDIUM',
            lossRate: 5,
            requirementDescription:
                '标准连接器A，普通单层屏蔽，无特殊加工要求。',
        },
    },
    {
        key: 'explicit-added-cost',
        name: '样例2 · 明确新增物料成本',
        focus: ['物料差异分析', '明确成本', '调整后参考', 'Cost Guard'],
        description:
            '验证新增物料具有明确单件成本时，系统能够在历史成本基础上完成受控调整报价。',
        values: {
            productModel: '高速互连线缆A型',
            productLengthMm: 500,
            manufacturingDifficulty: 'MEDIUM',
            lossRate: 5,
            requirementDescription:
                '新增耐高温保护套管，单件材料成本5元；新增屏蔽胶带，单件材料成本3元。',
        },
    },
    {
        key: 'missing-cost',
        name: '样例3 · 新增物料价格缺失',
        focus: ['缺失价格', 'Fail-Safe', '人工复核'],
        description:
            '验证发生需要重新定价的物料变化、但缺少可靠当前价格时，系统停止自动报价并进入人工复核。',
        values: {
            productModel: '高速互连线缆A型',
            productLengthMm: 500,
            manufacturingDifficulty: 'HARD',
            lossRate: 5,
            requirementDescription:
                '新增耐高温保护套管，但当前尚未取得该物料价格。',
        },
    },
    {
        key: 'no-history',
        name: '样例4 · 无可用历史案例',
        focus: ['无历史锚点', '需要补充信息', 'Fail-Closed'],
        description:
            '验证没有可匹配历史报价快照时，系统不会凭空估价，而是返回信息不足。',
        values: {
            productModel: '工业机器人视觉模组线束Z型',
            productLengthMm: 2500,
            manufacturingDifficulty: 'MEDIUM',
            lossRate: 5,
            requirementDescription:
                '用于机器人视觉模组供电与控制信号连接，当前为全新产品方案。',
        },
    },
];

function QuotePage() {
    const [form] = Form.useForm();
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSampleSelect = (sample) => {
        form.setFieldsValue(sample.values);
        setResult(null);
        setError('');
    };

    const handleGenerate = async (values) => {
        setLoading(true);
        setError('');
        setResult(null);

        try {
            const data = await generateQuote({
                productModel: values.productModel.trim(),
                productLengthMm: values.productLengthMm,
                manufacturingDifficulty: values.manufacturingDifficulty,
                lossRate: values.lossRate / 100,
                requirementDescription: values.requirementDescription.trim(),
            });
            setResult(data);
        } catch {
            setError('智能辅助报价请求失败，请检查输入内容或后端服务状态。');
        } finally {
            setLoading(false);
        }
    };

    const renderDecision = (decision) => {
        const colorMap = {
            DIRECT_REFERENCE: 'success',
            ADJUSTED_REFERENCE: 'processing',
            MANUAL_REVIEW: 'error',
            NEED_MORE_INFO: 'warning',
        };

        return (
            <Tag color={colorMap[decision]}>
                {DECISION_TEXT[decision] || '未知状态'}
            </Tag>
        );
    };

    const rangePriceText = (low, high) => {
        if (low == null && high == null) return '后端未返回价格';
        if (low != null && high == null) return `¥ ${low}`;
        if (low == null && high != null) return `¥ ${high}`;
        if (Number(low) === Number(high)) return `¥ ${low}`;
        return `¥ ${low} ～ ${high}`;
    };

    const singlePriceText = (price) =>
        price == null ? '后端未返回价格' : `¥ ${price}`;


    return (
        <div className="page-wrap">
            <Tag color="gold">智能辅助报价</Tag>
            <Title>智能辅助报价</Title>
            <Paragraph type="secondary">
                输入最小报价信息，系统将结合历史报价、产品变化分析和确定性规则生成三阶梯报价。
            </Paragraph>

            <DemoSampleSelector
                samples={QUOTE_SAMPLES}
                onSelect={handleSampleSelect}
            />

            <Card className="module-card business-form-card" variant="borderless">
                <Form
                    form={form}
                    layout="vertical"
                    initialValues={{
                        manufacturingDifficulty: 'MEDIUM',
                        lossRate: 5,
                    }}
                    onFinish={handleGenerate}
                    requiredMark
                >
                    <Form.Item
                        label="产品型号 / 描述"
                        name="productModel"
                        rules={[{required: true, message: '请输入产品型号或描述'}]}
                        extra="例如：高速互连线缆A型"
                    >
                        <Input placeholder="请输入产品型号、内部料号或简要描述"/>
                    </Form.Item>

                    <Row gutter={16}>
                        <Col xs={24} md={8}>
                            <Form.Item
                                label="产品长度（毫米）"
                                name="productLengthMm"
                                rules={[
                                    {required: true, message: '请输入产品长度'},
                                    {
                                        validator: (_, value) =>
                                            value > 0
                                                ? Promise.resolve()
                                                : Promise.reject(new Error('产品长度必须大于0')),
                                    },
                                ]}
                            >
                                <InputNumber
                                    min={0.01}
                                    style={{width: '100%'}}
                                    placeholder="例如：500"
                                />
                            </Form.Item>
                        </Col>

                        <Col xs={24} md={8}>
                            <Form.Item
                                label="制造难度"
                                name="manufacturingDifficulty"
                                rules={[{required: true, message: '请选择制造难度'}]}
                                extra="请根据现有工艺经验选择制造难度。"
                            >
                                <Select
                                    options={[
                                        {value: 'EASY', label: '简单'},
                                        {value: 'MEDIUM', label: '中等'},
                                        {value: 'HARD', label: '困难'},
                                    ]}
                                />
                            </Form.Item>
                        </Col>

                        <Col xs={24} md={8}>
                            <Form.Item
                                label="损耗率（%）"
                                name="lossRate"
                                rules={[
                                    {required: true, message: '请输入损耗率'},
                                    {
                                        validator: (_, value) =>
                                            value >= 0 && value < 100
                                                ? Promise.resolve()
                                                : Promise.reject(new Error('损耗率必须在0%到100%之间')),
                                    },
                                ]}
                                extra="例如填写 5，提交时系统会自动转换为 0.05。"
                            >
                                <InputNumber
                                    min={0}
                                    max={99.99}
                                    step={0.1}
                                    style={{width: '100%'}}
                                />
                            </Form.Item>
                        </Col>
                    </Row>

                    <Form.Item
                        label="报价需求说明"
                        name="requirementDescription"
                        rules={[{required: true, message: '请输入报价需求说明'}]}
                        extra="只需描述当前产品要求和实际变化，不需要知道或引用历史案例。"
                    >
                        <TextArea
                            placeholder="例如：新增耐高温保护套管，单件材料成本5元。"
                            autoSize={{minRows: 4, maxRows: 8}}
                        />
                    </Form.Item>

                    <Button type="primary" htmlType="submit" loading={loading}>
                        生成报价
                    </Button>

                    {error && (
                        <div style={{marginTop: 16}}>
                            <Alert type="error" showIcon title={error}/>
                        </div>
                    )}
                </Form>
            </Card>

            <div style={{marginTop: 20}}>
                {loading && (
                    <Card variant="borderless">
                        <Spin/>
                        <Text style={{marginLeft: 12}}>
                            正在分析历史报价并计算三阶梯价格...
                        </Text>
                    </Card>
                )}

                {!loading && result && (
                    <Space orientation="vertical" size={18} style={{width: '100%'}}>
                        <Card variant="borderless">
                            <Space orientation="vertical" size={10}>
                                {renderDecision(result.decision)}
                                <Title level={3} style={{margin: 0}}>
                                    报价结论
                                </Title>
                                <Paragraph style={{margin: 0}}>
                                    {result.summary || '暂无摘要'}
                                </Paragraph>
                            </Space>
                        </Card>

                        <Alert
                            className="quote-reference-alert"
                            type="error"
                            showIcon
                            title="报价结果仅供参考"
                            description="最终价格请结合实际物料采购价格、工艺条件及内部审批结果进行确认。"
                        />

                        <Row gutter={[16, 16]}>
                            <Col xs={24} md={8}>
                                <Card title="样品报价（10PCS）">
                                    <Title level={3}>
                                        {singlePriceText(result.samplePrice)}
                                    </Title>
                                    <Space orientation="vertical" size={4}>
                                        <Text type="secondary">
                                            工时：{result.productionEfficiency == null ? "暂无数据" : `${result.productionEfficiency} PCS / H`}
                                        </Text>
                                        <Text type="secondary">成本加成率：100%</Text>
                                    </Space>
                                </Card>
                            </Col>

                            <Col xs={24} md={8}>
                                <Card title="小批量报价（1K PCS）">
                                    <Title level={3}>
                                        {rangePriceText(
                                            result.smallBatchPriceLow,
                                            result.smallBatchPriceHigh,
                                        )}
                                    </Title>
                                    <Space orientation="vertical" size={4}>
                                        <Text type="secondary">
                                            工时：{result.productionEfficiency == null ? "暂无数据" : `${result.productionEfficiency} PCS / H`}
                                        </Text>
                                        <Text type="secondary">
                                            成本加成率：40% ～ 45%
                                        </Text>
                                    </Space>
                                </Card>
                            </Col>

                            <Col xs={24} md={8}>
                                <Card title="量产报价（5K PCS）">
                                    <Title level={3}>
                                        {rangePriceText(
                                            result.massProductionPriceLow,
                                            result.massProductionPriceHigh,
                                        )}
                                    </Title>
                                    <Space orientation="vertical" size={4}>
                                        <Text type="secondary">
                                            工时：{result.productionEfficiency == null ? "暂无数据" : `${result.productionEfficiency} PCS / H`}
                                        </Text>
                                        <Text type="secondary">
                                            成本加成率：35% ～ 40%
                                        </Text>
                                    </Space>
                                </Card>
                            </Col>
                        </Row>

                        <Card title="报价依据" variant="borderless">
                            <Descriptions column={{xs: 1, md: 2}} bordered>
                                <Descriptions.Item label="参考历史案例">
                                    {result.referenceCase?.caseId || '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="变化程度">
                                    {CHANGE_LEVEL_TEXT[result.changeLevel] || '未确定'}
                                </Descriptions.Item>
                                <Descriptions.Item label="制造难度">
                                    {DIFFICULTY_TEXT[result.manufacturingDifficulty] || '未确定'}
                                </Descriptions.Item>
                                <Descriptions.Item label="分析可信度">
                                    {CONFIDENCE_TEXT[result.confidence] || '未确定'}
                                </Descriptions.Item>
                                <Descriptions.Item label="历史物料成本">
                                    {result.historicalMaterialCost ?? '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="当前物料成本">
                                    {result.currentMaterialCost ?? '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="材料成本保底">
                                    {result.materialCostFloor ?? '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="是否触发保底">
                                    {result.materialCostFloorTriggered ? '是' : '否'}
                                </Descriptions.Item>
                                <Descriptions.Item label="生产效率">
                                    {result.productionEfficiency != null
                                        ? `${result.productionEfficiency} PCS/小时`
                                        : '-'}
                                </Descriptions.Item>
                                <Descriptions.Item label="单件人工成本">
                                    {result.laborCost ?? '-'}
                                </Descriptions.Item>
                            </Descriptions>
                        </Card>

                        <Card
                            title={`风险提示（${result.risks?.length || 0}）`}
                            variant="borderless"
                        >
                            {result.risks?.length ? (
                                <List
                                    dataSource={result.risks}
                                    renderItem={(item) => <List.Item>{item}</List.Item>}
                                />
                            ) : (
                                <Empty description="暂无风险提示"/>
                            )}
                        </Card>

                        <Card
                            title={`缺失信息（${result.missingInformation?.length || 0}）`}
                            variant="borderless"
                        >
                            {result.missingInformation?.length ? (
                                <List
                                    dataSource={result.missingInformation}
                                    renderItem={(item) => <List.Item>{item}</List.Item>}
                                />
                            ) : (
                                <Empty description="暂无缺失信息"/>
                            )}
                        </Card>

                        {result.reviewSuggestion && (
                            <Card title="复核建议" variant="borderless">
                                <Paragraph style={{margin: 0}}>
                                    {result.reviewSuggestion}
                                </Paragraph>
                            </Card>
                        )}

                        {result.agentSteps?.length > 0 && (
                            <Card title="执行步骤" variant="borderless">
                                <List
                                    dataSource={result.agentSteps}
                                    renderItem={(item, index) => (
                                        <List.Item>
                                            <Text>
                                                {index + 1}. {item}
                                            </Text>
                                        </List.Item>
                                    )}
                                />
                            </Card>
                        )}
                    </Space>
                )}
            </div>
        </div>
    );
}

export default QuotePage;
