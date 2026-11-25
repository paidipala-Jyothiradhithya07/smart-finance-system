export function getLocaleForCurrency(currency) {
  switch (currency) {
    case 'INR':
      return 'en-IN';
    case 'USD':
      return 'en-US';
    case 'GBP':
      return 'en-GB';
    default:
      return 'en-IN';
  }
}

export function formatMoney(value, currency = 'INR') {
  if (value === null || value === undefined) {
    return 'Not available';
  }

  const curr = currency || 'INR';
  const locale = getLocaleForCurrency(curr);

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: curr,
  }).format(value);
}

export function formatDate(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') {
    return '';
  }

  const parts = dateStr.split('-');
  if (parts.length !== 3) {
    return dateStr;
  }

  const [year, month, day] = parts.map(Number);
  if (Number.isNaN(year) || Number.isNaN(month) || Number.isNaN(day)) {
    return dateStr;
  }

  const date = new Date(year, month - 1, day);
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

export function formatMonth(monthStr) {
  if (!monthStr || typeof monthStr !== 'string') {
    return '';
  }

  const parts = monthStr.split('-');
  if (parts.length !== 2) {
    return monthStr;
  }

  const [year, month] = parts.map(Number);
  if (Number.isNaN(year) || Number.isNaN(month)) {
    return monthStr;
  }

  const date = new Date(year, month - 1, 1);
  return new Intl.DateTimeFormat('en-GB', {
    month: 'short',
    year: 'numeric',
  }).format(date);
}
