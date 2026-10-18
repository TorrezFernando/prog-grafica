package com.graphics; // Comprueba el tráfico sin abrir una ventana.

import junit.framework.TestCase; // Reutiliza el sistema de pruebas del proyecto.
import static org.lwjgl.glfw.GLFW.*; // Permite verificar el reinicio con la tecla real.

/** Regresiones de rutas, colisiones móviles, prioridad del jugador y estabilidad temporal. */
public class TraficoTest extends TestCase {
    private static class Juego extends clase4 {
        boolean acelerar, retroceder; // Selecciona controles en las pruebas de conducción real.
        @Override protected boolean pulsada(int key) {
            return (acelerar && key == GLFW_KEY_W) || (retroceder && key == GLFW_KEY_S); // Simula las teclas sin depender de GLFW.
        }
    }

    public void testTresRutasCompletasDentroDeCallesSinBarreras() {
        Juego juego = new Juego(); // Construye las mismas rutas utilizadas en la aplicación.
        assertEquals(3, juego.trafico.length); // Exige los tres vehículos previstos.
        for (VehiculoNpc npc : juego.trafico) {
            for (double distancia = 0; distancia < npc.longitudRuta(); distancia += 0.05) {
                VehiculoNpc.Posicion p = npc.posicionEn(distancia); // Incluye cada curva y no solo los tramos rectos.
                assertTrue("Ruta bloqueada en " + p, juego.puedeCircularSinTrafico(p.x(), p.z(), p.angulo()));
            }
        }
    }

    public void testGirosContinuosYCircuitoCerrado() {
        for (VehiculoNpc npc : new Juego().trafico) {
            VehiculoNpc.Posicion inicio = npc.posicionEn(0);
            assertEquals(inicio, npc.posicionEn(npc.longitudRuta())); // La vuelta termina en la misma pose.
            for (double d = 0; d < npc.longitudRuta(); d += 0.1) {
                VehiculoNpc.Posicion a = npc.posicionEn(d);
                VehiculoNpc.Posicion b = npc.posicionEn(d + 0.1);
                assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) <= 0.101); // No teletransporta en las esquinas.
                double giro = Math.atan2(Math.sin(b.angulo() - a.angulo()), Math.cos(b.angulo() - a.angulo()));
                assertTrue(Math.abs(giro) < 0.04); // Tampoco gira bruscamente al cambiar de tramo.
            }
        }
    }

    public void testSeDetieneAnteJugadorYReanudaAlLiberarVia() {
        Juego juego = new Juego();
        VehiculoNpc npc = juego.trafico[2]; // Usa una recta del circuito central.
        juego.colocarJugador(-2, -12, -(float) Math.PI / 2); // Deja espacio inicial para la chata entre el auto y el NPC.
        for (int i = 0; i < 600; i++) juego.actualizarTrafico(1f / 60f);
        assertEquals(0f, npc.velocidad(), 0f); // El conductor espera sin empujar al jugador.
        VehiculoNpc.Posicion detenido = npc.posicion();
        assertFalse(juego.colisionVehiculos(juego.autoX, juego.autoZ, juego.angulo, detenido));
        juego.colocarJugador(clase2.INICIO_X, clase2.INICIO_Z, 0); // Libera también el espacio del remolque.
        for (int i = 0; i < 120; i++) juego.actualizarTrafico(1f / 60f);
        assertTrue(npc.velocidad() > 0);
        assertTrue(Math.hypot(npc.posicion().x() - detenido.x(), npc.posicion().z() - detenido.z()) > 2);
    }

    public void testJugadorNoPuedeAtravesarCarroceriasOrientadas() {
        Juego juego = new Juego();
        for (VehiculoNpc npc : juego.trafico) {
            VehiculoNpc.Posicion p = npc.posicion();
            assertFalse(juego.puedeCircular(p.x(), p.z(), p.angulo())); // La geometría fija está libre pero el vehículo ocupa el paso.
            assertTrue(juego.puedeCircularSinTrafico(p.x(), p.z(), p.angulo()));
            assertTrue(juego.colisionVehiculos(p.x(), p.z(), p.angulo() + 0.7f, p)); // Verifica también huellas con orientaciones distintas.
            assertFalse(juego.colisionVehiculos(p.x() + 4, p.z() + 4, p.angulo(), p));
        }
    }

    public void testVariasVueltasSinSalirNiInvadirJugador() {
        Juego juego = new Juego();
        VehiculoNpc.Posicion[] iniciales = java.util.Arrays.stream(juego.trafico).map(VehiculoNpc::posicion).toArray(VehiculoNpc.Posicion[]::new);
        for (int i = 0; i < 12000; i++) {
            juego.actualizarTrafico(1f / 60f); // Simula doscientos segundos con el jugador en la salida.
            for (VehiculoNpc npc : juego.trafico) {
                VehiculoNpc.Posicion p = npc.posicion();
                assertTrue(juego.puedeCircularSinTrafico(p.x(), p.z(), p.angulo()));
                assertFalse(juego.colisionVehiculos(juego.autoX, juego.autoZ, juego.angulo, p));
            }
        }
        for (int i = 0; i < juego.trafico.length; i++) assertFalse(iniciales[i].equals(juego.trafico[i].posicion()));
    }

    public void testFrecuenciasDistintasMantienenRecorridoSimilar() {
        VehiculoNpc lento = new VehiculoNpc(28, 6, 0, 1, 1, 1);
        VehiculoNpc rapido = new VehiculoNpc(28, 6, 0, 1, 1, 1);
        for (int i = 0; i < 600; i++) lento.actualizar(1f / 30f, p -> true);
        for (int i = 0; i < 2880; i++) rapido.actualizar(1f / 144f, p -> true);
        assertEquals(lento.posicion().x(), rapido.posicion().x(), 0.05f);
        assertEquals(lento.posicion().z(), rapido.posicion().z(), 0.05f);
    }

    public void testRRestableceDistribucionYVelocidad() {
        Juego juego = new Juego();
        VehiculoNpc.Posicion[] iniciales = java.util.Arrays.stream(juego.trafico).map(VehiculoNpc::posicion).toArray(VehiculoNpc.Posicion[]::new);
        for (int i = 0; i < 120; i++) juego.actualizarTrafico(1f / 60f);
        juego.tecla(GLFW_KEY_R); // Usa el control compartido con la carrera y el jugador.
        for (int i = 0; i < juego.trafico.length; i++) {
            assertEquals(iniciales[i], juego.trafico[i].posicion());
            assertEquals(0f, juego.trafico[i].velocidad(), 0f);
        }
    }

    public void testChoqueConTurboContraNpcPermiteSalirEnReversa() {
        Juego juego = new Juego(); // Acerca dos vehículos de frente sobre la calle del circuito central.
        juego.colocarJugador(-3, -12, (float) Math.PI / 2); // El jugador mira al oeste y el NPC al este; la chata queda detrás.
        juego.acelerar = true;
        juego.activarTurbo(); // Comprueba la colisión incluso con la aceleración máxima.
        for (int i = 0; i < 180; i++) {
            juego.actualizar(1f / 60f);
            assertFalse(juego.colisionVehiculos(juego.autoX, juego.autoZ, juego.angulo, juego.trafico[2].posicion()));
        }
        assertEquals(0f, juego.velocidad, 0.001f); // El choque frontal detiene al jugador sin atravesar al NPC.
        float detenidoX = juego.autoX;
        juego.acelerar = false;
        juego.retroceder = true; // Libera el bloqueo alejándose en reversa.
        for (int i = 0; i < 60; i++) juego.actualizar(1f / 60f);
        assertTrue(juego.autoX > detenidoX + 1);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }
}
