import client from './client';

// Files go in a multipart form, as repeated parts named "files".
// No Content-Type header is set here: the browser adds it, including the multipart boundary.
function buildForm(files) {
  const form = new FormData();
  files.forEach((file) => form.append('files', file));
  return form;
}

export async function previewStatements(files) {
  const response = await client.post('/api/bank-statements/preview', buildForm(files));
  return response.data;
}

export async function uploadStatements(files) {
  const response = await client.post('/api/bank-statements/upload', buildForm(files));
  return response.data;
}

export async function getStatements() {
  const response = await client.get('/api/bank-statements');
  return response.data;
}

export async function deleteStatement(id) {
  await client.delete(`/api/bank-statements/${id}`);
}

export default { previewStatements, uploadStatements, getStatements, deleteStatement };
