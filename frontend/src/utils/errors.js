export function getErrorMessage(error) {
  if (!error || !error.response) {
    return 'Cannot reach the server. Check that the backend is running.';
  }

  const data = error.response.data;
  if (data && typeof data === 'object' && typeof data.message === 'string' && data.message.trim()) {
    return data.message;
  }

  if (typeof data === 'string' && data.trim()) {
    return data;
  }

  return error.message || 'An unexpected error occurred.';
}

export function getFieldErrors(error) {
  const fieldErrors = error?.response?.data?.fieldErrors;
  if (fieldErrors && typeof fieldErrors === 'object' && !Array.isArray(fieldErrors)) {
    return fieldErrors;
  }
  return {};
}
