import { apiClient } from './client';

export const statementApi = {
  upload(files) {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return apiClient.post('/api/bank-statements/upload', formData);
  },

  list() {
    return apiClient.get('/api/bank-statements');
  },

  remove(id) {
    return apiClient.delete(`/api/bank-statements/${id}`);
  },
};

export default statementApi;