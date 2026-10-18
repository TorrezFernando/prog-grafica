package com.graphics; // Reúne la versión final con las otras tres lecciones.

import java.util.List;

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CONTRARRELOJ, TRÁFICO, DECORACIÓN Y MINIMAPA.
 * Orden de lectura: estado, controles, animación, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Los semáforos son decorativos; tres vehículos autónomos recorren calles y ceden al jugador.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    protected boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    protected double tiempoAnimacion = 0; // Reloj continuo para los semáforos, independiente de objetivos.
    protected final Contrarreloj mision = new Contrarreloj(); // Conserva estado, tiempo y mejor marca de la carrera.
    protected static final List<PasoPeatonal> PASOS = PasoPeatonal.generar(MAPA, CELDA);
    protected final List<PeatonNpc> peatones = crearPeatones(); // Piloto de cuatro bots, ampliable sin cambiar la física.
    protected final PuntuacionConduccion puntuacion = new PuntuacionConduccion();
    protected final ChataRemolque chata = new ChataRemolque(INICIO_X, INICIO_Z, 0); // Solo el jugador posee un remolque.
    private float avisoAtropello;
    private PanelCarrera panel; // Crea el panel únicamente cuando existe un contexto gráfico y espacio útil.
    protected final VehiculoNpc[] trafico = { // Distribuye tres conductores en circuitos separados y transitables.
        new VehiculoNpc(52, 7, 180, 0.15f, 0.45f, 1), // Vehículo azul: avenidas exteriores.
        new VehiculoNpc(28, 6, 180, 0.65f, 0.25f, 0.95f), // Vehículo violeta: calles del núcleo original.
        new VehiculoNpc(12, 5, 0, 0.92f, 0.92f, 0.88f) // Vehículo blanco: circuito alrededor de la manzana central.
    };
    // X, Z, semiancho y semilargo: la misma geometría sirve para dibujo y colisión.
    protected static final float[][] OBSTACULOS = {
        {-31.6f, 10, 1.2f, 1.5f},
        {8, -31.6f, 1.5f, 1.2f},
        {48.4f, -10, 1.2f, 1.5f}
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia jugador, tráfico y misión, y devuelve los semáforos al comienzo del ciclo. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        chata.reiniciar(autoX, autoZ, angulo); // Restaura el enganche y elimina la articulación del intento anterior.
        tiempoAnimacion = 0; // Devuelve semáforos y baliza al inicio de su animación.
        mision.reiniciar(); // Prepara otro intento sin borrar la mejor marca de la sesión.
        for (VehiculoNpc npc : trafico) npc.reiniciar(); // Devuelve el tráfico a la misma distribución en cada intento.
        for (PeatonNpc peaton : peatones) peaton.reiniciar();
        puntuacion.reiniciar();
        avisoAtropello = 0;
    }

    // ==================== 3. ANIMACIÓN E INDICADORES ====================

    /** Actualiza la conducción y mantiene la animación durante todo el recorrido libre. */
    @Override // Amplía el ciclo de conducción con tráfico, señalización y misión.
    protected void actualizar(float deltaTime) {
        tiempoAnimacion += Math.max(0, deltaTime); // Anima la señalización con el tiempo real transcurrido.
        float anteriorX = autoX; // Guarda X para detectar el primer movimiento real.
        float anteriorZ = autoZ; // Guarda Z antes de integrar la conducción.
        avisoAtropello = Math.max(0, avisoAtropello - Math.max(0, deltaTime));
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        actualizarTrafico(deltaTime); // Avanza los conductores después de conocer la posición aceptada del jugador.
        actualizarPeatones(deltaTime);
        mision.actualizar(deltaTime, anteriorX, anteriorZ, autoX, autoZ); // Cuenta el tiempo y comprueba si el jugador alcanzó la meta.
        actualizarTitulo(); // Publica el estado después de actualizar la carrera.
    }

    /** Añade el estado real del minimapa a los indicadores de iluminación. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        return " | " + mision.estado() + " " + Contrarreloj.formato(mision.tiempo()) + // Añade el estado y cronómetro a los indicadores heredados.
            super.estadoExtra() + " | Puntos: " + puntuacion.puntos() + " | M: mapa " + (mostrarMapa ? "ON" : "OFF"); // Conserva las luces e identifica la visibilidad del mapa.
    }

    // ==================== 4. ESCENA FINAL ====================

    /** Añade la decoración de todos los barrios a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        dibujarObstaculos(); // Añade las barreras con la misma geometría que sus colisiones.
        dibujarSalidaYMeta(); // Identifica el inicio y el destino activo de la misión.
        dibujarTrafico(); // Dibuja los tres vehículos o sus indicadores según el pase actual.
        dibujarPeatones();
        chata.dibujar(this); // Dibuja la pose física de la chata en la escena y en el minimapa.
    }

    /** Coloca el conjunto completo para preparar escenas y pruebas; la conducción nunca teletransporta. */
    protected void colocarJugador(float x, float z, float giro) {
        autoX = x; // Sitúa el vehículo tractor.
        autoZ = z; // Conserva la coordenada de profundidad solicitada.
        angulo = giro; // Establece la orientación inicial de la maniobra.
        chata.reiniciar(x, z, giro); // Coloca la chata alineada detrás del tractor.
    }

    /**
     * Valida el recorrido del conjunto antes de aceptar cualquier parte de la maniobra.
     * El muestreo espacial complementa los pasos de 1/120 s, incluso con turbo o derrape.
     * Una propuesta rechazada no cambia la pose del remolque; se puede intentar retroceder.
     */
    @Override
    protected boolean intentarMoverJugador(float x, float z, float giro) {
        float cambioGiro = (float) Math.atan2(Math.sin(giro - angulo), Math.cos(giro - angulo)); // Usa el giro más corto al cruzar pi.
        int muestras = Math.max(1, (int) Math.ceil(Math.max(Math.hypot(x - autoX, z - autoZ) / 0.08,
            Math.abs(cambioGiro) / 0.02))); // Limita cada avance a 8 cm y cada giro a aproximadamente un grado.
        ChataRemolque.Posicion propuesta = chata.posicion(); // Trabaja sobre una copia inmutable de la pose aceptada.
        for (int i = 1; i <= muestras; i++) { // Revisa también el espacio barrido entre la posición inicial y final.
            float fraccion = (float) i / muestras; // Fracción de la maniobra que corresponde a esta muestra.
            float px = autoX + (x - autoX) * fraccion, pz = autoZ + (z - autoZ) * fraccion; // Interpola el avance del auto.
            float pg = angulo + cambioGiro * fraccion; // Interpola el giro del enganche.
            propuesta = chata.proponer(propuesta, px, pz, pg); // Calcula el seguimiento del eje sin modificar el estado real.
            if (!libreDeVehiculos(px, pz, pg) || !chataLibreDeObstaculos(propuesta, px, pz, pg)) return false; // Rechaza penetraciones del conjunto.
            if (!puedeMoverJugador(px, pz, pg)) return false; // Mantiene la penalización por contacto directo del auto.
            boolean libre = true; // Comprueba todos los peatones que podrían tocar el remolque.
            for (PeatonNpc peaton : peatones) {
                if (!chata.colisionPeaton(propuesta, peaton.posicion())) continue; // Ignora peatones fuera de sus dos huellas.
                libre = false; // La chata tampoco puede atravesar a una persona.
                if ((Math.abs(x - autoX) + Math.abs(z - autoZ) + Math.abs(cambioGiro)) > 0.000001f)
                    registrarContactoPeaton(peaton); // Aplica la misma penalización única que el auto tractor.
            }
            if (!libre) return false; // Conserva las poses anteriores al bloquearse el remolque.
        }
        chata.confirmar(propuesta); // Solo confirma el seguimiento cuando todas las muestras están libres.
        return true; // clase2 acepta inmediatamente la posición u orientación correspondiente del tractor.
    }

    /** Consulta pura: comprueba articulación, auto propio, límites, aceras, barreras y tráfico. */
    protected boolean chataLibreDeObstaculos(ChataRemolque.Posicion p, float x, float z, float giro) {
        if (!chata.articulacionValida(p, giro)) return false; // Impide plegar la chata más de 70 grados.
        ChataRemolque.Huella tractor = huellaAuto(x, z, giro); // Incluye ruedas y carrocería del vehículo principal.
        for (ChataRemolque.Huella huella : chata.huellas(p)) { // Valida por separado la plataforma y la lanza.
            if (!huella.dentroDelLimite(LIMITE) || huella.colision(tractor)) return false; // Evita salir del mapa o invadir el propio auto.
            for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las manzanas ocupadas.
                for (int columna = 0; columna < MAPA[fila].length; columna++) {
                    if (MAPA[fila][columna] != 0 && huella.colision(new ChataRemolque.Huella(
                        centro(columna), centro(fila), 0, CELDA / 2, CELDA / 2))) return false; // Aceras incluyen edificios y parques.
                }
            }
            for (float[] o : OBSTACULOS) { // Comparte la geometría de las barreras dibujadas.
                if (huella.colision(new ChataRemolque.Huella(o[0], o[1], 0, o[2], o[3]))) return false; // Bloquea el avance o giro contra ellas.
            }
            for (VehiculoNpc npc : trafico) { // Impide que el remolque invada cualquier auto autónomo.
                VehiculoNpc.Posicion v = npc.posicion(); // Lee la pose aceptada del conductor.
                if (huella.colision(huellaAuto(v.x(), v.z(), v.angulo()))) return false; // Usa colisión entre rectángulos orientados.
            }
        }
        return true; // Toda la chata cabe en la nueva posición.
    }

    /** Adapta la huella común del automóvil a las consultas de colisión de la chata. */
    private ChataRemolque.Huella huellaAuto(float x, float z, float giro) {
        return new ChataRemolque.Huella(x, z, giro, MITAD_ANCHO_AUTO, MITAD_LARGO_AUTO); // No agranda artificialmente la carrocería.
    }

    /** Rechaza movimientos del jugador que invaden geometría fija o la huella de otro automóvil. */
    @Override // Añade objetos móviles a la colisión de las etapas anteriores.
    protected boolean puedeCircular(float x, float z, float giro) {
        if (!libreDeVehiculos(x, z, giro)) return false;
        for (PeatonNpc peaton : peatones) {
            if (colisionPeaton(peaton.posicion(), x, z, giro)) return false;
        }
        return true;
    }

    private boolean libreDeVehiculos(float x, float z, float giro) {
        if (!puedeCircularSinTrafico(x, z, giro)) return false; // Rechaza primero bordes, aceras y barreras.
        for (VehiculoNpc npc : trafico) { // Compara la propuesta del jugador con cada carrocería orientada.
            if (colisionVehiculos(x, z, giro, npc.posicion())) return false; // Impide atravesar un NPC.
        }
        return true; // La huella propuesta está libre de objetos fijos y móviles.
    }

    /** Solo el movimiento real del jugador registra atropellos; consultar colisiones no resta puntos. */
    @Override
    protected boolean puedeMoverJugador(float x, float z, float giro) {
        if (!libreDeVehiculos(x, z, giro)) return false;
        boolean libre = true;
        for (PeatonNpc peaton : peatones) {
            if (!colisionPeaton(peaton.posicion(), x, z, giro)) continue;
            libre = false;
            boolean desplazamiento = Math.abs(x - autoX) + Math.abs(z - autoZ) + Math.abs(giro - angulo) > 0.000001f;
            if (desplazamiento) registrarContactoPeaton(peaton); // Comparte la regla de puntuación con la chata.
        }
        return libre;
    }

    /** Registra una sola penalización por persona e incidente, también si el contacto es con la chata. */
    private void registrarContactoPeaton(PeatonNpc peaton) {
        if (Math.abs(velocidad) > 0.01f && peaton.registrarImpacto()) { // Estar estacionado o repetir el contacto no vuelve a descontar.
            puntuacion.registrarAtropello(); // Descuenta los 100 puntos definidos para el atropello.
            avisoAtropello = 3; // Mantiene visible el aviso durante tres segundos.
        }
    }

    protected boolean colisionPeaton(PasoPeatonal.Punto p, float x, float z, float giro) {
        return PeatonNpc.colision(p, x, z, giro, MITAD_ANCHO_AUTO, MITAD_LARGO_AUTO);
    }

    /** Distribuye el piloto en cruces existentes de ambos ejes, lejos de salida y meta. */
    private static List<PeatonNpc> crearPeatones() {
        return PASOS.stream().filter(p ->
            (p.cruzaEnX() && p.x() == 10 && (p.z() == 3.5f || p.z() == -16.5f)) ||
            (!p.cruzaEnX() && p.z() == -10 && (p.x() == -16.5f || p.x() == 23.5f)))
            .map(p -> new PeatonNpc(p, 1)).toList();
    }

    protected void actualizarPeatones(float deltaTime) {
        for (PeatonNpc peaton : peatones) {
            peaton.actualizar(deltaTime, p -> {
                if (colisionPeaton(p, autoX, autoZ, angulo)) return false;
                if (chata.colisionPeaton(chata.posicion(), p)) return false; // El peatón espera también ante la plataforma o la lanza.
                for (VehiculoNpc npc : trafico) {
                    VehiculoNpc.Posicion v = npc.posicion();
                    if (colisionPeaton(p, v.x(), v.z(), v.angulo())) return false;
                }
                for (PeatonNpc otro : peatones) {
                    if (otro == peaton) continue;
                    PasoPeatonal.Punto q = otro.posicion();
                    if (Math.hypot(p.x() - q.x(), p.z() - q.z()) < PeatonNpc.RADIO * 2) return false;
                }
                return true;
            });
        }
    }

    /** Figuras provisionales para poder comprobar rutas y contactos antes de incorporar modelos. */
    private void dibujarPeatones() {
        for (PeatonNpc peaton : peatones) {
            PasoPeatonal.Punto p = peaton.posicion();
            float verde = peaton.impactado() ? 0.12f : 0.8f;
            if (vistaMapa) {
                caja(p.x(), 25, p.z(), 1.2f, 0.1f, 1.2f, 1, verde, 0.3f);
                continue;
            }
            float base = peaton.sobreAcera() ? 0.3f : 0;
            caja(p.x(), base + 1, p.z(), 0.48f, 0.7f, 0.35f, 1, verde, 0.3f);
            caja(p.x(), base + 1.55f, p.z(), 0.36f, 0.36f, 0.36f, 0.8f, 0.58f, 0.4f);
            for (float lado : new float[] {-0.14f, 0.14f})
                caja(p.x() + lado, base + 0.33f, p.z(), 0.17f, 0.66f, 0.22f, 0.13f, 0.2f, 0.3f);
        }
    }

    /** Consulta la geometría fija para comprobar rutas sin depender del tráfico en movimiento. */
    protected boolean puedeCircularSinTrafico(float x, float z, float giro) {
        if (!super.puedeCircular(x, z, giro)) return false; // Rechaza posiciones que invaden una acera o el borde de la ciudad.
        for (float[] obstaculo : OBSTACULOS) { // Comprueba todas las barreras visibles.
            if (colisionRectangulo(x, z, giro, obstaculo[0], obstaculo[1], obstaculo[2], obstaculo[3])) return false; // Rechaza huellas que atraviesan la barrera.
        }
        return true; // La propuesta no invade ningún objeto fijo.
    }

    /** Transforma una huella al espacio del otro vehículo y aplica los cuatro ejes de separación. */
    protected boolean colisionVehiculos(float x, float z, float giro, VehiculoNpc.Posicion otro) {
        float c = (float) Math.cos(otro.angulo()); // Calcula la rotación inversa del segundo vehículo.
        float s = (float) Math.sin(otro.angulo()); // Conserva el signo para transformar ambos ejes.
        float dx = x - otro.x(); // Desplazamiento horizontal entre los centros.
        float dz = z - otro.z(); // Desplazamiento en profundidad entre los centros.
        float localX = c * dx - s * dz; // Expresa X en el sistema local del segundo automóvil.
        float localZ = s * dx + c * dz; // Expresa Z en ese mismo sistema local.
        return colisionRectangulo(localX, localZ, giro - otro.angulo(), 0, 0,
            MITAD_ANCHO_AUTO, MITAD_LARGO_AUTO); // Reutiliza la huella real del modelo común.
    }

    /** Mantiene las rutas, detiene un conductor ante el jugador y lo reanuda cuando la vía queda libre. */
    protected void actualizarTrafico(float deltaTime) {
        for (VehiculoNpc npc : trafico) { // Cada automóvil conserva su velocidad y su progreso propios.
            npc.actualizar(deltaTime, siguiente -> { // El conductor consulta cada avance y su margen frontal.
                if (!puedeCircularSinTrafico(siguiente.x(), siguiente.z(), siguiente.angulo())) return false; // Evita aceras y barreras.
                if (colisionVehiculos(siguiente.x(), siguiente.z(), siguiente.angulo(), // Compara la propuesta del NPC con la pose actual del jugador.
                    new VehiculoNpc.Posicion(autoX, autoZ, angulo))) return false; // Cede el paso al jugador sin empujarlo.
                if (chata.colision(chata.posicion(), huellaAuto(siguiente.x(), siguiente.z(), siguiente.angulo()))) return false; // El NPC respeta la chata del jugador.
                for (VehiculoNpc otro : trafico) { // También comprueba los demás vehículos móviles.
                    if (otro != npc && colisionVehiculos(siguiente.x(), siguiente.z(), siguiente.angulo(), otro.posicion())) return false; // Evita solaparlos.
                }
                for (PeatonNpc peaton : peatones) {
                    if (colisionPeaton(peaton.posicion(), siguiente.x(), siguiente.z(), siguiente.angulo())) return false;
                }
                return true; // Autoriza el paso si no hay ninguna huella ocupando la ruta.
            });
        }
    }

    /** Comparte el modelo del jugador y muestra posición y orientación del tráfico sobre el minimapa. */
    private void dibujarTrafico() {
        for (VehiculoNpc npc : trafico) { // Dibuja cada conductor con su color identificable.
            VehiculoNpc.Posicion p = npc.posicion(); // Obtiene la misma pose que utiliza la colisión.
            if (vistaMapa) { // Eleva el indicador sobre los edificios del segundo pase.
                cajaGirada(p.x(), 24, p.z(), 2, 0.1f, 2.72f, npc.rojo, npc.verde, npc.azul, p.angulo()); // Marca su carrocería orientada.
                caja(p.x() - (float) Math.sin(p.angulo()) * 1.7f, 25, p.z() - (float) Math.cos(p.angulo()) * 1.7f,
                    0.6f, 0.1f, 0.6f, 1, 1, 1); // Una punta blanca identifica el frente.
            } else { // Construye carrocería, cabina, ruedas y luces en la escena principal.
                dibujarVehiculo(p.x(), p.z(), p.angulo(), npc.rojo, npc.verde, npc.azul); // Reutiliza el modelo sin alterar al jugador.
            }
        }
    }

    /** Dibuja cada barrera en la escena y su marca naranja en el minimapa. */
    private void dibujarObstaculos() {
        for (float[] o : OBSTACULOS) { // Recorre centros y semidimensiones compartidos con la colisión.
            entero("uEmision", 1); // Permite ver el color propio sin depender de la iluminación ambiental.
            caja(o[0], vistaMapa ? 24 : 0.55f, o[1], o[2] * 2, 1.1f, o[3] * 2, 1, 0.30f, 0.04f); // Dibuja la barrera naranja o su indicador elevado en el mapa.
            if (!vistaMapa) { // Reserva los detalles del asfalto para el pase principal.
                caja(o[0], 0.60f, o[1], o[2] * 2 + 0.01f, 0.24f, o[3] * 2 + 0.01f, 0.95f, 0.95f, 0.9f); // Añade la franja blanca de identificación.
            }
            entero("uEmision", 0); // Recupera la iluminación normal para los objetos siguientes.
        }
    }

    /** Salida verde y meta dorada, visibles tanto en el mundo como en el minimapa. */
    private void dibujarSalidaYMeta() {
        entero("uEmision", 1); // Permite ver el color propio sin depender de la iluminación ambiental.
        float altura = vistaMapa ? 22 : 0.04f; // Eleva las marcas sobre edificios en el mapa o sobre el asfalto en la vista 3D.
        caja(INICIO_X, altura, INICIO_Z, 5, 0.04f, 5, 0.12f, 0.85f, 0.35f); // Dibuja la salida verde en sus coordenadas reales.
        float x = Contrarreloj.META_X; // Comparte X con la detección de llegada.
        float z = Contrarreloj.META_Z; // Comparte Z con la detección de llegada.
        caja(x, altura, z, 6, 0.05f, 6, 1, 0.74f, 0.08f); // Dibuja la base dorada de la meta.
        // Cuadros oscuros identifican la llegada, además de su color.
        for (int fila = 0; fila < 3; fila++) { // Recorre las filas del patrón de llegada.
            for (int columna = 0; columna < 3; columna++) { // Recorre las columnas del mismo patrón.
                if ((fila + columna) % 2 == 0) { // Alterna cuadros oscuros sobre la base dorada.
                    caja(x - 2 + columna * 2, altura + 0.04f, z - 2 + fila * 2, 1.7f, 0.03f, 1.7f, 0.10f, 0.12f, 0.15f); // Dibuja un cuadro del patrón sin hundirlo en la base.
                }
            }
        }
        if (!vistaMapa) { // Reserva los detalles del asfalto para el pase principal.
            float y = 5.5f + (float) Math.sin(tiempoAnimacion * 2) * 0.3f; // Hace oscilar la baliza verticalmente para identificar el destino.
            cajaGirada(x, y, z, 1.2f, 1.2f, 1.2f, 1, 0.74f, 0.08f, (float) tiempoAnimacion); // Dibuja la baliza dorada giratoria sobre la meta.
        }
        entero("uEmision", 0); // Recupera la iluminación normal para los objetos siguientes.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = alturaEdificio(fila, columna); // Comparte la altura real del edificio.
                    dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo != 0) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarSemaforo(x + 4, z - 4); // Coloca el semáforo dentro de la acera.
                }
            }
        }
        for (PasoPeatonal paso : PASOS) dibujarPasoPeatonal(paso);
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Cubre ambos carriles y orienta las franjas según las aceras que conecta el cruce. */
    private void dibujarPasoPeatonal(PasoPeatonal paso) {
        for (PasoPeatonal.Franja f : paso.franjas()) {
            caja(f.x(), 0.045f, f.z(), f.ancho(), 0.04f, f.largo(), 0.90f, 0.92f, 0.88f);
        }
    }

    /** Interrumpe la línea central antes de las cebras para evitar pintura superpuesta. */
    @Override
    protected void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        boolean vertical = columna > 0 && columna + 1 < MAPA[fila].length &&
            MAPA[fila][columna - 1] != 0 && MAPA[fila][columna + 1] != 0;
        boolean horizontal = fila > 0 && fila + 1 < MAPA.length &&
            MAPA[fila - 1][columna] != 0 && MAPA[fila + 1][columna] != 0;
        if (vertical || horizontal) {
            caja(x, 0.025f, z, horizontal ? 1.6f : 0.13f, 0.03f,
                vertical ? 1.6f : 0.13f, 1, 0.84f, 0.35f);
        } else super.dibujarMarcasCalle(fila, columna, x, z);
    }

    /** Construye un semáforo decorativo que alterna rojo, verde y amarillo cada 12 segundos. */
    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.
        int fase = faseSemaforo(); // 0 = rojo, 1 = amarillo, 2 = verde; se repite sin detenerse.
        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = indice == fase; // Selecciona únicamente la bombilla correspondiente al ciclo actual.
            float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
            entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    /** Ciclo de 12 segundos: rojo 5 s, verde 5 s y amarillo 2 s. */
    protected int faseSemaforo() {
        double segundo = tiempoAnimacion % 12; // Repite el ciclo completo cada doce segundos.
        if (segundo < 5) { // Los primeros cinco segundos corresponden al rojo.
            return 0; // Selecciona la bombilla roja.
        }
        return segundo < 10 ? 2 : 1; // Mantiene verde otros cinco segundos y amarillo los últimos dos.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        dibujarMinimapa(); // Superpone la segunda vista después de completar la escena principal.
        if (ancho >= 240 && alto >= 180) { // Omite el panel si la ventana no deja espacio útil.
            if (panel == null) panel = new PanelCarrera(); // Reserva sus recursos gráficos solo cuando se necesita dibujarlo.
            String estado = switch (mision.estado()) { // Elige la instrucción correspondiente al estado actual de la misión.
                case LISTO -> "A -> B: acelera para iniciar"; // Explica cómo iniciar el intento.
                case EN_CURSO -> "Llega a la meta dorada"; // Recuerda el destino durante la carrera.
                case COMPLETADO -> "META ALCANZADA - R: reintentar"; // Identifica la llegada y permite repetir el intento.
            };
            panel.dibujar(ancho, alto, estado, // Dibuja la misión y los indicadores dentro de la ventana.
                "Tiempo " + Contrarreloj.formato(mision.tiempo()) + "   Mejor " + Contrarreloj.formato(mision.mejor()), // Muestra el tiempo actual y el récord de la sesión.
                "Puntos " + puntuacion.puntos() + " | Atropellos " + puntuacion.atropellos() +
                    (avisoAtropello > 0 ? " | ATROPELLO -" + PuntuacionConduccion.PENALIZACION_ATROPELLO : " | Cede a peatones"),
                estadoTurbo() + " | " + (derrapando ? "DERRAPANDO" : "Espacio + giro: derrape"), // Identifica el impulso y el estado del freno de mano.
                "Verde: salida | Dorado: meta | Naranja: obstaculos", // Explica las marcas fijas del minimapa.
                "Azul / violeta / blanco: autos | Amarillo: peatones", // Distingue vehículos y peatones del jugador cian.
                "Ctrl: freno | M: mapa | R: reiniciar | Norte arriba"); // Resume controles y orientación fija del minimapa.
        }
    }

    /** Libera los recursos del panel mientras el contexto de OpenGL sigue disponible. */
    @Override // Amplía la limpieza prevista en clase1.
    protected void limpiarExtras() {
        if (panel != null) { // Comprueba que realmente se hayan creado recursos del panel.
            panel.cerrar(); // Libera textura, buffers y programa antes de destruir el contexto.
            panel = null; // Evita reutilizar identificadores gráficos ya liberados.
        }
        super.limpiarExtras(); // Conserva la limpieza de las etapas anteriores.
    }

    /** Dibuja la ciudad con norte arriba en un viewport recortado y restaura el estado principal. */
    private void dibujarMinimapa() {
        if (!mostrarMapa || Math.min(ancho, alto) < 60) { // Omite el mapa si no hay espacio útil para dibujarlo.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 0.9f, 0.1f, 0.9f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
