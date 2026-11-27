import client from './client';

// type is optional: "INCOME" or "EXPENSE". Without it, every category is returned once.
export async function getCategories(type) {
  const response = await client.get('/api/categories', { params: type ? { type } : {} });
  return response.data;
}

export default { getCategories };