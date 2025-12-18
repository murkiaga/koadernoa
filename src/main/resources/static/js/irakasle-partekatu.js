(function(){
  const input = document.getElementById('shareIdentInput');
  const box   = document.getElementById('teacherSuggestions');
  if (!input || !box) return;

  let timer = null;

  input.addEventListener('input', function(){
    const q = input.value.trim();

    // 3 karaktere baino gutxiago → ez bilatu
    if (q.length < 3) {
      box.style.display = 'none';
      box.innerHTML = '';
      if (timer) clearTimeout(timer);
      return;
    }

    if (timer) clearTimeout(timer);
    timer = setTimeout(() => bilatu(q), 250);  // debounce txiki bat
  });

  function bilatu(q) {
    fetch('/irakasle/irakasle-aukera?q=' + encodeURIComponent(q))
      .then(res => res.ok ? res.json() : [])
      .then(data => {
        // bitartean erabiltzaileak beste zerbait idatzi badu, ez gainidatzi
        if (input.value.trim().length < 3) {
          box.style.display = 'none';
          box.innerHTML = '';
          return;
        }
        renderSuggestions(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        box.style.display = 'none';
        box.innerHTML = '';
      });
  }

  function renderSuggestions(list) {
    if (!list.length) {
      box.style.display = 'none';
      box.innerHTML = '';
      return;
    }

    box.innerHTML = '';
    list.forEach(item => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'teacher-suggestion';
      btn.textContent = item.label || item.value;
      btn.style.display = 'block';
      btn.style.width   = '100%';
      btn.style.textAlign = 'left';
      btn.style.padding = '.35rem .6rem';
      btn.style.border  = 'none';
      btn.style.background = '#fff';
      btn.style.fontSize = '.85rem';
      btn.style.cursor  = 'pointer';

      btn.addEventListener('mouseover', () => {
        btn.style.background = '#eff6ff';
      });
      btn.addEventListener('mouseout', () => {
        btn.style.background = '#fff';
      });

      btn.addEventListener('click', () => {
        input.value = item.value;   // ident balioa (email edo izena)
        box.style.display = 'none';
        box.innerHTML = '';
        input.focus();
      });

      box.appendChild(btn);
    });

    box.style.display = 'block';
  }

  // kanpoan klik eginez gero, itxi dropdown-a
  document.addEventListener('click', (e) => {
    if (!box.contains(e.target) && e.target !== input) {
      box.style.display = 'none';
      box.innerHTML = '';
    }
  });
})();