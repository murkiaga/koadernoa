// /static/js/irakasle-nav.js
window.TNav = (function(){
  function toggle(){
    const panel = document.getElementById('tNav');
    if (!panel) return;
    const btn = document.querySelector('.t-nav .t-hamb');
    const isOpen = panel.classList.toggle('t-open');
    if (btn) btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    panel.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
  }
  return { toggle };
})();
