// /static/js/kudeatzaile-nav.js
window.KNav = (function(){
  function toggle(){
    const panel = document.getElementById('kNav');
    if (!panel) return;
    const btn = document.querySelector('.k-nav .k-hamb');
    const isOpen = panel.classList.toggle('k-open');
    if (btn) btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    panel.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
  }
  return { toggle };
})();
