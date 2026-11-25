import { apiClient } from './client';

export const financeApi = {
  monthlySummary(from, to) {
    return apiClient.get('/api/finance/monthly-summary', {
      params: { from, to },
    });
  },

  overview(from, to) {
    return apiClient.get('/api/finance/overview', {
      params: { from, to },
    });
  },
};

export default financeApi;