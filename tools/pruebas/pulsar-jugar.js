// Guion de prueba (BACKROOMS_PRUEBA): pulsa JUGAR en cuanto este habilitado.
const t = setInterval(() => {
  const b = document.getElementById('btn-jugar');
  if (b && !b.disabled) {
    clearInterval(t);
    console.log('JUGAR pulsado');
    b.click();
  }
}, 1000);
