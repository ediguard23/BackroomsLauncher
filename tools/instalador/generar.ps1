# Imagenes del instalador (NSIS) del Backrooms Launcher:
#
#   powershell -ExecutionPolicy Bypass -File tools/instalador/generar.ps1
#
#   assets/instalador/lateral.bmp    164x314, la barra de las pantallas de bienvenida y final
#   assets/instalador/cabecera.bmp   150x57, arriba a la derecha en el resto de pantallas
#   assets/icon.ico                  el icono (16 a 256 px) del instalador, el desinstalador y la app
#
# Salen de assets/logo-backrooms.png, assets/logo-peakmc-studio.png y assets/icon.png.
# NSIS solo acepta BMP de 24 bits para las dos primeras.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$raiz = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$assets = Join-Path $raiz 'assets'
$salida = Join-Path $assets 'instalador'
New-Item -ItemType Directory -Force $salida | Out-Null

function Lienzo($w, $h) {
    $b = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($b)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    return @($b, $g)
}

function TextoCentrado($g, $texto, $fuente, $color, $ancho, $y, $espaciado) {
    # letra a letra, con espaciado, como los rotulos del launcher
    $pincel = New-Object System.Drawing.SolidBrush $color
    $formato = [System.Drawing.StringFormat]::GenericTypographic
    $anchos = @()
    # con GenericTypographic el espacio mide 0: se le da el ancho de media letra
    foreach ($c in $texto.ToCharArray()) { $anchos += $(if ($c -eq [char]32) { $fuente.Size * 0.7 } else { $g.MeasureString([string]$c, $fuente, 1000, $formato).Width }) }
    $total = ($anchos | Measure-Object -Sum).Sum + $espaciado * ($texto.Length - 1)
    $x = ($ancho - $total) / 2
    $i = 0
    foreach ($c in $texto.ToCharArray()) {
        $g.DrawString([string]$c, $fuente, $pincel, [single]$x, [single]$y, $formato)
        $x += $anchos[$i] + $espaciado
        $i++
    }
    $pincel.Dispose()
}

# ------------------------------------------------------------------ lateral
$w = 164; $h = 314
$l = Lienzo $w $h; $b = $l[0]; $g = $l[1]
$fondo = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.Point 0, 0), (New-Object System.Drawing.Point 0, $h), ([System.Drawing.Color]::FromArgb(255, 30, 26, 14)), ([System.Drawing.Color]::FromArgb(255, 10, 9, 5))
$g.FillRectangle($fondo, 0, 0, $w, $h)
# papel pintado: chevrones muy suaves
$raya = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(14, 232, 211, 106)), 3
for ($y = -20; $y -lt $h + 20; $y += 12) {
    for ($x = 0; $x -lt $w; $x += 16) {
        $g.DrawLines($raya, @((New-Object System.Drawing.Point $x, ($y + 6)), (New-Object System.Drawing.Point ($x + 8), $y), (New-Object System.Drawing.Point ($x + 16), ($y + 6))))
    }
}
# resplandor del fluorescente detras del logo
$camino = New-Object System.Drawing.Drawing2D.GraphicsPath
$camino.AddEllipse(-30, 10, $w + 60, 190)
$halo = New-Object System.Drawing.Drawing2D.PathGradientBrush $camino
$halo.CenterColor = [System.Drawing.Color]::FromArgb(70, 243, 224, 138)
$halo.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 243, 224, 138))
$g.FillPath($halo, $camino)
# logo del evento
$logo = [System.Drawing.Image]::FromFile((Join-Path $assets 'logo-backrooms.png'))
$lw = 142; $lh = [int]($logo.Height * $lw / $logo.Width)
$g.DrawImage($logo, [int](($w - $lw) / 2), 34, $lw, $lh)
# lema
$f1 = New-Object System.Drawing.Font 'Consolas', 8.5, ([System.Drawing.FontStyle]::Bold)
TextoCentrado $g 'NIVEL 0' $f1 ([System.Drawing.Color]::FromArgb(255, 243, 224, 138)) $w (34 + $lh + 14) 3
$f2 = New-Object System.Drawing.Font 'Consolas', 6.5
# la É va por su código: PowerShell 5.1 lee este archivo como ANSI
TextoCentrado $g ('NO OS SEPAR' + [char]0x00C9 + 'IS') $f2 ([System.Drawing.Color]::FromArgb(200, 239, 230, 200)) $w (34 + $lh + 30) 2
# PeakMC Studio abajo
$peak = [System.Drawing.Image]::FromFile((Join-Path $assets 'logo-peakmc-studio.png'))
$pw = 104; $ph = [int]($peak.Height * $pw / $peak.Width)
$f3 = New-Object System.Drawing.Font 'Consolas', 6
TextoCentrado $g 'UN EVENTO DE' $f3 ([System.Drawing.Color]::FromArgb(150, 239, 230, 200)) $w ($h - $ph - 30) 2
$g.DrawImage($peak, [int](($w - $pw) / 2), $h - $ph - 16, $pw, $ph)
$b.Save((Join-Path $salida 'lateral.bmp'), [System.Drawing.Imaging.ImageFormat]::Bmp)
$g.Dispose(); $b.Dispose()

# ----------------------------------------------------------------- cabecera
$w = 150; $h = 57
$l = Lienzo $w $h; $b = $l[0]; $g = $l[1]
$g.Clear([System.Drawing.Color]::White)
# solo el rotulo BACKROOMS (la franja de abajo del logo), centrado
$fy = [int]($logo.Height * 0.735); $fh = $logo.Height - $fy
$rw = 138; $rh = [int]($fh * $rw / $logo.Width)
$g.DrawImage($logo, (New-Object System.Drawing.Rectangle ([int](($w - $rw) / 2)), ([int](($h - $rh) / 2)), $rw, $rh), (New-Object System.Drawing.Rectangle 0, $fy, $logo.Width, $fh), [System.Drawing.GraphicsUnit]::Pixel)
$b.Save((Join-Path $salida 'cabecera.bmp'), [System.Drawing.Imaging.ImageFormat]::Bmp)
$g.Dispose(); $b.Dispose()
$logo.Dispose(); $peak.Dispose()

# --------------------------------------------------------------------- icono
# ICO con PNG dentro (Windows Vista en adelante), de 16 a 256 px
$icono = [System.Drawing.Image]::FromFile((Join-Path $assets 'icon.png'))
$tamanos = @(16, 24, 32, 48, 64, 128, 256)
$pngs = @()
foreach ($t in $tamanos) {
    $bm = New-Object System.Drawing.Bitmap $t, $t, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $gg = [System.Drawing.Graphics]::FromImage($bm)
    $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $gg.DrawImage($icono, 0, 0, $t, $t)
    $ms = New-Object System.IO.MemoryStream
    $bm.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngs += , $ms.ToArray()
    $gg.Dispose(); $bm.Dispose(); $ms.Dispose()
}
$icono.Dispose()
$fs = [System.IO.File]::Create((Join-Path $assets 'icon.ico'))
$bw = New-Object System.IO.BinaryWriter $fs
$bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]$tamanos.Count)
$offset = 6 + 16 * $tamanos.Count
for ($i = 0; $i -lt $tamanos.Count; $i++) {
    $t = $tamanos[$i]
    $lado = if ($t -ge 256) { 0 } else { $t }
    $bw.Write([byte]$lado); $bw.Write([byte]$lado); $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Write([UInt16]1); $bw.Write([UInt16]32)
    $bw.Write([UInt32]$pngs[$i].Length); $bw.Write([UInt32]$offset)
    $offset += $pngs[$i].Length
}
foreach ($p in $pngs) { $bw.Write($p) }
$bw.Close(); $fs.Close()

Get-ChildItem $salida, (Join-Path $assets 'icon.ico') | Select-Object Name, Length | Format-Table -AutoSize
