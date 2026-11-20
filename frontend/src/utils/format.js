export function getLocaleForCurrency(currency) {
  switch (currency) {
    case 'INR':
      return 'en-IN';
    case 'USD':
      return 'en-US';
    case 'GBP':
      return 'en-GB';
    default:
      return 'en-US';
  }
}

export function formatMoney(amount, currency = 'USD') {
  if (amount === null || amount === undefined) {
    return 'Not available';
  }
  const locale = getLocaleForCurrency(currency);
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(amount);
}

export function formatDate(dateStr) {
  if (!dateStr) {
    return '';
  }
  const parts = dateStr.split('-');
  if (parts.length !== 3) {
    return dateStr;
  }
  const [year, month, day] = parts.map(Number);
  const date = new Date(year, month - 1, day);
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

export function formatMonth(monthStr) {
  if (!monthStr) {
    return '';
  }
  const parts = monthStr.split('-');
  if (parts.length !== 2) {
    return monthStr;
  }
  const [year, month] = parts.map(Number);
  const date = new Date(year, month - 1, 1);
  return new Intl.DateTimeFormat('en-GB', {
    month: 'short',
    year: 'numeric',
  }).format(date);
}
