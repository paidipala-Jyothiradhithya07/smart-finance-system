export function getErrorMessage(err) {
  if (!err) {
    return 'An unknown error occurred.';
  }
  if (!err.response) {
    return 'Cannot reach the server. Check that the backend is running.';
  }
  return err.response.data?.message || err.message || 'An unexpected error occurred.';
}

export function getFieldErrors(err) {
  if (err?.response?.data?.fieldErrors && typeof err.response.data.fieldErrors === 'object') {
    return err.response.data.fieldErrors;
  }
  return {};
}
