(() => {
  const challenge = document.querySelector('#erronka-form');
  if (challenge) {
    const filter = () => {
      const cycle = challenge.querySelector('#zikloaId').value;
      const level = challenge.querySelector('#mailaId').value;
      const language = challenge.querySelector('#hizkuntza').value;
      let count = 0;
      challenge.querySelectorAll('.erronka-module').forEach(label => {
        const visible = label.dataset.cycle === cycle && label.dataset.level === level &&
          (language === 'ZEHAZTU_GABE' || label.dataset.language === language || label.dataset.language === 'ZEHAZTU_GABE');
        label.hidden = !visible;
        const input = label.querySelector('input'); input.disabled = !visible;
        if (!visible) input.checked = false;
        if (visible) count++;
      });
      document.querySelector('#no-modules').hidden = count > 0;
    };
    ['zikloaId', 'mailaId', 'hizkuntza'].forEach(id => challenge.querySelector('#' + id).addEventListener('change', filter));
    filter();
  }
  const dialog = document.querySelector('#rubric-dialog');
  if (!dialog) return;
  const form = document.querySelector('#rubric-form');
  const unsaved = new Set();
  const saveWeights = new Map();
  const updateSelectionCounts = () => {
    form.querySelectorAll('.selection-competency').forEach(row => {
      const count = row.querySelectorAll('[name=adierazleaIds]:checked').length;
      row.querySelector('.selection-count').textContent = count ? ` · ${count} hautatuta` : '';
    });
  };
  form.querySelectorAll('.competency-toggle').forEach(toggle => {
    toggle.addEventListener('click', () => {
      const collapsed = toggle.closest('tr').classList.toggle('is-collapsed');
      toggle.setAttribute('aria-expanded', String(!collapsed));
      toggle.querySelector('.collapse-icon').textContent = collapsed ? '▸' : '▾';
      toggle.querySelector('.collapse-label').textContent = collapsed ? 'Zabaldu' : 'Tolestu';
    });
  });
  form.addEventListener('change', updateSelectionCounts);
  let submitting = false;
  form.addEventListener('submit', async event => {
    if (submitting || !unsaved.size) return;
    event.preventDefault();
    const results = await Promise.all([...unsaved].map(weightForm => saveWeights.get(weightForm)()));
    if (results.every(Boolean)) { submitting = true; form.requestSubmit(); }
    else document.querySelector('#rubric-save-status').textContent = 'Pisu batzuk ez dira gorde. Itxi leihoa eta berrikusi pisuak.';
  });
  window.addEventListener('beforeunload', event => {
    if (unsaved.size) { event.preventDefault(); event.returnValue = ''; }
  });
  document.querySelectorAll('.ie-select').forEach(button => button.addEventListener('click', () => {
    const selected = new Set([...button.closest('.kiniela-module').querySelectorAll(`.kiniela-link[data-outcome="${button.dataset.ieId}"]`)].map(row => row.dataset.indicator));
    form.querySelectorAll('[name=adierazleaIds]').forEach(input => { input.checked = selected.has(input.value); });
    form.action = form.dataset.actionTemplate.replace('IE_ID', button.dataset.ieId);
    document.querySelector('#selected-outcome').textContent = button.textContent;
    updateSelectionCounts();
    dialog.showModal();
  }));
  document.querySelector('#cancel-rubric').addEventListener('click', () => dialog.close());
  const recalculate = module => {
    let total = 0;
    module.querySelectorAll('.kiniela-ie').forEach(body => {
      let subtotal = 0;
      module.querySelectorAll(`.kiniela-link[data-outcome="${body.dataset.outcome}"] [name=pisua]`).forEach(input => { subtotal += Math.round(Number(input.value || 0) * 100); });
      body.querySelector('.ie-total').textContent = `${subtotal / 100}%`;
      total += subtotal;
    });
    module.querySelectorAll('.module-total').forEach(cell => { cell.textContent = `${total / 100}%`; });
    const status = module.querySelector('.module-status');
    status.textContent = `${total / 100}% / 100%`;
    status.classList.toggle('complete', total === 10000);
    status.classList.toggle('incomplete', total !== 10000);
  };
  document.querySelectorAll('.kiniela-module').forEach(module => {
    const key = `kiniela-module-${module.dataset.module}`;
    try { if (localStorage.getItem(key) === 'closed') module.open = false; } catch (_) { /* Storage may be disabled. */ }
    module.addEventListener('toggle', () => { try { localStorage.setItem(key, module.open ? 'open' : 'closed'); } catch (_) {} });
    recalculate(module);
    module.querySelectorAll('.weight-form').forEach(weightForm => {
      const input = weightForm.querySelector('[name=pisua]');
      const status = weightForm.querySelector('.save-status');
      let savedValue = input.value;
      let pending = null;
      const save = () => {
        if (pending) return pending;
        if (input.value === savedValue) {
          unsaved.delete(weightForm);
          status.textContent = '';
          return Promise.resolve(true);
        }
        if (!weightForm.reportValidity()) {
          status.textContent = 'Sartu 0–100 arteko pisua, gehienez bi hamartarrekin.';
          return Promise.resolve(false);
        }
        const value = input.value;
        const payload = new URLSearchParams(new FormData(weightForm));
        input.readOnly = true;
        status.textContent = 'Gordetzen…';
        pending = (async () => {
          try {
            const response = await fetch(weightForm.action, { method: 'POST', body: payload, headers: { Accept: 'application/json' } });
            if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error();
            status.textContent = (await response.json()).message;
            savedValue = value;
            unsaved.delete(weightForm);
            return true;
          } catch (_) {
            status.textContent = 'Ez da gorde. Sakatu Enter berriro saiatzeko.';
            return false;
          } finally { input.readOnly = false; pending = null; }
        })();
        return pending;
      };
      saveWeights.set(weightForm, save);
      input.addEventListener('input', () => {
        unsaved.add(weightForm);
        recalculate(module);
        status.textContent = 'Gorde gabe';
      });
      input.addEventListener('blur', save);
      input.addEventListener('keydown', event => {
        if (event.key === 'Enter') { event.preventDefault(); save(); }
      });
      weightForm.addEventListener('submit', event => { event.preventDefault(); save(); });
    });
  });
  if (location.hash.startsWith('#ie-')) document.querySelector(location.hash)?.closest('details')?.setAttribute('open', '');
})();
