import axios from 'axios';

/**
 * 行业信息咨询接口。
 */
export async function analyzeIndustryIntelligence(question) {
  const response = await axios.post('/api/industry-intelligence/analyze', {
    question,
  });

  return response.data;
}
