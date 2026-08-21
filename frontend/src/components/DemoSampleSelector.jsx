import { Card, Select, Space, Tag, Typography } from 'antd';
import { useState } from 'react';

const { Paragraph, Text } = Typography;

/**
 * 演示样例选择器。
 * 选择样例后只负责自动填充表单，用户仍可继续修改内容后再提交真实业务接口。
 */
function DemoSampleSelector({ samples, onSelect }) {
  const [selectedKey, setSelectedKey] = useState();
  const selectedSample = samples.find((sample) => sample.key === selectedKey);

  const handleChange = (key) => {
    setSelectedKey(key);
    const sample = samples.find((item) => item.key === key);
    if (sample) {
      onSelect(sample);
    }
  };

  return (
    <Card className="demo-sample-panel" variant="borderless">
      <div className="demo-sample-header">
        <div>
          <Text className="demo-sample-title">演示样例</Text>
          <Text className="demo-sample-subtitle">
            选择后自动填充，仍可手动修改
          </Text>
        </div>

        <Select
          value={selectedKey}
          onChange={handleChange}
          placeholder="请选择测试场景"
          className="demo-sample-select"
          options={samples.map((sample) => ({
            value: sample.key,
            label: sample.name,
          }))}
        />
      </div>

      {selectedSample && (
        <div className="demo-sample-detail">
          <div className="demo-sample-focus-row">
            <Text className="demo-sample-focus-label">测试重点</Text>
            <Space wrap size={[6, 6]}>
              {selectedSample.focus.map((item) => (
                <Tag key={item}>{item}</Tag>
              ))}
            </Space>
          </div>

          <Paragraph className="demo-sample-description">
            {selectedSample.description}
          </Paragraph>
        </div>
      )}
    </Card>
  );
}

export default DemoSampleSelector;
