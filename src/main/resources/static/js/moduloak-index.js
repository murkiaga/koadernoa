(() => {
  const select = document.getElementById('moduloPageSize');
  if (!select) return;

  const storageKey = 'kudeatzaile.moduloa.pageSize';
  const allowedSizes = new Set(Array.from(select.options, option => option.value));
  const params = new URLSearchParams(window.location.search);

  let savedSize = null;
  try {
    savedSize = localStorage.getItem(storageKey);
  } catch (_) {
    // Biltegiratzea desgaituta badago, zerbitzariaren lehenetsia erabiltzen da.
  }

  if (!params.has('size') && savedSize && allowedSizes.has(savedSize) && savedSize !== select.value) {
    params.set('size', savedSize);
    params.set('page', '0');
    window.location.replace(`${window.location.pathname}?${params.toString()}${window.location.hash}`);
    return;
  }

  select.addEventListener('change', () => {
    try {
      localStorage.setItem(storageKey, select.value);
    } catch (_) {
      // Hautapena oraingo nabigazioan aplikatzen da hala ere.
    }
    select.form.submit();
  });
})();
