'use strict';
// Mutations remain ordinary CSRF-protected server forms; JS adds confirmation and filter convenience.
document.querySelectorAll('form[data-confirm]').forEach(form => {
  form.addEventListener('submit', event => {
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
  form.addEventListener('input', () => dirtyForms.add(form));
  form.addEventListener('submit', () => dirtyForms.delete(form));
});
window.addEventListener('beforeunload', event => {
  if (dirtyForms.size) {
    event.preventDefault();
    event.returnValue = '';
  }
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
  rubricSection.querySelector('.rubric-lock-toggle').addEventListener('click', () => {
    setRubricEditable(rubricIsLocked());
  });
  // Validation errors and a newly created level must remain immediately editable.
  setRubricEditable(savedRubricEditable() || rubricSection.dataset.editRequired === 'true');
  rubricSection.addEventListener('submit', event => {
    if (rubricIsLocked()) {
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
