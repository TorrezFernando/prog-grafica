package com.graphics; // Verifica el remolque junto al resto de la física del juego.

import java.util.HashSet; // Permite mantener varias teclas durante una maniobra.
import java.util.Set; // Describe el teclado simulado.
import junit.framework.TestCase; // Ejecuta las pruebas sin crear un contexto gráfico.
import static org.lwjgl.glfw.GLFW.*; // Reutiliza los controles reales de conducción.

/** Pruebas de seguimiento, maniobras y colisiones que afectan exclusivamente a la chata. */
public class ChataRemolqueTest extends TestCase {
    private static class Juego extends clase4 { // Ejecuta la física normal con entradas reproducibles.
        final Set<Integer> teclas = new HashSet<>(); // Guarda las teclas sostenidas durante cada prueba.
        @Override protected boolean pulsada(int key) { return teclas.contains(key); } // Sustituye únicamente el teclado GLFW.
        void avanzar(float segundos, int fps) { // Simula tiempo con la frecuencia solicitada.
            for (int i = 0; i < Math.round(segundos * fps); i++) actualizar(1f / fps); // Conserva subdivisiones y colisiones reales.
        }
    }

    /** El eje debe conservar su distancia a la rótula, aunque la chata gire respecto al auto. */
    private void comprobarEnganche(Juego juego) {
        ChataRemolque.Posicion p = juego.chata.posicion(); // Lee la pose realmente confirmada.
        float x = juego.autoX + (float) Math.sin(juego.angulo) * ChataRemolque.ENGANCHE_AUTO; // Calcula la rótula del tractor.
        float z = juego.autoZ + (float) Math.cos(juego.angulo) * ChataRemolque.ENGANCHE_AUTO; // Conserva la misma transformación del dibujo.
        assertEquals(ChataRemolque.DISTANCIA_EJE, Math.hypot(p.x() - x, p.z() - z), 0.00002); // No permite estirar o soltar el remolque.
        assertTrue(juego.chata.articulacionValida(p, juego.angulo)); // Impide plegados inválidos.
        assertTrue(juego.chataLibreDeObstaculos(p, juego.autoX, juego.autoZ, juego.angulo)); // Exige que toda la chata quede libre.
    }

    public void testEngancheEnReposoAvanceYReversa() {
        Juego juego = new Juego(); // Empieza con auto y chata alineados en la salida.
        ChataRemolque.Posicion inicio = juego.chata.posicion(); // Guarda la pose estacionada.
        juego.avanzar(1, 60); // No pulsa ninguna tecla.
        assertEquals(inicio.x(), juego.chata.posicion().x(), 0.00001f); // La chata no deriva estando detenida.
        assertEquals(inicio.z(), juego.chata.posicion().z(), 0.00001f); // Tampoco se acerca gradualmente al auto.
        juego.teclas.add(GLFW_KEY_W); // Tira del remolque en línea recta.
        juego.avanzar(1, 60);
        assertTrue(juego.chata.posicion().z() < inicio.z() - 1); // El eje sigue el desplazamiento del enganche.
        comprobarEnganche(juego);
        juego.teclas.clear(); // Prepara una marcha atrás desde reposo.
        juego.velocidad = 0;
        juego.teclas.add(GLFW_KEY_S);
        float anterior = juego.chata.posicion().z(); // Guarda la profundidad antes de retroceder.
        juego.avanzar(0.5f, 60);
        assertTrue(juego.chata.posicion().z() > anterior + 0.3f); // Empuja realmente al remolque en reversa.
        comprobarEnganche(juego);
    }

    public void testDerrapeConTurboArticulaChataSinPerderEnganche() {
        Juego juego = prepararDerrape(); // Usa una intersección amplia y despejada.
        for (int i = 0; i < 18; i++) {
            juego.actualizar(1f / 60); // Integra el movimiento lateral y el giro de la carrocería.
            comprobarEnganche(juego); // Comprueba cada cuadro, no solo la posición final.
        }
        assertTrue(juego.derrapando && juego.turboRestante > 0); // Exige que ambas mecánicas estén activas.
        assertTrue(Math.abs(juego.angulo - juego.chata.posicion().angulo()) > 0.15f); // No copia rígidamente el ángulo del auto.
    }

    private Juego prepararDerrape() {
        Juego juego = new Juego(); // Prepara la misma maniobra para distintas frecuencias.
        juego.colocarJugador(-30, -30, 0); // Alinea el conjunto dentro de una intersección.
        juego.velocidad = 10; // Permite entrar inmediatamente en derrape.
        juego.teclas.add(GLFW_KEY_W);
        juego.teclas.add(GLFW_KEY_A);
        juego.teclas.add(GLFW_KEY_SPACE);
        juego.activarTurbo(); // Incluye aceleración máxima en la comprobación del enganche.
        return juego;
    }

    public void testSeguimientoComparableA30Y144Fps() {
        Juego lento = prepararDerrape(), rapido = prepararDerrape(); // Conserva configuración e impulso idénticos.
        lento.avanzar(0.5f, 30);
        rapido.avanzar(0.5f, 144);
        assertEquals(lento.autoX, rapido.autoX, 0.15f); // El tractor sigue la misma trayectoria aproximada.
        assertEquals(lento.autoZ, rapido.autoZ, 0.15f);
        assertEquals(lento.chata.posicion().x(), rapido.chata.posicion().x(), 0.15f); // El eje no depende fuertemente de los FPS.
        assertEquals(lento.chata.posicion().z(), rapido.chata.posicion().z(), 0.15f);
        assertEquals(lento.chata.posicion().angulo(), rapido.chata.posicion().angulo(), 0.04f); // Comprueba también su articulación.
        comprobarEnganche(lento);
        comprobarEnganche(rapido);
    }

    public void testBarreraDetieneSoloLaChataYSePuedeSalirHaciaDelante() {
        Juego juego = new Juego();
        juego.colocarJugador(-31.6f, 3, 0); // La chata queda antes de la barrera situada en Z=10.
        juego.teclas.add(GLFW_KEY_S); // Retrocede hacia la barrera con la chata por delante.
        juego.avanzar(2, 60);
        assertTrue(juego.autoZ > 3.1f && juego.autoZ < 3.71f); // Avanza hasta el contacto, sin cruzarlo.
        assertTrue(juego.puedeCircularSinTrafico(juego.autoX, juego.autoZ, 0)); // El auto por sí solo todavía tiene espacio.
        assertEquals(0f, juego.velocidad, 0.0001f); // La colisión del remolque detiene también al tractor.
        comprobarEnganche(juego);
        float detenido = juego.autoZ; // Guarda el punto de bloqueo.
        juego.teclas.clear();
        juego.teclas.add(GLFW_KEY_W); // Se aleja del obstáculo sin reiniciar ni desenganchar.
        juego.avanzar(1, 60);
        assertTrue(juego.autoZ < detenido - 1); // El contacto no deja el conjunto atrapado.
        comprobarEnganche(juego);
    }

    public void testAceraDetieneLaChataAntesDeQueElAutoLaToque() {
        Juego juego = new Juego();
        juego.colocarJugador(-30, -40, (float) Math.PI / 2); // Auto hacia el oeste; la chata apunta hacia la manzana del este.
        juego.teclas.add(GLFW_KEY_S);
        juego.avanzar(1, 60);
        assertTrue(juego.autoX > -30 && juego.autoX <= -29.79f); // Solo dispone de 20 cm antes de que la cola toque la acera.
        assertTrue(juego.puedeCircularSinTrafico(juego.autoX, juego.autoZ, juego.angulo)); // La carrocería aún está lejos de la acera.
        assertEquals(0f, juego.velocidad, 0.0001f);
        comprobarEnganche(juego);
    }

    public void testChataRespetaElBordeDelMapaInclusoTrasUnCuadroLento() {
        Juego juego = new Juego();
        juego.colocarJugador(50, 49, 0); // Deja espacio entre la cola y el borde sur.
        juego.teclas.add(GLFW_KEY_S);
        juego.actualizar(3); // Una pausa larga no debe teletransportar el conjunto.
        assertTrue(juego.autoZ < 49.1f);
        juego.avanzar(2, 30); // Llega al límite usando el motor real.
        assertTrue(juego.autoZ <= 50.21f && juego.autoZ > 50); // La cola alcanza Z=55 antes que el tractor.
        assertEquals(0f, juego.velocidad, 0.0001f);
        comprobarEnganche(juego);
    }

    public void testMovimientoRechazadoNoArrastraNiTeletransportaLaChata() {
        Juego juego = new Juego();
        juego.colocarJugador(-28, 6, 0); // Auto libre al sur de la barrera; remolque junto a su costado.
        ChataRemolque.Posicion anterior = juego.chata.posicion();
        assertTrue(juego.puedeCircularSinTrafico(-31, 6, 0)); // El tractor podría moverse lateralmente sin la chata.
        assertFalse(juego.intentarMoverJugador(-31, 6, 0)); // La plataforma barre la barrera durante el desplazamiento.
        assertEquals(anterior, juego.chata.posicion()); // La propuesta fallida no deja modificaciones parciales.
        assertEquals(-28f, juego.autoX, 0f); // Tampoco cambia la posición del tractor.
    }

    public void testLanzaYPlataformaTienenHuellasOrientadasSeparadas() {
        ChataRemolque chata = new ChataRemolque(0, 0, 0); // Aísla la geometría del mapa.
        ChataRemolque.Posicion p = chata.posicion();
        assertTrue(chata.colision(p, new ChataRemolque.Huella(0, 1.8f, 0, 0.05f, 0.05f))); // La lanza tiene colisión propia.
        assertFalse(chata.colision(p, new ChataRemolque.Huella(0.6f, 1.8f, 0, 0.05f, 0.05f))); // No bloquea el espacio vacío al lado del enganche.
        chata.reiniciar(0, 0, (float) Math.PI / 2); // Gira el conjunto noventa grados.
        assertTrue(chata.colision(chata.posicion(), new ChataRemolque.Huella(3.6f, 0, 0.7f, 0.2f, 0.2f))); // Rota también las huellas.
        assertFalse(chata.articulacionValida(new ChataRemolque.Posicion(0, 0, (float) Math.PI / 2), 0)); // Rechaza plegado de noventa grados.
    }

    public void testPeatonEsperaAnteChataEstacionadaSinPenalizar() {
        Juego juego = new Juego();
        juego.colocarJugador(10, -20.1f, 0); // La plataforma ocupa la cebra y el auto está fuera de ella.
        PeatonNpc peaton = juego.peatones.get(0);
        for (int i = 0; i < 240; i++) juego.actualizarPeatones(0.05f); // Permite llegar hasta el remolque.
        PasoPeatonal.Punto detenido = peaton.posicion();
        assertTrue(detenido.x() > 5 && detenido.x() < 9); // Camina hasta el lateral y espera.
        assertFalse(juego.colisionPeaton(detenido, juego.autoX, juego.autoZ, juego.angulo)); // El bloqueo lo causa únicamente la chata.
        assertFalse(juego.chata.colisionPeaton(juego.chata.posicion(), detenido)); // No entra en la plataforma.
        assertEquals(0, juego.puntuacion.puntos()); // Un vehículo estacionado no registra atropellos.
        juego.colocarJugador(clase2.INICIO_X, clase2.INICIO_Z, 0); // Libera el cruce completo.
        for (int i = 0; i < 40; i++) juego.actualizarPeatones(0.05f);
        assertTrue(peaton.posicion().x() > detenido.x() + 1); // El peatón reanuda su marcha.
    }

    public void testAtropelloConLaChataRestaUnaVezYReiniciaConR() {
        Juego juego = new Juego();
        PeatonNpc peaton = juego.peatones.get(0);
        for (int i = 0; i < 100; i++) peaton.actualizar(0.05f, p -> true); // Sitúa al peatón en medio de su cebra.
        PasoPeatonal.Punto p = peaton.posicion();
        juego.colocarJugador(p.x(), p.z() - 7, 0); // La cola queda a más de dos unidades del peatón.
        juego.teclas.add(GLFW_KEY_S);
        juego.avanzar(1.5f, 60); // El retroceso golpea con la chata y bloquea el conjunto.
        assertTrue(peaton.impactado());
        assertEquals(-100, juego.puntuacion.puntos());
        assertFalse(juego.colisionPeaton(peaton.posicion(), juego.autoX, juego.autoZ, juego.angulo)); // El auto nunca toca a la persona.
        juego.avanzar(0.3f, 60); // Mantiene el mismo contacto durante varios cuadros.
        assertEquals(1, juego.puntuacion.atropellos());
        juego.tecla(GLFW_KEY_R); // Reinicia mediante el control del juego.
        assertEquals(0, juego.puntuacion.puntos());
        assertEquals(0f, juego.chata.posicion().angulo(), 0f);
        comprobarEnganche(juego);
    }

    public void testNpcSeDetieneAnteLaChataSinTocarElAuto() {
        Juego juego = new Juego();
        juego.colocarJugador(-2, -12, -(float) Math.PI / 2); // La chata queda entre el NPC blanco y el jugador.
        VehiculoNpc npc = juego.trafico[2];
        for (int i = 0; i < 600; i++) juego.actualizarTrafico(1f / 60); // Deja tiempo para aproximarse y esperar.
        VehiculoNpc.Posicion detenido = npc.posicion();
        assertEquals(0f, npc.velocidad(), 0f);
        assertTrue(Math.hypot(detenido.x() - juego.autoX, detenido.z() - juego.autoZ) > 5); // El jugador queda lejos del punto de bloqueo.
        assertFalse(juego.chata.colision(juego.chata.posicion(), new ChataRemolque.Huella(
            detenido.x(), detenido.z(), detenido.angulo(), 1, 1.36f))); // El NPC no invade el remolque.
        juego.colocarJugador(clase2.INICIO_X, clase2.INICIO_Z, 0); // Retira auto y chata.
        for (int i = 0; i < 120; i++) juego.actualizarTrafico(1f / 60);
        assertTrue(npc.velocidad() > 0); // Reanuda la marcha cuando se libera la vía.
    }

    public void testGiroRealDeNoventaGradosConLaChata() {
        Juego juego = new Juego();
        juego.colocarJugador(-30, -42, 0); // Inicia el giro antes de la avenida norte para dar espacio al remolque.
        juego.velocidad = 4;
        juego.teclas.add(GLFW_KEY_W);
        juego.teclas.add(GLFW_KEY_D); // Gira hacia el este mediante la dirección normal.
        int cuadros = 0;
        while (juego.angulo > -Math.PI / 2 && cuadros++ < 360) {
            juego.actualizar(1f / 60);
            comprobarEnganche(juego); // Incluye la trayectoria interior que sigue la chata en la curva.
        }
        assertTrue("Debe completar el giro sin recolocar el remolque", juego.angulo <= -Math.PI / 2);
        juego.teclas.remove(GLFW_KEY_D); // Continúa recto después del giro.
        float x = juego.autoX;
        juego.avanzar(0.5f, 60);
        assertTrue(juego.autoX > x + 1);
        comprobarEnganche(juego);
    }
}
