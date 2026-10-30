# El esqueleto articulado (figuras de las cinemáticas)

Las figuras humanas de las cinemáticas (el otro superviviente del final, las manos del jugador) tienen un
**esqueleto de verdad**: cada parte del cuerpo es un hueso con su articulación (pivote) y cuelga de su
padre, así que al girar el hombro se mueven el antebrazo y la mano, al doblar la rodilla se mueve el pie, etc.
Se conserva el aspecto de Minecraft (cubos, piel de 64×64 en formato de jugador), pero se doblan codos,
rodillas, muñecas, tobillos y cintura.

## Qué hay

| Archivo | Qué es |
|---|---|
| `evento/.../esqueleto/Hueso.java` | Los 15 huesos, su padre, su pivote y su trozo de piel (datos, cliente y servidor) |
| `evento/.../esqueleto/Pose.java` | Una postura: el giro de cada hueso + el cuerpo entero (tumbado, desplazado). Se mezclan, se suman y se espejan |
| `evento/.../esqueleto/Animacion.java` | Fotogramas clave: en cada segundo, una pose (fija o que cambia con el tiempo); entre una y otra, suave o lineal |
| `evento/.../esqueleto/Ciclos.java` | Poses y ciclos de base: quieto (respira), caminar, correr (con prisa), agachado, mirar atrás, susto, boca arriba, forcejeo, manos |
| `evento/.../esqueleto/Figura.java` | La entidad (solo de cliente): piel, pose de cada fotograma (`animador`) y huesos ocultos |
| `evento/.../cliente/esqueleto/ModeloArticulado.java` | El modelo: un `ModelPart` por hueso, colgados como dice `Hueso`, con el pivote en la articulación |
| `evento/.../cliente/esqueleto/FiguraRenderer.java` | Lo dibuja con la pose del fotograma |
| `evento/.../cliente/esqueleto/Muestrario.java` | Para probar y diseñar: la orden de prueba `figura <animacion> [lado]` |
| `tools/texturas/superviviente.js` | Las pieles: `superviviente` (el traje amarillo del evento) y `superviviente_sangre` |

## Los huesos

```
CADERA ─ TORSO ─ CABEZA
  │        ├─ BRAZO_D ─ ANTEBRAZO_D ─ MANO_D     (hombro, codo, muñeca)
  │        └─ BRAZO_I ─ ANTEBRAZO_I ─ MANO_I
  ├─ MUSLO_D ─ PIERNA_D ─ PIE_D                  (cadera, rodilla, tobillo)
  └─ MUSLO_I ─ PIERNA_I ─ PIE_I
```

La geometría es la de un jugador partida por las articulaciones (brazo 6 + antebrazo 4 + mano 2 de los 12
píxeles; igual la pierna; el cuerpo, 8 de pecho y 4 de pelvis), y cada trozo toma de la piel justo sus filas:
por eso no se ven cortes ni costuras.

**Giros** (grados en `Pose.gira`): en brazos y piernas, x negativo lleva el miembro **hacia delante**; en el
antebrazo, x negativo **dobla el codo**; en la pierna (espinilla), x positivo **dobla la rodilla**; en el torso, x
positivo **inclina hacia delante**; en la cabeza, x positivo **mira abajo**. y gira a un lado, z ladea. El cuerpo
entero (`Pose.cuerpo`) gira sobre los pies: x −88 lo tumba boca arriba y +88 boca abajo.

## Cómo hacer una animación nueva

1. Una pose fija se escribe hueso a hueso:

   ```java
   Pose agarrarPomo = new Pose()
       .gira(Hueso.BRAZO_D, -80, 0, -10)     // el brazo hacia delante
       .gira(Hueso.ANTEBRAZO_D, -15, 0, 0)   // el codo casi recto
       .gira(Hueso.MANO_D, 0, 0, 30)         // la muñeca girada hacia el pomo
       .gira(Hueso.TORSO, 8, 0, 0)           // se inclina un poco
       .gira(Hueso.CABEZA, 10, 0, 0);        // mira el pomo
   ```

2. Se combinan: `Pose.mezcla(a, b, k)` pasa de una a otra; `a.mas(capa, peso)` suma una capa encima (correr y,
   encima, `Ciclos.mirarAtras`); `p.espejo()` cambia derecha por izquierda.

3. Se encadenan en el tiempo con `Animacion`:

   ```java
   Animacion a = new Animacion()
       .clave(0.0F, t -> Ciclos.correr(t * 1.4F, 1.0F))   // corriendo (1,4 ciclos por segundo)
       .clave(1.2F, t -> Ciclos.correr(t * 1.4F, 1.0F))
       .claveLineal(1.4F, tropieza)                         // sin frenar: golpes y caídas
       .clave(2.2F, Ciclos.bocaArriba());
   ```

   Los ciclos van por fase (1 = dos pasos): para que los pies no patinen, la fase sale de lo que avanza la
   figura (en el final: `distancia / 3.8` bloques por ciclo corriendo).

4. Se pone en una `Figura` (solo en el cliente que la crea):

   ```java
   Figura f = Entidades.FIGURA.create(mc.level, EntitySpawnReason.EVENT);
   f.piel = BackroomsEvento.id("textures/entity/superviviente.png");
   f.animador = (fig, parcial) -> a.en(segundos(parcial));
   mc.level.addEntity(f);
   ```

   Cada tick, quien la usa la mueve (`xo/yo/zo` y `setPos`, como las demás entidades de las cinemáticas).
   `f.ocultar(Hueso...)` esconde trozos.

   **El cuerpo del propio jugador** en una cinemática (la cámara va sola y el jugador de verdad no se dibuja):
   `cliente/esqueleto/CuerpoPrimeraPersona`. `crear(mc, piel, id, huesos visibles...)` hace la figura (la
   cabeza nunca se ve: estaría en la cámara) y cada tick `colocar(f, x, y, z, giro, adelante)` la pone con
   los pies 1,62 bajo los ojos de la cámara, mirando a donde mira ella. `pulsar(t, desde, hasta, pulsaciones,
   dentro)` es el brazo derecho marcando en una botonera que hay delante (sube, estira el codo de golpe en
   cada pulsación y baja). Lo usan los dos ascensores (la Fiesta y el final) y las manos con sangre del final
   (el cuerpo entero con el pecho justo detrás de los ojos: al bajar la vista suben los brazos desde abajo,
   unidos a los hombros). Lo que queda muy cerca de la cámara se ve enorme: mejor el cuerpo un poco detrás de
   los ojos que delante.

5. Para verla mientras se diseña: añadirla a `Muestrario.animacion` y, con el cliente de pruebas
   (`BACKROOMS_PRUEBAS=1`), escribir en `%TEMP%/backrooms-ordenes.txt` `figura <nombre> lado` (de perfil) o
   `figura <nombre>` (de frente). Sale delante del jugador 12 segundos.

## Límites

- Es para figuras de cinemática (solo de cliente, sin IA). Los jugadores siguen con el modelo de Minecraft
  (cambiarlo afectaría a todo el juego y a otros mods); las entidades del evento (Amasijo, Anfitrión...) tienen
  su propio modelo con su anatomía (`tools/texturas/entidades.js`) con la misma idea: el Anfitrión tiene codo
  (`antebrazo_*` cuelga de `brazo_*`) y rodilla (`espinilla_*` de `pierna_*`), y `ModeloAnfitrion` los dobla
  al correr. El Amasijo tiene, además, `embisteCliente` / `comeCliente` / `alertaCliente` (0..1, solo de
  cliente) que ponen las cinemáticas: salta con los brazos abiertos, come a tirones, se yergue alerta.
- La cinemática del /start (`cinematica.fsh`) no usa Figura: el investigador se dibuja en el shader, caja a
  caja, con la misma idea (muslo y espinilla, brazo y antebrazo, cada uno girando sobre su articulación;
  `partesInvestigador` y los campos `codoD/codoI` de `Pose`).
- La cara de abajo de manos y pies toma unos píxeles del lado de la piel (no se ve en la práctica).
