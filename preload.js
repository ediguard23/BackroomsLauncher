'use strict';
/**
 * Puente entre la interfaz y el proceso principal. La interfaz no tiene
 * acceso a Node ni al disco: solo a estas funciones.
 */

const { contextBridge, ipcRenderer } = require('electron');

function escuchar (canal) {
  return (fn) => {
    const h = (e, datos) => fn(datos);
    ipcRenderer.on(canal, h);
    return () => ipcRenderer.removeListener(canal, h);
  };
}

contextBridge.exposeInMainWorld('launcher', {
  estado: () => ipcRenderer.invoke('estado'),
  guardarConfig: (cambios) => ipcRenderer.invoke('config:set', cambios),
  loginMicrosoft: () => ipcRenderer.invoke('cuenta:login'),
  logout: () => ipcRenderer.invoke('cuenta:logout'),
  manifest: () => ipcRenderer.invoke('evento:manifest'),
  ping: () => ipcRenderer.invoke('evento:ping'),
  acceso: (nombre) => ipcRenderer.invoke('evento:acceso', nombre),
  jugar: () => ipcRenderer.invoke('jugar'),
  abrirEnlace: (cual) => ipcRenderer.invoke('abrir-enlace', cual),
  minimizar: () => ipcRenderer.invoke('ventana:minimizar'),
  cerrar: () => ipcRenderer.invoke('ventana:cerrar'),
  instalarActualizacion: () => ipcRenderer.invoke('actualizacion:instalar'),
  onProgreso: escuchar('progreso'),
  onJuego: escuchar('juego'),
  onActualizacion: escuchar('actualizacion')
});
