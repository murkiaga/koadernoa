(() => {
  const panels = Array.from(document.querySelectorAll('[data-week-panel]'));
  if (!panels.length) return;
  const tabs = Array.from(document.querySelectorAll('[data-week-tab]'));
  const label = document.querySelector('[data-week-label]');
  const previous = document.querySelector('[data-week-previous]');
  const next = document.querySelector('[data-week-next]');
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  document.querySelectorAll(`.kronograma-day[data-date="${today}"]`).forEach(day => day.classList.add('is-today'));
  let current = Math.max(0, panels.findIndex(panel => panel.dataset.weekStart <= today && panel.dataset.weekEnd >= today));

  function showWeek(index) {
    current = Math.max(0, Math.min(index, panels.length - 1));
    panels.forEach((panel, i) => { panel.hidden = i !== current; });
    tabs.forEach((tab, i) => {
      tab.classList.toggle('active', i === current);
      tab.setAttribute('aria-current', i === current ? 'true' : 'false');
    });
    previous.disabled = current === 0;
    next.disabled = current === panels.length - 1;
    const panel = panels[current];
    label.textContent = `${current + 1}. astea · ${panel.dataset.weekStart.replaceAll('-', '/')} — ${panel.dataset.weekEnd.replaceAll('-', '/')}`;
  }
  tabs.forEach((tab, index) => tab.addEventListener('click', () => showWeek(index)));
  previous?.addEventListener('click', () => showWeek(current - 1));
  next?.addEventListener('click', () => showWeek(current + 1));
  document.querySelector('[data-week-start]')?.addEventListener('click', () => showWeek(0));
  const todayButton = document.querySelector('[data-week-today]');
  const todayIndex = panels.findIndex(panel => panel.dataset.weekStart <= today && panel.dataset.weekEnd >= today);
  if (todayButton) todayButton.disabled = todayIndex < 0;
  todayButton?.addEventListener('click', () => showWeek(todayIndex));
  showWeek(current);

  const visibility = document.querySelector('[data-module-visibility]');
  const filter = document.querySelector('[data-module-filter]');
  function applyFilters() {
    const showModules = visibility?.checked !== false;
    const selected = filter?.value || '';
    document.querySelectorAll('[data-module-row]').forEach(row => {
      const moduleId = row.dataset.moduleRow;
      if (!moduleId) return;
      row.hidden = !showModules || (selected && selected !== moduleId);
    });
  }
  visibility?.addEventListener('change', applyFilters);
  filter?.addEventListener('change', applyFilters);

  const dialog = document.querySelector('#kronograma-dialog');
  const form = dialog?.querySelector('[data-kronograma-form]');
  if (!dialog || !form) return;
  const type = form.elements.mota;
  const moduleField = form.querySelector('[data-module-field]');
  const plannedModuleField = form.querySelector('[data-planned-module-field]');
  const save = form.querySelector('[data-activity-save]');
  const remove = form.querySelector('[data-activity-delete]');
  const readonlyNote = form.querySelector('[data-readonly-note]');
  const editableFields = Array.from(form.querySelectorAll('input:not([type=hidden]), textarea, select'));

  function refreshType() {
    const common = type.value === 'KOMUNA';
    moduleField.hidden = common;
    plannedModuleField.hidden = !common;
    form.elements.moduloaId.required = !common;
  }
  type?.addEventListener('change', refreshType);
  dialog.querySelectorAll('[data-dialog-close]').forEach(button => button.addEventListener('click', () => dialog.close()));

  function openNew(date = panels[current].dataset.weekStart, moduleId = '', activityType = '') {
    form.reset();
    form.elements.id.value = '';
    form.elements.hasieraData.value = date;
    if (activityType) type.value = activityType;
    if (moduleId) form.elements.moduloaId.value = moduleId;
    if (!Array.from(type.options).some(option => option.value === type.value)) type.selectedIndex = 0;
    editableFields.forEach(field => { field.disabled = false; });
    readonlyNote.hidden = true;
    save.hidden = false;
    remove.hidden = true;
    dialog.querySelector('[data-dialog-title]').textContent = 'Jarduera berria';
    refreshType();
    dialog.showModal();
  }
  document.querySelector('[data-kronograma-new]')?.addEventListener('click', () => openNew());
  document.querySelectorAll('[data-kronograma-cell]').forEach(cell => cell.addEventListener('click', () => {
    openNew(cell.dataset.date, cell.dataset.moduleId || '', cell.dataset.activityType || '');
  }));

  document.querySelectorAll('[data-activity-id]').forEach(button => button.addEventListener('click', () => {
    const d = button.dataset;
    form.elements.id.value = d.activityId;
    form.elements.izenburua.value = d.title || '';
    form.elements.deskribapena.value = d.description || '';
    form.elements.hasieraData.value = d.start;
    form.elements.iraupenOrduak.value = d.hours;
    type.value = d.type;
    form.elements.moduloaId.value = d.moduleId || '';
    form.elements.aurreikusitakoModuloaId.value = d.plannedModuleId || '';
    const editable = d.editable === 'true';
    editableFields.forEach(field => { field.disabled = !editable; });
    readonlyNote.hidden = editable;
    save.hidden = !editable;
    remove.hidden = !editable;
    dialog.querySelector('[data-dialog-title]').textContent = editable ? 'Jarduera editatu' : 'Jardueraren xehetasunak';
    refreshType();
    dialog.showModal();
  }));

  remove?.addEventListener('click', () => {
    if (!window.confirm('Jarduera ezabatu?')) return;
    const deleteForm = document.querySelector('[data-delete-form]');
    deleteForm.action = `${deleteForm.dataset.deleteBase}/${form.elements.id.value}/ezabatu`;
    deleteForm.submit();
  });
})();
