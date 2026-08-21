import axios from 'axios';

/**
 * 工程设计指导接口。
 */
export async function analyzeDesignGuidance(payload) {
  const response = await axios.post('/api/design-guidance/analyze', payload);
  return response.data;
}
