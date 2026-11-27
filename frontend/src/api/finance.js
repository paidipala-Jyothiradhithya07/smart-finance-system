import client from './client';

export async function getMonthlySummary(params) {
  const response = await client.get('/api/finance/monthly-summary', { params });
  return response.data;
}

export async function getOverview(params) {
  const response = await client.get('/api/finance/overview', { params });
  return response.data;
}

export async function getCategorySummary(params) {
  const response = await client.get('/api/finance/category-summary', { params });
  return response.data;
}

const finance = {
  getMonthlySummary,
  getOverview,
  getCategorySummary,
};

export default finance;
