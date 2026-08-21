import axios from 'axios';

/**
 * 智能辅助报价接口。
 */
export async function generateQuote(payload) {
  const response = await axios.post('/api/quote/generate', payload);
  return response.data;
}
