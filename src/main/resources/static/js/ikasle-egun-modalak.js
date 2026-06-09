(function () {
  let unekoa = { ikasleaId: null, data: null, izena: null, trigger: null };
  const csrf = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

  function erakutsiErrorea(id, mezua) {
    const elementua = document.getElementById(id);
    elementua.textContent = mezua;
    elementua.style.display = 'block';
  }

  function itxiDenak() {
    document.getElementById('oharPopup')?.classList.remove('open');
    document.getElementById('jokabideModal')?.classList.remove('open');
  }
  window.itxiIkasleEgunModalak = itxiDenak;

  function kokatuPopup(trigger) {
    const popup = document.getElementById('oharPopup');
    const rect = trigger.getBoundingClientRect();
    const tartea = 8;
    popup.classList.add('open');

    const popupRect = popup.getBoundingClientRect();
    let left = rect.right + tartea;
    let top = rect.top;
    if (left + popupRect.width > window.innerWidth - tartea) left = rect.left - popupRect.width - tartea;
    if (left < tartea) left = tartea;
    if (top + popupRect.height > window.innerHeight - tartea) top = window.innerHeight - popupRect.height - tartea;
    if (top < tartea) top = tartea;
    popup.style.left = `${left}px`;
    popup.style.top = `${top}px`;
  }

  function ezarriTestuingurua(ikasleaId, data, izena, trigger) {
    unekoa = { ikasleaId, data, izena, trigger };
    document.getElementById('oharIkaslea').textContent = izena;
    document.getElementById('oharData').textContent = data;
    document.getElementById('jokIkaslea').textContent = izena;
    document.getElementById('jokData').textContent = data;
  }

  window.irekiIkasleEgunOharra = async function (ikasleaId, data, izena, trigger) {
    itxiDenak();
    ezarriTestuingurua(ikasleaId, data, izena, trigger);
    document.getElementById('oharErrorea').style.display = 'none';
    document.getElementById('oharTestua').value = '';
    kokatuPopup(trigger);

    try {
      const erantzuna = await fetch(`/irakasle/oharrak?ikasleaId=${ikasleaId}&data=${data}`);
      const json = await erantzuna.json();
      if (!erantzuna.ok) throw new Error(json.errorea);
      document.getElementById('oharTestua').value = json.testua || '';
    } catch (errorea) {
      erakutsiErrorea('oharErrorea', errorea.message);
    }
  };

  window.irekiJokabideDesegokia = function (ikasleaId, data, izena, trigger) {
    ezarriTestuingurua(ikasleaId, data, izena, trigger);
    window.irekiJokabideModal();
  };

  window.gordeOharra = async function () {
    const parametroak = new URLSearchParams({
      ikasleaId: unekoa.ikasleaId,
      data: unekoa.data,
      testua: document.getElementById('oharTestua').value
    });
    await bidali('/irakasle/oharrak', 'POST', parametroak, 'oharErrorea', function () {
      markatuAktibo('oharra');
      itxiDenak();
    });
  };

  window.ezabatuOharra = async function () {
    const parametroak = new URLSearchParams({ ikasleaId: unekoa.ikasleaId, data: unekoa.data });
    await bidali(`/irakasle/oharrak?${parametroak}`, 'DELETE', null, 'oharErrorea', function () {
      markatuInaktibo('oharra');
      document.getElementById('oharTestua').value = '';
      itxiDenak();
    });
  };

  window.irekiJokabideModal = async function () {
    itxiDenak();
    document.getElementById('jokErrorea').style.display = 'none';
    document.getElementById('jokabideModal').classList.add('open');
    try {
      const erantzuna = await fetch('/irakasle/jokabide-desegokia/form-data');
      const json = await erantzuna.json();
      if (!erantzuna.ok) throw new Error(json.errorea || 'Ezin izan dira aukerak kargatu.');
      beteSelect('portaeraArrazoia', json.portaeraArrazoiak, json.portaeraArrazoiaDefektuzkoaId);
      beteSelect('neurriZuzentzailea', json.neurriZuzentzaileak, json.neurriZuzentzaileaDefektuzkoaId);
    } catch (errorea) {
      erakutsiErrorea('jokErrorea', errorea.message);
    }
  };

  window.irekiOharPopupBerriro = function () {
    itxiDenak();
    if (unekoa.trigger) kokatuPopup(unekoa.trigger);
  };

  window.sortuJokabidea = async function () {
    const parametroak = new URLSearchParams({
      ikasleaId: unekoa.ikasleaId,
      data: unekoa.data,
      portaeraArrazoiaId: document.getElementById('portaeraArrazoia').value,
      neurriZuzentzaileaId: document.getElementById('neurriZuzentzailea').value,
      deskribapenZehatza: document.getElementById('deskribapenZehatza').value
    });
    await bidali('/irakasle/jokabide-desegokia', 'POST', parametroak, 'jokErrorea', function (json) {
      markatuAktibo('jokabidea', json.pdfUrl);
      document.getElementById('deskribapenZehatza').value = '';
      itxiDenak();
    });
  };

  function beteSelect(id, elementuak, defektuzkoaId) {
    const select = document.getElementById(id);
    select.innerHTML = '';
    elementuak.forEach(function (elementua) {
      const option = new Option(`${elementua.kodea} - ${elementua.testua}`, elementua.id);
      option.selected = String(elementua.id) === String(defektuzkoaId);
      select.add(option);
    });
  }

  async function bidali(url, method, body, erroreId, ondo) {
    try {
      const erantzuna = await fetch(url, {
        method,
        headers: {
          ...(body ? { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' } : {}),
          [csrfHeader]: csrf || ''
        },
        body: body?.toString()
      });
      const json = await erantzuna.json();
      if (!erantzuna.ok) throw new Error(json.errorea || 'Errorea gertatu da.');
      ondo(json);
    } catch (errorea) {
      erakutsiErrorea(erroreId, errorea.message);
    }
  }

  function aurkituIkonoa(mota) {
    if (!unekoa.trigger) return null;
    if (unekoa.trigger.matches?.(`[data-mota="${mota}"]`)) return unekoa.trigger;
    return unekoa.trigger.querySelector?.(`[data-mota="${mota}"]`) || null;
  }

  function markatuAktibo(mota, pdfUrl) {
    let ikonoa = aurkituIkonoa(mota);
    if (!ikonoa && unekoa.trigger?.classList.contains('ikasle-egun-gelaxka')) {
      let ikonoKutxa = unekoa.trigger.querySelector('.ie-ikonoak');
      if (!ikonoKutxa) {
        ikonoKutxa = document.createElement('span');
        ikonoKutxa.className = 'ie-ikonoak';
        unekoa.trigger.appendChild(ikonoKutxa);
      }
      ikonoa = document.createElement('span');
      ikonoa.dataset.mota = mota;
      ikonoa.title = mota === 'oharra' ? 'Oharra dago' : 'Jokabide desegokia dago';
      ikonoa.textContent = mota === 'oharra' ? '📝' : '⚠️';
      ikonoKutxa.append(' ', ikonoa);
    }
    if (ikonoa) {
      ikonoa.classList.remove('inaktibo');
      ikonoa.classList.add('aktibo');
      ikonoa.setAttribute('aria-label', mota === 'oharra' ? 'Oharra dago' : 'Jokabide desegokia dago');
    }
    if (mota === 'jokabidea' && pdfUrl && unekoa.trigger?.parentElement) {
      let pdfak = unekoa.trigger.parentElement.querySelector('.ikasle-egun-pdfak');
      if (!pdfak) {
        pdfak = document.createElement('span');
        pdfak.className = 'ikasle-egun-pdfak';
        unekoa.trigger.insertAdjacentElement('afterend', pdfak);
      }
      const esteka = document.createElement('a');
      esteka.href = pdfUrl;
      esteka.target = '_blank';
      esteka.title = 'PDFa ikusi';
      esteka.textContent = '📄';
      pdfak.appendChild(esteka);
    }
  }

  function markatuInaktibo(mota) {
    const ikonoa = aurkituIkonoa(mota);
    if (!ikonoa) return;
    if (unekoa.trigger?.classList.contains('ikasle-egun-gelaxka')) {
      ikonoa.remove();
      return;
    }
    ikonoa.classList.remove('aktibo');
    ikonoa.classList.add('inaktibo');
    ikonoa.setAttribute('aria-label', 'Ez dago oharrik');
  }

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') itxiDenak();
  });
  document.addEventListener('click', function (event) {
    const popup = document.getElementById('oharPopup');
    if (popup?.classList.contains('open') && !popup.contains(event.target) && !unekoa.trigger?.contains(event.target)) {
      popup.classList.remove('open');
    }
    if (event.target.classList.contains('ie-modal')) itxiDenak();
  });
  window.addEventListener('resize', function () {
    if (document.getElementById('oharPopup')?.classList.contains('open') && unekoa.trigger) kokatuPopup(unekoa.trigger);
  });
})();
