import { apiClient } from './client';

export const categoryApi = {
  list() {
    return apiClient.get('/api/categories');
  },

  create(data) {
    return apiClient.post('/api/categories', data);
  },

  update(id, data) {
    return apiClient.put(`/api/categories/${id}`, data);
  },

  delete(id) {
    return apiClient.delete(`/api/categories/${id}`);
  },
};

export default categoryApi;