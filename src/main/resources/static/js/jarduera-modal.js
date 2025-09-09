// Helper txikia: elementuak jasotzeko eta akatsa argiago emateko
function byId(id) {
  const el = document.getElementById(id);
  if (!el) throw new Error(`Element not found: #${id}`);
  return el;
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
  byId('jardueraMota').value = 'planifikatua'; // default
  byId('jardueraModalTitle').textContent = 'Jarduera berria';
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
      byId('jardueraTitulua').value = j.titulua || '';
      byId('jardueraDeskribapena').value = j.deskribapena || '';
      byId('jardueraOrduak').value = (j.orduak ?? 1);
      byId('jardueraMota').value = j.mota || 'planifikatua';

      byId('jardueraModalTitle').textContent = 'Jarduera editatu';
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

function berretsiEtaEzabatu() {
  if (confirm('Ziur jarduera ezabatu nahi duzula?')) {
    byId('jardueraEzabatuForm').submit();
  }
}
