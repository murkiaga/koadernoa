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

(() => {
  const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  document.querySelectorAll('.module-language-select').forEach(select => {
    select.dataset.savedValue = select.value;
    select.addEventListener('change', async () => {
      const previous = select.dataset.savedValue;
      const status = select.parentElement.querySelector('.module-language-status');
      select.disabled = true;
      status.classList.remove('is-error');
      status.textContent = 'Gordetzen…';
      try {
        const body = new URLSearchParams({ hizkuntza: select.value });
        const response = await fetch(select.dataset.url, {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
            Accept: 'application/json',
            ...(csrf ? { [csrfHeader]: csrf } : {})
          },
          body: body.toString()
        });
        const result = response.headers.get('content-type')?.includes('application/json')
          ? await response.json() : {};
        if (!response.ok || result.ok !== true) throw new Error(result.mezua || 'Ezin izan da hizkuntza gorde.');
        select.dataset.savedValue = result.hizkuntza;
        select.value = result.hizkuntza;
        status.textContent = 'Gordeta';
      } catch (error) {
        select.value = previous;
        status.classList.add('is-error');
        status.textContent = error.message || 'Ezin izan da gorde.';
      } finally {
        select.disabled = false;
      }
    });
  });
})();
