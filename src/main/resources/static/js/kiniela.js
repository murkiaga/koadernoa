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
    const results = await Promise.all([...unsaved].filter(weightForm => saveWeights.has(weightForm)).map(weightForm => saveWeights.get(weightForm)()));
    if (results.every(Boolean)) { submitting = true; form.requestSubmit(); }
    else document.querySelector('#rubric-save-status').textContent = 'Pisu batzuk ez dira gorde. Itxi leihoa eta berrikusi pisuak.';
  });
  window.addEventListener('beforeunload', event => {
    if (unsaved.size) { event.preventDefault(); event.returnValue = ''; }
  });
  document.querySelectorAll('.ie-select').forEach(button => button.addEventListener('click', () => {
    const selected = new Set([...button.closest('.kiniela-module').querySelectorAll(`.kiniela-link[data-outcome="${button.dataset.ieId}"]`)].map(row => row.dataset.indicator));
    form.querySelectorAll('[name=adierazleaIds]').forEach(input => { input.checked = selected.has(input.value); });
    form.elements.moduloaId.value = button.closest('.kiniela-module').dataset.module;
    form.action = form.dataset.actionTemplate.replace('IE_ID', button.dataset.ieId);
    document.querySelector('#selected-outcome').textContent = button.textContent;
    updateSelectionCounts();
    const language = button.closest('.kiniela-module').dataset.language;
    const key = language === 'GAZTELERA' ? 'es' : language === 'INGELERA' ? 'en' : 'eu';
    dialog.querySelectorAll('[data-localized]').forEach(node => { node.textContent = node.dataset[key]; });
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
      input.dataset.savedValue = input.value;
      let pending = null;
      const save = () => {
        if (pending) return pending;
        if (input.value === input.dataset.savedValue) {
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
            const row = weightForm.closest('.kiniela-link');
            document.querySelectorAll('.kiniela-link').forEach(relatedRow => {
              if (relatedRow.dataset.indicator !== row.dataset.indicator || relatedRow.dataset.outcome !== row.dataset.outcome) return;
              const relatedForm = relatedRow.querySelector('.weight-form');
              const relatedInput = relatedForm.querySelector('[name=pisua]');
              relatedInput.value = value;
              relatedInput.dataset.savedValue = value;
              unsaved.delete(relatedForm);
              relatedForm.querySelector('.save-status').textContent = 'Gordeta';
              recalculate(relatedRow.closest('.kiniela-module'));
            });
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
  const indicatorRows = (id, outcome, module) => [...document.querySelectorAll('.kiniela-link')].filter(row => row.dataset.indicator === id && row.dataset.outcome === outcome && row.dataset.module === module);
  const post = async (action, payload) => {
    const response = await fetch(action, { method: 'POST', body: payload, headers: { Accept: 'application/json' } });
    if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw new Error();
    return response.json();
  };
  document.querySelectorAll('.challenge-form').forEach(challengeForm => {
    const input = challengeForm.querySelector('[name=landuta]');
    input.addEventListener('change', async () => {
      const row = challengeForm.closest('.kiniela-link');
      const forms = [...document.querySelectorAll('.challenge-form')].filter(item => {
        const itemRow = item.closest('.kiniela-link');
        return itemRow.dataset.indicator === row.dataset.indicator && itemRow.dataset.outcome === row.dataset.outcome
          && item.dataset.syncKey === challengeForm.dataset.syncKey;
      });
      const checked = input.checked;
      forms.forEach(item => { item.elements.landuta.disabled = true; });
      const payload = new URLSearchParams(new FormData(challengeForm));
      payload.set('landuta', String(checked));
      const status = challengeForm.querySelector('.save-status');
      status.textContent = 'Gordetzen…';
      unsaved.add(challengeForm);
      try {
        await post(challengeForm.action, payload);
        forms.forEach(item => { item.elements.landuta.checked = checked; });
        new Set(forms.map(item => item.closest('.kiniela-link'))).forEach(item => {
          item.classList.toggle('is-covered', !!item.querySelector('[name=landuta]:checked'));
        });
        status.textContent = 'Gordeta';
      } catch (_) {
        input.checked = !checked;
        status.textContent = 'Ez da gorde. Saiatu berriro.';
      } finally {
        forms.forEach(item => { item.elements.landuta.disabled = false; });
        unsaved.delete(challengeForm);
      }
    });
    challengeForm.addEventListener('submit', event => event.preventDefault());
  });
  const noteDialog = document.querySelector('#indicator-note-dialog');
  const noteForm = document.querySelector('#indicator-note-form');
  const noteText = document.querySelector('#indicator-note-text');
  const noteStatus = document.querySelector('#indicator-note-status');
  let noteIndicator = null;
  let savingNote = false;
  const openNote = row => {
    noteIndicator = row.dataset.indicator;
    noteForm.elements.ieId.value = row.dataset.outcome;
    noteForm.elements.moduloaId.value = row.dataset.module;
    noteText.value = row.dataset.note || '';
    noteStatus.textContent = '';
    document.querySelector('#indicator-note-label').textContent = document.getElementById('ie-' + row.dataset.module + '-' + row.dataset.outcome).querySelector('.ie-select').textContent + ' · ' + row.querySelector('.indicator-text').textContent;
    noteForm.action = noteForm.dataset.actionTemplate.replace('INDICATOR_ID', noteIndicator);
    noteDialog.showModal();
    noteText.focus();
  };
  document.querySelectorAll('.kiniela-link').forEach(row => {
    row.querySelector('.indicator-cell').addEventListener('contextmenu', event => { event.preventDefault(); openNote(row); });
    row.querySelector('.indicator-note').addEventListener('click', () => openNote(row));
  });
  noteText.addEventListener('input', () => unsaved.add(noteForm));
  noteDialog.addEventListener('cancel', event => { if (savingNote) event.preventDefault(); });
  noteDialog.addEventListener('close', () => unsaved.delete(noteForm));
  document.querySelector('#cancel-indicator-note').addEventListener('click', () => noteDialog.close());
  noteForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (savingNote || !noteForm.reportValidity()) return;
    const payload = new URLSearchParams(new FormData(noteForm));
    savingNote = true;
    noteForm.querySelectorAll('button, textarea').forEach(input => { input.disabled = true; });
    noteStatus.textContent = 'Gordetzen…';
    try {
      await post(noteForm.action, payload);
      indicatorRows(noteIndicator, noteForm.elements.ieId.value, noteForm.elements.moduloaId.value).forEach(row => {
        row.dataset.note = noteText.value.trim();
        row.querySelector('.indicator-note').textContent = row.dataset.note ? '📝 Oharra' : 'Oharra';
      });
      noteDialog.close();
    } catch (_) {
      noteStatus.textContent = 'Ez da oharra gorde. Saiatu berriro.';
    } finally {
      savingNote = false;
      noteForm.querySelectorAll('button, textarea').forEach(input => { input.disabled = false; });
    }
  });
  if (location.hash.startsWith('#ie-')) document.querySelector(location.hash)?.closest('details')?.setAttribute('open', '');
})();
