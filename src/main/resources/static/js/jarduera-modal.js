// Sortzeko (egunean klik)
window.irekiJardueraModala = function(td){
  const data = td.getAttribute('data-date');
  if(!data) return;
  // reset -> sortu modua
  document.getElementById('jardueraForm').reset();
  document.getElementById('jardueraId').value = '';
  document.getElementById('jardueraData').value = data;
  document.getElementById('jardueraModalTitle').textContent = 'Jarduera berria';
  document.getElementById('jardueraModal').style.display = 'flex';
};

// Editatzeko (tituluan klik)
window.editatuJarduera = function(evt, el){
  evt.stopPropagation();
  const id = el.getAttribute('data-id');

  fetch(`/irakasle/denboralizazioa/jarduera/${id}`, {credentials: 'same-origin'})
    .then(r => {
      if (!r.ok) {
        throw new Error(`Eskaria huts: ${r.status}`);
      }
      const ct = r.headers.get('content-type') || '';
      if (!ct.includes('application/json')) {
        throw new Error('Ez da JSON jaso (agian loginera birbideratu da?)');
      }
      return r.json();
    })
    .then(j => {
      document.getElementById('jardueraForm').reset();
      document.getElementById('jardueraId').value = j.id;
      document.getElementById('jardueraData').value = j.data; // "yyyy-MM-dd"
      document.getElementById('jardueraTitulua').value = j.titulua || '';
      document.getElementById('jardueraDeskribapena').value = j.deskribapena || '';
      document.getElementById('jardueraOrduak').value = (j.orduak ?? 1);
      document.getElementById('jardueraMota').value = j.mota || 'planifikatua';
      document.getElementById('jardueraEginda').checked = !!j.eginda;

      document.getElementById('jardueraModalTitle').textContent = 'Jarduera editatu';
      document.getElementById('jardueraModal').style.display = 'flex';
    })
    .catch(err => {
      console.error('Editatzeko datuak jasotzean errorea:', err);
      alert(`Ezin izan dira datuak kargatu: ${err.message}`);
    });
};

// Itxi
window.itxiJardueraModala = function(){
  document.getElementById('jardueraModal').style.display = 'none';
  document.getElementById('jardueraForm').reset();
};
