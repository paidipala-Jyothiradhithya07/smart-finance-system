import client from './client';

export async function getTransactions(params = {}) {
  const response = await client.get('/api/transactions', { params });
  return response.data; // { content, page, size, totalElements, totalPages, last }
}

export async function getTransaction(id) {
  const response = await client.get(`/api/transactions/${id}`);
  return response.data;
}

export async function createTransaction(body) {
  const response = await client.post('/api/transactions', body);
  return response.data;
}

export async function updateTransaction(id, body) {
  const response = await client.put(`/api/transactions/${id}`, body);
  return response.data;
}

export async function deleteTransaction(id) {
  await client.delete(`/api/transactions/${id}`);
}

export default {
  getTransactions,
  getTransaction,
  createTransaction,
  updateTransaction,
  deleteTransaction,
};