// Helper txikia: elementuak jasotzeko eta akatsa argiago emateko
function byId(id) {
  const el = document.getElementById(id);
  if (!el) throw new Error(`Element not found: #${id}`);
  return el;
}

function eguneratuModalarenNabigazioa(data) {
  if (!data) return;
  const [urtea, hilabetea] = data.split('-');
  const modal = byId('jardueraModal');
  modal.querySelectorAll('input[name="urtea"]').forEach(input => { input.value = urtea; });
  modal.querySelectorAll('input[name="hilabetea"]').forEach(input => { input.value = Number(hilabetea); });

  const egutegiMota = byId('jardueraForm').querySelector('input[name="egutegiMota"]').value;
  if (egutegiMota === 'astea') {
    const eguna = new Date(`${data}T00:00:00Z`);
    eguna.setUTCDate(eguna.getUTCDate() - (eguna.getUTCDay() - 1));
    const asteHasiera = eguna.toISOString().slice(0, 10);
    modal.querySelectorAll('input[name="asteHasiera"]').forEach(input => { input.value = asteHasiera; });
  }
}

// Sortzeko (egunean klik)
window.irekiJardueraModala = function(td){
  const data = td.getAttribute('data-date');
  if(!data) return;

  // Jarduera berria: ezabatu botoia ezkutatu eta form nagusia prestatu
  ezkutatuEzabatuBotoia();

  byId('jardueraForm').reset();
  byId('jardueraId').value = '';
  byId('jardueraData').value = data;
  eguneratuModalarenNabigazioa(data);
  byId('jardueraMota').value = 'planifikatua'; // default
  byId('jardueraKalifikagarria').checked = false;
  eguneratuKalifikazioEremuak();
  const unitateaSelect = document.getElementById('jardueraUnitatea');
  const defaultUnitateaId = td.getAttribute('data-default-ud-id');
  if (unitateaSelect) {
    const aukera = defaultUnitateaId
      ? Array.from(unitateaSelect.options).some(opt => opt.value === defaultUnitateaId)
      : false;
    unitateaSelect.value = aukera ? defaultUnitateaId : '';
  }
  byId('jardueraModalTitle').textContent = 'Jarduera berria';
  byId('jardueraModal').querySelector('.modal-content').scrollTop = 0;
  byId('jardueraModal').style.display = 'flex';
};

// Editatzeko (tituluan klik)
window.editatuJarduera = function(evt, el){
  evt.stopPropagation();
  const id = el.getAttribute('data-id');

  fetch(`/irakasle/denboralizazioa/jarduera/${id}`, {credentials: 'same-origin'})
    .then(r => {
      if (!r.ok) throw new Error(`Eskaria huts: ${r.status}`);
      const ct = r.headers.get('content-type') || '';
      if (!ct.includes('application/json')) throw new Error('Ez da JSON jaso (loginera birbideratu ote?)');
      return r.json();
    })
    .then(j => {
      // Form nagusia bete (EGINDA EZ DAGO JADA; MOTA BAKARRIK)
      byId('jardueraForm').reset();
      byId('jardueraId').value = j.id;
      byId('jardueraData').value = j.data; // "yyyy-MM-dd"
      eguneratuModalarenNabigazioa(j.data);
      byId('jardueraTitulua').value = j.titulua || '';
      byId('jardueraDeskribapena').value = j.deskribapena || '';
      byId('jardueraOrduak').value = (j.orduak ?? 1);
      byId('jardueraMota').value = j.mota || 'planifikatua';
      byId('jardueraKalifikagarria').checked = !!j.kalifikagarria;
      byId('jardueraPisua').value = (j.pisua ?? '');
      byId('jardueraMoodleEsteka').value = j.moodleEsteka || '';
      eguneratuKalifikazioEremuak(j.id);
      const unitateaSelect = document.getElementById('jardueraUnitatea');
      if (unitateaSelect) unitateaSelect.value = j.unitateaId ? String(j.unitateaId) : '';

      byId('jardueraModalTitle').textContent = 'Jarduera editatu';
      byId('jardueraModal').querySelector('.modal-content').scrollTop = 0;
      byId('jardueraModal').style.display = 'flex';

      // Editatzeko: ezabatu botoia prestatu (action eta ikusgai)
      erakutsiEzabatuBotoia(j.id);
    })
    .catch(err => {
      console.error('Editatzeko datuak jasotzean errorea:', err);
      alert(`Ezin izan dira datuak kargatu: ${err.message}`);
    });
};

// Itxi
window.itxiJardueraModala = function(){
  byId('jardueraModal').style.display = 'none';
  byId('jardueraForm').reset();
  ezkutatuEzabatuBotoia();
};

// ---- Ezabatu botoiaren kudeaketa ----
function erakutsiEzabatuBotoia(jardueraId) {
  const ezabatuBtn  = byId('jardueraEzabatuBtn');
  const ezabatuForm = byId('jardueraEzabatuForm');
  ezabatuForm.action = '/irakasle/denboralizazioa/jarduera/' + jardueraId + '/ezabatu';
  ezabatuBtn.style.display = 'inline-block';
}

function ezkutatuEzabatuBotoia() {
  const ezabatuBtn = document.getElementById('jardueraEzabatuBtn');
  if (ezabatuBtn) ezabatuBtn.style.display = 'none';
}

window.eguneratuKalifikazioEremuak = function(jardueraId) {
  const aktibo = byId('jardueraKalifikagarria').checked;
  byId('jardueraKalifikazioEremuak').style.display = aktibo ? 'block' : 'none';
  const botoia = byId('jardueraKalifikazioakBtn');
  const id = jardueraId || byId('jardueraId').value;
  botoia.style.display = aktibo && id ? 'inline-flex' : 'none';
  botoia.href = id ? `/irakasle/notak/jarduerak?jardueraId=${encodeURIComponent(id)}` : '#';
};

function berretsiEtaEzabatu() {
  if (confirm('Ziur jarduera ezabatu nahi duzula?')) {
    byId('jardueraEzabatuForm').submit();
  }
}
