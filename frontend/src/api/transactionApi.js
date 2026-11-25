import { apiClient } from './client';

export const transactionApi = {
  list(params = {}) {
    const {
      page = 0,
      size = 20,
      sortBy = 'transactionDate',
      direction = 'DESC',
      from,
      to,
      categoryId,
      type,
      minAmount,
      maxAmount,
      q,
      source,
    } = params;

    return apiClient.get('/api/transactions', {
      params: { page, size, sortBy, direction, from, to, categoryId, type, minAmount, maxAmount, q, source },
    });
  },

  create(data) {
    return apiClient.post('/api/transactions', data);
  },

  update(id, data) {
    return apiClient.put(`/api/transactions/${id}`, data);
  },

  remove(id) {
    return apiClient.delete(`/api/transactions/${id}`);
  },
};

export default transactionApi;