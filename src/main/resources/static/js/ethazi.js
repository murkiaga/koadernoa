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
