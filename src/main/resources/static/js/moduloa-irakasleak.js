(() => {
  const panel = document.getElementById('teacherPanel');
  if (!panel) return;
  const moduloId = panel.dataset.moduloId;
  const input = document.getElementById('teacherSearch');
  const suggestions = document.getElementById('teacherSuggestions');
  const list = document.getElementById('teacherList');
  const empty = document.getElementById('teacherEmpty');
  const status = document.getElementById('teacherStatus');
  const csrf = document.querySelector('meta[name="_csrf"]')?.content || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
  let timer;
  let lastQuery = '';

  const headers = () => ({ Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', [csrfHeader]: csrf });
  const updateEmpty = () => { empty.hidden = list.children.length > 0; };
  const hideSuggestions = () => { suggestions.hidden = true; suggestions.replaceChildren(); };

  function teacherRow(teacher) {
    const li = document.createElement('li');
    li.dataset.teacherId = teacher.id;
    const text = document.createElement('span');
    const strong = document.createElement('strong');
    strong.textContent = teacher.izena;
    const small = document.createElement('small');
    small.textContent = teacher.emaila || '';
    text.append(strong, document.createElement('br'), small);
    const remove = document.createElement('button');
    remove.type = 'button';
    remove.className = 'teacher-remove';
    remove.title = 'Irakaslea modulutik kendu';
    remove.setAttribute('aria-label', remove.title);
    remove.textContent = '×';
    li.append(text, remove);
    return li;
  }

  async function addTeacher(teacher) {
    const body = new URLSearchParams({ irakasleaId: teacher.id });
    const response = await fetch(`/kudeatzaile/moduloa/${moduloId}/irakasleak`, {
      method: 'POST', headers: headers(), credentials: 'same-origin', body
    });
    const result = await response.json();
    if (!response.ok || !result.ok) throw new Error(result.mezua || 'Ezin izan da irakaslea gehitu.');
    if (!list.querySelector(`[data-teacher-id="${result.id}"]`)) list.append(teacherRow(result));
    input.value = '';
    hideSuggestions();
    updateEmpty();
    status.textContent = 'Irakaslea gehitu da.';
  }

  async function removeTeacher(li) {
    const response = await fetch(`/kudeatzaile/moduloa/${moduloId}/irakasleak/${li.dataset.teacherId}/kendu`, {
      method: 'POST', headers: headers(), credentials: 'same-origin'
    });
    const result = await response.json();
    if (!response.ok || !result.ok) throw new Error(result.mezua || 'Ezin izan da irakaslea kendu.');
    li.remove();
    updateEmpty();
    status.textContent = 'Irakaslea kendu da.';
  }

  function renderSuggestions(items) {
    suggestions.replaceChildren();
    if (!items.length) { hideSuggestions(); return; }
    items.forEach(teacher => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'teacher-suggestion';
      button.textContent = `${teacher.izena}${teacher.emaila ? ` · ${teacher.emaila}` : ''}`;
      button.addEventListener('click', () => addTeacher(teacher).catch(e => { status.textContent = e.message; }));
      suggestions.append(button);
    });
    suggestions.hidden = false;
  }

  async function search(q) {
    if (q.length < 3) { hideSuggestions(); return; }
    lastQuery = q;
    const response = await fetch(`/kudeatzaile/moduloa/${moduloId}/irakasleak-bilatu?q=${encodeURIComponent(q)}`);
    if (!response.ok) { hideSuggestions(); return; }
    const items = await response.json();
    if (lastQuery === q) renderSuggestions(items);
  }

  input.addEventListener('input', () => {
    clearTimeout(timer);
    const q = input.value.trim();
    timer = setTimeout(() => search(q), 200);
  });
  list.addEventListener('click', event => {
    const button = event.target.closest('.teacher-remove');
    if (!button) return;
    removeTeacher(button.closest('li')).catch(e => { status.textContent = e.message; });
  });
  document.addEventListener('click', event => {
    if (event.target !== input && !suggestions.contains(event.target)) hideSuggestions();
  });
  updateEmpty();
})();
