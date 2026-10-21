'use strict';
// Mutations remain ordinary CSRF-protected server forms; JS adds confirmation and filter convenience.
document.querySelectorAll('form[data-confirm]').forEach(form => {
  form.addEventListener('submit', event => {
    if (event.submitter?.hasAttribute('data-skip-confirm')) return;
    if (!window.confirm(form.dataset.confirm)) event.preventDefault();
  });
});
document.querySelectorAll('[data-cycle-filter]').forEach(select => {
  select.addEventListener('change', () => {
    const moduleSelect = select.form.querySelector('[name="moduloaId"]');
    if (moduleSelect) moduleSelect.value = '';
    select.form.requestSubmit();
  });
});

const dirtyForms = new Set();
document.querySelectorAll('form[data-dirty-warning]').forEach(form => {
  form.addEventListener('input', event => {
    if (!event.target.matches('[data-translation-select], [data-rubric-grade], [data-team-feedback], [data-general-feedback], [data-overall-weight], [data-other-test-grade]')) dirtyForms.add(form);
  });
  form.addEventListener('submit', () => dirtyForms.delete(form));
});
window.addEventListener('beforeunload', event => {
  if (dirtyForms.size) {
    event.preventDefault();
    event.returnValue = '';
  }
});

const teamGradeNumber = value => Number.parseFloat(String(value ?? '0').replace(',', '.')) || 0;
const teamGradeFormatter = new Intl.NumberFormat('eu-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function refreshParticipantTotal(select) {
  const table = select.closest('.challenge-rubric-table');
  const participantId = select.dataset.participantId;
  const participantKind = select.dataset.participantKind;
  const output = Array.from(table.querySelectorAll('[data-grade-total]'))
    .find(candidate => candidate.dataset.participantId === participantId
      && candidate.dataset.participantKind === participantKind);
  if (!output) return;
  const total = Array.from(table.querySelectorAll('[data-rubric-grade]'))
    .filter(candidate => candidate.dataset.participantId === participantId
      && candidate.dataset.participantKind === participantKind)
    .reduce((sum, grade) => {
      const levelValue = teamGradeNumber(grade.selectedOptions[0]?.dataset.levelValue);
      const evidenceWeight = teamGradeNumber(grade.closest('tr')?.dataset.evidenceWeight);
      return sum + levelValue * evidenceWeight / 100;
    }, 0);
  output.value = teamGradeFormatter.format(total);
  output.textContent = output.value;
}

document.querySelectorAll('[data-rubric-grade]').forEach(select => {
  select.dataset.savedValue = select.value;
  refreshParticipantTotal(select);
  select.addEventListener('change', async () => {
    const status = select.parentElement.querySelector('.team-grade-status');
    const previous = select.dataset.savedValue;
    const payload = new URLSearchParams({
      ebidentziaId: select.dataset.evidenceId
    });
    payload.set(select.dataset.participantKind === 'student' ? 'ikasleaId' : 'taldeaId', select.dataset.participantId);
    if (select.value) payload.set('mailaId', select.value);
    const csrf = document.querySelector('#team-grade-csrf');
    if (csrf?.name) payload.set(csrf.name, csrf.value);
    select.disabled = true;
    status.classList.remove('is-error');
    status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(select.dataset.action, {
        method: 'POST', body: payload, credentials: 'same-origin', headers: { Accept: 'application/json' }
      });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan da kalifikazioa gorde.');
      select.dataset.savedValue = select.value;
      status.textContent = result.message || 'Gordeta';
      (result.affectedStudentIds || []).forEach(studentId => {
        const teammate = Array.from(select.closest('tr').querySelectorAll('[data-rubric-grade]'))
          .find(candidate => candidate.dataset.participantId === String(studentId));
        if (teammate) { teammate.value = select.value; teammate.dataset.savedValue = select.value; refreshParticipantTotal(teammate); }
      });
      refreshParticipantTotal(select);
    } catch (error) {
      select.value = previous;
      status.classList.add('is-error');
      status.textContent = error.message || 'Ezin izan da kalifikazioa gorde.';
      refreshParticipantTotal(select);
    } finally {
      select.disabled = false;
    }
  });
});

document.querySelectorAll('[data-transverse-grade]').forEach(select => {
  select.dataset.savedValue = select.value;
  select.addEventListener('change', async () => {
    const status = select.parentElement.querySelector('.team-grade-status');
    const average = select.parentElement.querySelector('[data-transverse-average]');
    const previous = select.dataset.savedValue;
    const payload = new URLSearchParams({
      ebidentziaId: select.dataset.evidenceId,
      ikasleaId: select.dataset.studentId
    });
    if (select.value) payload.set('mailaId', select.value);
    const csrf = document.querySelector('#team-grade-csrf');
    if (csrf?.name) payload.set(csrf.name, csrf.value);
    select.disabled = true; status.classList.remove('is-error'); status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(select.dataset.action, {
        method: 'POST', body: payload, credentials: 'same-origin', headers: { Accept: 'application/json' }
      });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan da kalifikazioa gorde.');
      select.dataset.savedValue = select.value; status.textContent = result.message || 'Gordeta';
      average.textContent = result.average == null ? 'Batezbestekoa: —'
        : `Batezbestekoa: ${teamGradeFormatter.format(result.average)} (${result.evaluatorCount})`;
    } catch (error) {
      select.value = previous; status.classList.add('is-error');
      status.textContent = error.message || 'Ezin izan da kalifikazioa gorde.';
    } finally { select.disabled = false; }
  });
});

function closeTeamFeedbackPanels(except = null) {
  document.querySelectorAll('[data-team-feedback-panel]:not([hidden])').forEach(panel => {
    if (panel === except) return;
    panel.hidden = true;
    panel.closest('.team-feedback').querySelector(`[data-team-feedback-toggle="${panel.dataset.teamFeedbackPanel}"]`)
      ?.setAttribute('aria-expanded', 'false');
  });
}

document.querySelectorAll('.team-feedback').forEach(feedback => {
  feedback.querySelectorAll('[data-team-feedback-toggle]').forEach(toggle => {
    toggle.addEventListener('click', () => {
      const panel = feedback.querySelector(`[data-team-feedback-panel="${toggle.dataset.teamFeedbackToggle}"]`);
      const opening = panel.hidden;
      closeTeamFeedbackPanels(opening ? panel : null);
      panel.hidden = !opening;
      toggle.setAttribute('aria-expanded', String(opening));
      if (opening) panel.querySelector('textarea').focus();
    });
  });
  feedback.querySelectorAll('[data-team-feedback-close]').forEach(button => {
    button.addEventListener('click', () => closeTeamFeedbackPanels());
  });
  feedback.querySelectorAll('[data-team-feedback]').forEach(textarea => {
    textarea.addEventListener('input', () => {
      const status = textarea.closest('[data-team-feedback-panel]').querySelector('.team-feedback-status');
      status.classList.remove('is-error');
      status.textContent = 'Gorde gabe';
    });
  });
  feedback.querySelectorAll('[data-team-feedback-save]').forEach(button => button.addEventListener('click', async () => {
    const panel = button.closest('[data-team-feedback-panel]');
    const status = panel.querySelector('.team-feedback-status');
    const payload = new URLSearchParams({
      ebidentziaId: feedback.dataset.evidenceId,
      ondoEgindakoak: feedback.querySelector('[data-feedback-kind="strengths"]').value,
      hobetuBeharrekoak: feedback.querySelector('[data-feedback-kind="improvements"]').value
    });
    payload.set(feedback.dataset.participantKind === 'student' ? 'ikasleaId' : 'taldeaId', feedback.dataset.participantId);
    const csrf = document.querySelector('#team-grade-csrf');
    if (csrf?.name) payload.set(csrf.name, csrf.value);
    button.disabled = true;
    status.classList.remove('is-error');
    status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(feedback.dataset.action, {
        method: 'POST', body: payload, credentials: 'same-origin', headers: { Accept: 'application/json' }
      });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan dira oharrak gorde.');
      status.textContent = result.message || 'Oharrak gordeta';
      const strengths = feedback.querySelector('[data-feedback-kind="strengths"]').value;
      const improvements = feedback.querySelector('[data-feedback-kind="improvements"]').value;
      const affectedIds = result.affectedStudentIds || [feedback.dataset.participantId];
      affectedIds.forEach(studentId => {
        const affectedFeedback = Array.from(feedback.closest('tr').querySelectorAll('.team-feedback'))
          .find(candidate => candidate.dataset.participantId === String(studentId));
        if (!affectedFeedback) return;
        affectedFeedback.querySelector('[data-feedback-kind="strengths"]').value = strengths;
        affectedFeedback.querySelector('[data-feedback-kind="improvements"]').value = improvements;
        affectedFeedback.querySelectorAll('[data-team-feedback-toggle]').forEach(toggle => {
          const textarea = affectedFeedback.querySelector(`[data-feedback-kind="${toggle.dataset.teamFeedbackToggle}"]`);
          toggle.querySelector('.feedback-present').classList.toggle('is-empty', !textarea.value.trim());
        });
      });
    } catch (error) {
      status.classList.add('is-error');
      status.textContent = error.message || 'Ezin izan dira oharrak gorde.';
    } finally {
      button.disabled = false;
    }
  }));
});

function closeGeneralFeedbackPanels(except = null) {
  document.querySelectorAll('.general-feedback .team-feedback-popover:not([hidden])').forEach(panel => {
    if (panel === except) return;
    panel.hidden = true;
    panel.closest('.general-feedback').querySelector('.general-feedback-toggle')?.setAttribute('aria-expanded', 'false');
  });
}

document.querySelectorAll('.general-feedback').forEach(feedback => {
  const toggle = feedback.querySelector('.general-feedback-toggle');
  const panel = feedback.querySelector('.team-feedback-popover');
  const textarea = feedback.querySelector('[data-general-feedback]');
  toggle.addEventListener('click', () => {
    const opening = panel.hidden;
    closeTeamFeedbackPanels(); closeGeneralFeedbackPanels(opening ? panel : null);
    panel.hidden = !opening; toggle.setAttribute('aria-expanded', String(opening));
    if (opening) textarea.focus();
  });
  feedback.querySelector('[data-general-feedback-close]').addEventListener('click', () => closeGeneralFeedbackPanels());
  feedback.querySelector('[data-general-feedback-save]').addEventListener('click', async event => {
    const button = event.currentTarget;
    const status = feedback.querySelector('.team-feedback-status');
    const payload = new URLSearchParams({ oharra: textarea.value });
    payload.set(feedback.dataset.participantKind === 'student' ? 'ikasleaId' : 'taldeaId', feedback.dataset.participantId);
    const csrf = document.querySelector('#team-grade-csrf');
    if (csrf?.name) payload.set(csrf.name, csrf.value);
    button.disabled = true; status.classList.remove('is-error'); status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(feedback.dataset.action, { method: 'POST', body: payload, credentials: 'same-origin', headers: { Accept: 'application/json' } });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan da ohar orokorra gorde.');
      status.textContent = result.message || 'Ohar orokorra gordeta';
      toggle.querySelector('.feedback-present').classList.toggle('is-empty', !textarea.value.trim());
    } catch (error) {
      status.classList.add('is-error'); status.textContent = error.message || 'Ezin izan da ohar orokorra gorde.';
    } finally { button.disabled = false; }
  });
});

const overallWeightEditor = document.querySelector('.overall-weight-editor');
if (overallWeightEditor) {
  const rubricWeight = overallWeightEditor.querySelector('[data-overall-weight="rubric"]');
  const testsWeight = overallWeightEditor.querySelector('[data-overall-weight="tests"]');
  const status = overallWeightEditor.querySelector('[data-overall-weight-status]');
  let saved = [rubricWeight.value, testsWeight.value];
  let timer;
  const saveWeights = async () => {
    const payload = new URLSearchParams({ errubrikaPisua: rubricWeight.value, bestelakoProbenPisua: testsWeight.value });
    const csrf = document.querySelector('#team-grade-csrf'); if (csrf?.name) payload.set(csrf.name, csrf.value);
    status.classList.remove('is-error'); status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(overallWeightEditor.dataset.action, { method:'POST', body:payload, credentials:'same-origin', headers:{Accept:'application/json'} });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan dira pisuak gorde.');
      saved = [rubricWeight.value, testsWeight.value]; status.textContent = result.message || 'Pisuak gordeta';
    } catch (error) {
      [rubricWeight.value, testsWeight.value] = saved; status.classList.add('is-error'); status.textContent = error.message || 'Ezin izan dira pisuak gorde.';
    }
  };
  [rubricWeight, testsWeight].forEach(input => input.addEventListener('input', () => {
    const value = Number.parseFloat(input.value);
    if (Number.isFinite(value) && value >= 0 && value <= 100) (input === rubricWeight ? testsWeight : rubricWeight).value = (100 - value).toFixed(2);
    clearTimeout(timer); timer = setTimeout(saveWeights, 500);
  }));
}

document.querySelectorAll('[data-other-test-grade]').forEach(input => {
  input.dataset.savedValue = input.value;
  input.addEventListener('change', async () => {
    const payload = new URLSearchParams({ ikasleaId: input.dataset.studentId });
    if (input.value !== '') payload.set('nota', input.value);
    const csrf = document.querySelector('#team-grade-csrf'); if (csrf?.name) payload.set(csrf.name, csrf.value);
    const status = input.parentElement.querySelector('.team-grade-status'); input.disabled = true; status.classList.remove('is-error'); status.textContent = 'Gordetzen…';
    try {
      const response = await fetch(input.dataset.action, { method:'POST', body:payload, credentials:'same-origin', headers:{Accept:'application/json'} });
      const result = response.headers.get('content-type')?.includes('application/json') ? await response.json() : null;
      if (!response.ok || !result) throw new Error(result?.message || 'Ezin izan da nota gorde.');
      input.dataset.savedValue = input.value; status.textContent = result.message || 'Nota gordeta';
    } catch (error) {
      input.value = input.dataset.savedValue; status.classList.add('is-error'); status.textContent = error.message || 'Ezin izan da nota gorde.';
    } finally { input.disabled = false; }
  });
});

document.querySelectorAll('.other-test-card').forEach(card => {
  const fields = {
    name: card.querySelector('[data-other-test-field="name"]'),
    date: card.querySelector('[data-other-test-field="date"]'),
    weight: card.querySelector('[data-other-test-field="weight"]')
  };
  const refreshSummary = () => {
    card.querySelector('[data-other-test-summary-name]').textContent = fields.name.value;
    card.querySelector('[data-other-test-summary-date]').textContent = fields.date.value;
    card.querySelector('[data-other-test-summary-weight]').textContent = `${fields.weight.value}%`;
  };
  Object.values(fields).forEach(field => field?.addEventListener('input', refreshSummary));
});

document.addEventListener('click', event => {
  if (!event.target.closest('.team-feedback')) closeTeamFeedbackPanels();
  if (!event.target.closest('.general-feedback')) closeGeneralFeedbackPanels();
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') { closeTeamFeedbackPanels(); closeGeneralFeedbackPanels(); }
});

const rubricImportToggle = document.querySelector('#rubric-import-toggle');
const rubricImportPanel = document.querySelector('#rubric-import-panel');
rubricImportToggle?.addEventListener('click', () => {
  const opening = rubricImportPanel.hidden;
  rubricImportPanel.hidden = !opening;
  rubricImportToggle.setAttribute('aria-expanded', String(opening));
  rubricImportToggle.textContent = opening ? 'Inportazioa itxi' : 'Inportatu';
  if (opening) rubricImportPanel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
});

// The lock is a consultation-mode control, like the teacher timetable toggle.
const rubricSection = document.querySelector('.rubric-section');
const rubricIsLocked = () => rubricSection?.dataset.editable === 'false';
const rubricEditKey = rubricSection ? `ethazi:rubric-editable:${rubricSection.dataset.rubricKey}` : null;
function savedRubricEditable() {
  try { return sessionStorage.getItem(rubricEditKey) === 'true'; }
  catch (_) { return false; }
}

function refreshRubricHeaderHeight() {
  const scroller = rubricSection?.querySelector('.rubric-scroll');
  const header = scroller?.querySelector('thead');
  if (scroller && header) scroller.style.setProperty('--rubric-head-height', `${header.getBoundingClientRect().height}px`);
}

function setRubricEditable(editable) {
  if (!editable) endIndicatorDrag(false);
  rubricSection.dataset.editable = String(editable);
  // Keep the choice across form redirects in this tab, independently per rubric.
  try { sessionStorage.setItem(rubricEditKey, String(editable)); } catch (_) { /* Storage may be disabled. */ }
  const toggle = rubricSection.querySelector('.rubric-lock-toggle');
  refreshRubricHeaderHeight();
  if (!toggle) return;
  toggle.setAttribute('aria-pressed', String(editable));
  toggle.setAttribute('aria-label', editable ? 'Errubrikaren edizioa blokeatu' : 'Errubrikaren edizioa desblokeatu');
  toggle.textContent = editable ? '🔓 Desblokeatuta · Blokeatu' : '🔒 Blokeatuta · Desblokeatu';
  rubricSection.querySelectorAll('.indicator-sortable').forEach(refreshIndicatorControls);
}

document.querySelectorAll('.rubric-collapse-cell').forEach(cell => {
  cell.addEventListener('click', event => {
    if (event.target.closest('a')) return;
    const row = cell.closest('.rubric-row');
    const collapsed = row.classList.toggle('is-collapsed');
    const toggle = cell.querySelector('.rubric-collapse-toggle');
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.querySelector('.rubric-collapse-icon').textContent = collapsed ? '▸' : '▾';
    toggle.querySelector('.rubric-collapse-label').textContent = collapsed ? 'Tolestuta' : 'Zabalik';
  });
});

// Dragging is confined to one list (one competency/level cell). The server checks
// the exact ID set again and assigns consecutive positions in a transaction.
let indicatorDrag = null;
let pendingOrders = 0;
const itemsIn = list => Array.from(list.children).filter(item => item.matches('.rubric-indicator'));

function refreshIndicatorControls(sortable) {
  const items = itemsIn(sortable.querySelector('.indicator-list'));
  items.forEach((item, index) => {
    item.querySelector('.indicator-order').textContent = `${sortable.dataset.levelNumber}.${index + 1}`;
    item.querySelector('[data-move="-1"]').disabled = index === 0 || (sortable.dataset.saving === 'true' || rubricIsLocked());
    item.querySelector('[data-move="1"]').disabled = index === items.length - 1 || (sortable.dataset.saving === 'true' || rubricIsLocked());
    item.querySelector('.drag-handle').disabled = (sortable.dataset.saving === 'true' || rubricIsLocked());
  });
}

async function saveIndicatorOrder(sortable, previous) {
  if (rubricIsLocked()) return;
  const list = sortable.querySelector('.indicator-list');
  const items = itemsIn(list);
  if (items.every((item, index) => item === previous[index])) return;
  const form = sortable.querySelector('.indicator-order-form');
  const status = sortable.querySelector('.sort-status');
  const body = new URLSearchParams(new FormData(form));
  items.forEach(item => body.append('adierazleaIds', item.dataset.indicatorId));
  sortable.dataset.saving = 'true';
  sortable.setAttribute('aria-busy', 'true');
  status.classList.remove('sort-error');
  status.textContent = 'Ordena gordetzen…';
  pendingOrders++;
  refreshIndicatorControls(sortable);
  try {
    const response = await fetch(form.action, {
      method: 'POST', body, credentials: 'same-origin', headers: { Accept: 'application/json' }
    });
    if (!response.headers.get('content-type')?.includes('application/json')) {
      throw new Error('Ezin da ordena gorde. Kargatu berriro orria eta egiaztatu saioa.');
    }
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || 'Ezin da ordena gorde. Kargatu berriro errubrika.');
    status.textContent = result.message;
  } catch (error) {
    previous.forEach(item => list.appendChild(item));
    status.classList.add('sort-error');
    status.textContent = error.message || 'Konexio-errorea. Kargatu berriro errubrika ordena egiaztatzeko.';
  } finally {
    pendingOrders--;
    sortable.dataset.saving = 'false';
    sortable.removeAttribute('aria-busy');
    refreshIndicatorControls(sortable);
  }
}

function startIndicatorDrag(handle) {
  const sortable = handle.closest('.indicator-sortable');
  if (rubricIsLocked() || sortable.dataset.saving === 'true' || indicatorDrag) return false;
  const list = sortable.querySelector('.indicator-list');
  const item = handle.closest('.rubric-indicator');
  indicatorDrag = { sortable, list, item, previous: itemsIn(list) };
  item.classList.add('dragging');
  return true;
}

function positionIndicator(target, y) {
  if (!indicatorDrag || target?.closest('.indicator-sortable') !== indicatorDrag.sortable) return false;
  const { list, item } = indicatorDrag;
  const over = target.closest('.rubric-indicator');
  if (over && over !== item) {
    const rect = over.getBoundingClientRect();
    list.insertBefore(item, y < rect.top + rect.height / 2 ? over : over.nextSibling);
  }
  const scroll = list.closest('.rubric-scroll');
  const bounds = scroll.getBoundingClientRect();
  if (y > bounds.bottom - 45) scroll.scrollTop += 14;
  else if (y < bounds.top + 80) scroll.scrollTop -= 14;
  return true;
}

function endIndicatorDrag(save) {
  if (!indicatorDrag) return;
  const { sortable, list, item, previous } = indicatorDrag;
  indicatorDrag = null;
  item.classList.remove('dragging');
  if (save) void saveIndicatorOrder(sortable, previous);
  else previous.forEach(node => list.appendChild(node));
}

document.querySelectorAll('.indicator-sortable').forEach(sortable => {
  refreshIndicatorControls(sortable);
  sortable.addEventListener('click', event => {
    const button = event.target.closest('[data-move]');
    if (rubricIsLocked() || !button || sortable.dataset.saving === 'true' || indicatorDrag) return;
    const list = sortable.querySelector('.indicator-list');
    const previous = itemsIn(list);
    const item = button.closest('.rubric-indicator');
    const index = previous.indexOf(item);
    const target = previous[index + Number(button.dataset.move)];
    if (!target) return;
    list.insertBefore(item, Number(button.dataset.move) < 0 ? target : target.nextSibling);
    void saveIndicatorOrder(sortable, previous);
  });
  sortable.querySelectorAll('.drag-handle').forEach(handle => {
    handle.addEventListener('pointerdown', event => {
      if (!event.isPrimary || event.button !== 0 || !startIndicatorDrag(handle)) return;
      indicatorDrag.pointerId = event.pointerId;
      handle.setPointerCapture(event.pointerId);
      event.preventDefault();
    });
  });
});
document.addEventListener('pointermove', event => {
  if (indicatorDrag?.pointerId !== event.pointerId) return;
  event.preventDefault();
  positionIndicator(document.elementFromPoint(event.clientX, event.clientY), event.clientY);
}, { passive: false });
document.addEventListener('pointerup', event => {
  if (indicatorDrag?.pointerId !== event.pointerId) return;
  const target = document.elementFromPoint(event.clientX, event.clientY);
  endIndicatorDrag(target?.closest('.indicator-sortable') === indicatorDrag.sortable);
});
document.addEventListener('pointercancel', event => {
  if (indicatorDrag?.pointerId === event.pointerId) endIndicatorDrag(false);
});
window.addEventListener('blur', () => endIndicatorDrag(false));
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') endIndicatorDrag(false);
});
window.addEventListener('beforeunload', event => {
  if (pendingOrders) { event.preventDefault(); event.returnValue = ''; }
});

if (rubricSection) {
  refreshRubricHeaderHeight();
  window.addEventListener('resize', refreshRubricHeaderHeight);
  if ('ResizeObserver' in window) new ResizeObserver(refreshRubricHeaderHeight).observe(rubricSection.querySelector('thead'));
  const rubricLockToggle = rubricSection.querySelector('.rubric-lock-toggle');
  rubricLockToggle?.addEventListener('click', () => setRubricEditable(rubricIsLocked()));
  // Validation errors and a newly created level must remain immediately editable.
  setRubricEditable(rubricLockToggle
    ? savedRubricEditable() || rubricSection.dataset.editRequired === 'true'
    : false);
  rubricSection.addEventListener('submit', event => {
    if (rubricIsLocked() && !event.target.closest('.other-tests-section')
        && !event.target.id?.startsWith('delete-other-test-')) {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  }, true);

  const scroller = rubricSection.querySelector('.rubric-scroll');
  // Consume downward scrolling with the page until the table reaches the top.
  // Once aligned, native scrolling retains sticky headings and descriptions.
  function scrollPageBeforeRubric(delta) {
    const top = scroller.getBoundingClientRect().top;
    const pageRemaining = document.documentElement.scrollHeight - window.innerHeight - window.scrollY;
    if (delta <= 0 || top <= 1 || pageRemaining <= 1) return false;
    const pageDelta = Math.min(delta, top, pageRemaining);
    window.scrollBy({ top: pageDelta, behavior: 'instant' });
    scroller.scrollTop += delta - pageDelta;
    return true;
  }
  scroller.addEventListener('wheel', event => {
    if (event.ctrlKey || event.shiftKey || Math.abs(event.deltaX) > Math.abs(event.deltaY)
        || event.target.closest('input, textarea, select')) return;
    const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? window.innerHeight : 1;
    if (scrollPageBeforeRubric(event.deltaY * unit)) event.preventDefault();
  }, { passive: false });
  let touchPosition = null;
  scroller.addEventListener('touchstart', event => {
    touchPosition = event.touches.length === 1 && !event.target.closest('input, textarea, select, .drag-handle')
      ? { x: event.touches[0].clientX, y: event.touches[0].clientY, routed: false } : null;
  }, { passive: true });
  scroller.addEventListener('touchmove', event => {
    if (!touchPosition || event.touches.length !== 1) return;
    const touch = event.touches[0];
    const delta = touchPosition.y - touch.clientY;
    const horizontal = Math.abs(touchPosition.x - touch.clientX);
    const routed = touchPosition.routed;
    touchPosition = { x: touch.clientX, y: touch.clientY, routed };
    if (!event.cancelable) return;
    if (delta > horizontal && scrollPageBeforeRubric(delta)) {
      touchPosition.routed = true;
      event.preventDefault();
    } else if (routed) {
      // A prevented touch gesture stays under our control until the finger lifts.
      const before = scroller.scrollTop;
      scroller.scrollTop += delta;
      const remaining = delta - (scroller.scrollTop - before);
      if (remaining) window.scrollBy({ top: remaining, behavior: 'instant' });
      event.preventDefault();
    }
  }, { passive: false });
  scroller.addEventListener('touchend', () => { touchPosition = null; }, { passive: true });
  scroller.addEventListener('touchcancel', () => { touchPosition = null; }, { passive: true });
  scroller.addEventListener('keydown', event => {
    if (event.target !== scroller || event.ctrlKey || event.altKey || event.metaKey || event.shiftKey) return;
    const delta = event.key === 'ArrowDown' ? 40 : ['PageDown', ' '].includes(event.key) ? scroller.clientHeight : 0;
    if (scrollPageBeforeRubric(delta)) event.preventDefault();
  });
}

const otherTestModal = document.querySelector('#other-test-modal');
const openOtherTestModal = document.querySelector('#other-test-modal-open');
const closeOtherTestModal = () => otherTestModal?.close();
openOtherTestModal?.addEventListener('click', () => {
  otherTestModal.showModal();
  otherTestModal.querySelector('input[name="izena"]')?.focus();
});
document.querySelector('#other-test-modal-close')?.addEventListener('click', closeOtherTestModal);
otherTestModal?.addEventListener('click', event => {
  if (event.target === otherTestModal) closeOtherTestModal();
});

const rubricWeightInputs = document.querySelectorAll('.evidence-weight-input');
const rubricWeightTotal = document.querySelector('#rubric-weight-total');
const rubricWeightTotalValues = document.querySelectorAll('.rubric-weight-total-value');
const rubricWeightStatus = rubricWeightTotal?.closest('.rubric-weight-total');
const rubricWeightHeading = document.querySelector('.evidence-heading-weight');
function refreshRubricWeight() {
  if (!rubricWeightTotal) return;
  const total = Array.from(rubricWeightInputs).reduce((sum, input) => sum + (Number.parseFloat(input.value) || 0), 0);
  const formattedTotal = new Intl.NumberFormat('eu', { maximumFractionDigits: 2 }).format(total);
  const invalid = Math.abs(total - 100) > 0.00001 && rubricWeightInputs.length > 0;
  rubricWeightTotalValues.forEach(value => { value.textContent = formattedTotal; });
  rubricWeightStatus.classList.toggle('is-invalid', invalid);
  rubricWeightHeading?.classList.toggle('is-invalid', invalid);
}
rubricWeightInputs.forEach(input => input.addEventListener('input', refreshRubricWeight));
refreshRubricWeight();

const focusedKinielaElement = document.querySelector('.kiniela-focus-target');
if (focusedKinielaElement) {
  requestAnimationFrame(() => focusedKinielaElement.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' }));
}

// Keep the edited/new column or cell in view after an ordinary form submission.
if (/^#(maila|gelaxka)-[\d-]+$/.test(window.location.hash)) {
  const target = document.getElementById(window.location.hash.slice(1));
  target?.scrollIntoView({ block: 'nearest', inline: 'end' });
  const scroller = target?.closest('.rubric-scroll');
  if (scroller) {
    if (target.matches('th')) scroller.scrollTop = 0;
    else scroller.scrollTop += target.getBoundingClientRect().top - scroller.getBoundingClientRect().top
      - scroller.querySelector('thead').getBoundingClientRect().height - 1;
  }
  target?.querySelector('.level-name-editor[open] input[name="izena"]')?.focus({ preventScroll: true });
}

// Each translation editor shows one language at a time; all drafts remain in the form.
document.querySelectorAll('.translation-editor').forEach(editor => {
  const select = editor.querySelector('[data-translation-select]');
  const refresh = () => editor.querySelectorAll('[data-translation-panel]').forEach(panel => {
    panel.hidden = panel.dataset.translationPanel !== select.value;
  });
  const currentLanguage = document.querySelector('#hizkuntza')?.value || select.dataset.defaultLanguage;
  if (currentLanguage === 'GAZTELERA') select.value = 'Es';
  if (currentLanguage === 'INGELERA') select.value = 'En';
  select.addEventListener('change', refresh);
  refresh();
});
// The same shared IE may be listed under several modules. Keep its checkboxes in sync.
document.querySelectorAll('.ie-picker').forEach(picker => picker.addEventListener('change', event => {
  if (event.target.name !== 'emaitzaIds') return;
  picker.querySelectorAll('input[name="emaitzaIds"]').forEach(input => {
    if (input.value === event.target.value) input.checked = event.target.checked;
  });
}));
