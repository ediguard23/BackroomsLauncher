; Instalador del Backrooms Launcher (lo incluye electron-builder: ver electron-builder.yml).
;
;  - Pantalla de bienvenida con la barra lateral del evento (assets/instalador/lateral.bmp).
;  - Al final, dos casillas marcadas que el jugador puede quitar: abrir el launcher y crear
;    el acceso directo del escritorio. El del menu Inicio se crea siempre: asi sale al
;    buscar "Backrooms" en Windows.
;  - Al desinstalar se borra el acceso directo del escritorio, si se creo (no al actualizar).
;
; Las imagenes se rehacen con tools/instalador/generar.ps1. Este archivo va en UTF-8 con BOM
; para que NSIS lea bien los acentos.

!macro customWelcomePage
  !define MUI_WELCOMEPAGE_TITLE "${PRODUCT_NAME}"
  !define MUI_WELCOMEPAGE_TEXT "Vas a instalar el launcher del evento BACKROOMS de PeakMC Studio.$\r$\n$\r$\nInstala Minecraft 1.21.11 con todo lo del evento y se mantiene al día solo. Para entrar necesitarás tu código de entrada: te lo dan al comprarla en tienda.peakmc.lat/eventos.$\r$\n$\r$\nPulsa Siguiente para continuar."
  !insertmacro MUI_PAGE_WELCOME
!macroend

!macro customFinishPage
  Function StartApp
    ${if} ${isUpdated}
      StrCpy $1 "--updated"
    ${else}
      StrCpy $1 ""
    ${endif}
    ${StdUtils.ExecShellAsUser} $0 "$launchLink" "open" "$1"
  FunctionEnd

  Function CrearAccesoEscritorio
    CreateShortCut "$newDesktopLink" "$appExe" "" "$appExe" 0 "" "" "${APP_DESCRIPTION}"
    ClearErrors
    WinShell::SetLnkAUMI "$newDesktopLink" "${APP_ID}"
    System::Call 'Shell32::SHChangeNotify(i 0x8000000, i 0, i 0, i 0)'
  FunctionEnd

  !define MUI_FINISHPAGE_TITLE "Todo listo"
  !define MUI_FINISHPAGE_TEXT "${PRODUCT_NAME} ya está instalado. Lo encontrarás en el menú Inicio: busca «Backrooms»."
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_TEXT "Abrir ${PRODUCT_NAME}"
  !define MUI_FINISHPAGE_RUN_FUNCTION "StartApp"
  !define MUI_FINISHPAGE_SHOWREADME
  !define MUI_FINISHPAGE_SHOWREADME_TEXT "Crear un acceso directo en el escritorio"
  !define MUI_FINISHPAGE_SHOWREADME_FUNCTION "CrearAccesoEscritorio"
  !insertmacro MUI_PAGE_FINISH
!macroend

!macro customUnInstall
  ${ifNot} ${isUpdated}
    WinShell::UninstShortcut "$DESKTOP\${SHORTCUT_NAME}.lnk"
    Delete "$DESKTOP\${SHORTCUT_NAME}.lnk"
  ${endIf}
!macroend
