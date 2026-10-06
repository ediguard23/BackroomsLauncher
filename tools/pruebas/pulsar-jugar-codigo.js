// Guion de prueba (BACKROOMS_PRUEBA): pulsa JUGAR y, si pide el codigo de entrada, lo escribe y canjea.
const t = setInterval(() => {
  const modal = document.getElementById('osd-entrada');
  if (modal && !modal.hidden) {
    clearInterval(t);
    const c = document.getElementById('codigo');
    c.value = 'BR-PRUE-BAPR-UEBA';
    c.dispatchEvent(new Event('input'));
    console.log('Codigo escrito');
    setTimeout(() => document.getElementById('codigo-ok').click(), 800);
    return;
  }
  const b = document.getElementById('btn-jugar');
  if (b && !b.disabled && !window.__pulsado) {
    window.__pulsado = true;
    console.log('JUGAR pulsado');
    b.click();
  }
}, 1000);
