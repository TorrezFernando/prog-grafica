package com.graphics;

import java.util.HashSet;
import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Regresiones de geometría, contactos reales, consultas puras y reinicio del intento. */
public class PeatonesTest extends TestCase {
    private static class Juego extends clase4 {
        int teclaMotor;
        boolean congelarPeatones;
        @Override protected boolean pulsada(int key) { return key == teclaMotor; }
        @Override protected void actualizarPeatones(float dt) {
            if (!congelarPeatones) super.actualizarPeatones(dt);
        }
    }

    private int celda(float coordenada) {
        return (int) Math.floor((coordenada + clase1.LIMITE) / clase1.CELDA);
    }

    public void testCebrasConectanAcerasSinDuplicadosNiPinturaFueraDeLaCalle() {
        assertEquals(80, clase4.PASOS.size());
        assertEquals(clase4.PASOS.size(), new HashSet<>(clase4.PASOS).size());
        int horizontales = 0;
        for (PasoPeatonal paso : clase4.PASOS) {
            if (paso.cruzaEnX()) horizontales++;
            for (float lado : new float[] {-1, 1}) {
                PasoPeatonal.Punto extremo = paso.punto(lado * paso.extremo());
                assertTrue(clase1.MAPA[celda(extremo.z())][celda(extremo.x())] != 0);
            }
            float minimo = Float.POSITIVE_INFINITY, maximo = Float.NEGATIVE_INFINITY;
            for (PasoPeatonal.Franja f : paso.franjas()) {
                for (float dx : new float[] {-f.ancho() / 2, f.ancho() / 2}) {
                    for (float dz : new float[] {-f.largo() / 2, f.largo() / 2})
                        assertEquals(0, clase1.MAPA[celda(f.z() + dz)][celda(f.x() + dx)]);
                }
                float centro = paso.cruzaEnX() ? f.x() - paso.x() : f.z() - paso.z();
                float mitad = (paso.cruzaEnX() ? f.ancho() : f.largo()) / 2;
                minimo = Math.min(minimo, centro - mitad);
                maximo = Math.max(maximo, centro + mitad);
            }
            assertTrue(minimo < -4.7f && maximo > 4.7f); // Abarca los dos carriles hasta ambas aceras.
        }
        assertEquals(40, horizontales);
    }

    public void testCirculoContraAutoGiradoYEsquinas() {
        assertTrue(PeatonNpc.colision(new PasoPeatonal.Punto(0, -1.5f), 0, 0, 0, 1, 1.36f));
        assertFalse(PeatonNpc.colision(new PasoPeatonal.Punto(1.3f, 1.65f), 0, 0, 0, 1, 1.36f));
        assertTrue(PeatonNpc.colision(new PasoPeatonal.Punto(-1.5f, 0), 0, 0, (float) Math.PI / 2, 1, 1.36f));
        assertFalse(PeatonNpc.colision(new PasoPeatonal.Punto(0, 1.5f), 0, 0, (float) Math.PI / 2, 1, 1.36f));
    }

    public void testPilotoCaminaPorLosCrucesYEsperaAnteUnAutoEstacionado() {
        Juego juego = new Juego();
        assertEquals(4, juego.peatones.size());
        PeatonNpc peaton = juego.peatones.get(0);
        PasoPeatonal.Punto inicio = peaton.posicion();
        juego.colocarJugador(10, -16.5f, 0); // Coloca también la chata en el escenario peatonal.
        for (int i = 0; i < 240; i++) juego.actualizarPeatones(0.05f);
        assertTrue(Math.hypot(peaton.posicion().x() - inicio.x(), peaton.posicion().z() - inicio.z()) > 1);
        assertFalse(juego.colisionPeaton(peaton.posicion(), juego.autoX, juego.autoZ, juego.angulo));
        assertEquals(0, juego.puntuacion.puntos());
        PasoPeatonal.Punto espera = peaton.posicion();
        juego.colocarJugador(clase2.INICIO_X, clase2.INICIO_Z, 0); // Retira todo el conjunto para liberar el cruce.
        for (int i = 0; i < 40; i++) juego.actualizarPeatones(0.05f);
        assertTrue(Math.hypot(peaton.posicion().x() - espera.x(), peaton.posicion().z() - espera.z()) > 1);
    }

    private Juego prepararContacto(boolean reversa) {
        Juego juego = new Juego();
        PeatonNpc peaton = juego.peatones.get(0);
        for (int i = 0; i < 100; i++) peaton.actualizar(0.05f, p -> true);
        PasoPeatonal.Punto p = peaton.posicion();
        juego.colocarJugador(p.x(), p.z() + (reversa ? -7 : 3), 0); // En reversa deja espacio para la chata antes del peatón.
        juego.velocidad = reversa ? -6 : 16;
        juego.teclaMotor = reversa ? GLFW_KEY_S : GLFW_KEY_W;
        juego.congelarPeatones = true;
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ, 0));
        return juego;
    }

    public void testConsultarColisionesNoRestaPuntos() {
        Juego juego = prepararContacto(false);
        PasoPeatonal.Punto p = juego.peatones.get(0).posicion();
        for (int i = 0; i < 20; i++) assertFalse(juego.puedeCircular(p.x(), p.z(), 0));
        assertEquals(0, juego.puntuacion.puntos());
        assertFalse(juego.peatones.get(0).impactado());
    }

    public void testAtropelloConTurboPenalizaUnaVezYPermiteRetroceder() {
        Juego juego = prepararContacto(false);
        juego.activarTurbo();
        for (int i = 0; i < 120; i++) juego.actualizar(1f / 60);
        assertEquals(-100, juego.puntuacion.puntos());
        assertEquals(1, juego.puntuacion.atropellos());
        assertTrue(juego.peatones.get(0).impactado());
        PasoPeatonal.Punto p = juego.peatones.get(0).posicion();
        assertFalse(juego.colisionPeaton(p, juego.autoX, juego.autoZ, juego.angulo));
        float z = juego.autoZ;
        juego.teclaMotor = GLFW_KEY_S;
        for (int i = 0; i < 30; i++) juego.actualizar(1f / 60);
        assertTrue(juego.autoZ > z + 0.2f);
        assertEquals(-100, juego.puntuacion.puntos());
    }

    public void testAtropelloEnReversaYConDistintosFps() {
        for (int fps : new int[] {30, 144}) {
            Juego juego = prepararContacto(true);
            for (int i = 0; i < fps; i++) juego.actualizar(1f / fps);
            assertEquals(-100, juego.puntuacion.puntos());
            assertFalse(juego.colisionPeaton(juego.peatones.get(0).posicion(), juego.autoX, juego.autoZ, juego.angulo));
        }
    }

    public void testRecuperacionEnAceraYSegundoIncidente() {
        PeatonNpc p = new PeatonNpc(new PasoPeatonal(10, -16.5f, true, 10), 0);
        assertTrue(p.registrarImpacto());
        assertFalse(p.registrarImpacto());
        for (int i = 0; i < 100; i++) p.actualizar(0.05f, q -> false);
        assertTrue(p.impactado()); // No reaparece si la acera está ocupada.
        p.actualizar(1f / 120, q -> true);
        assertFalse(p.impactado());
        assertTrue(p.sobreAcera());
        assertTrue(p.registrarImpacto());
        PuntuacionConduccion puntos = new PuntuacionConduccion();
        puntos.registrarAtropello();
        puntos.registrarAtropello();
        assertEquals(-200, puntos.puntos());
    }

    public void testReinicioRestauraPeatonesYPuntuacion() {
        Juego juego = prepararContacto(false);
        for (int i = 0; i < 60; i++) juego.actualizar(1f / 60);
        assertEquals(-100, juego.puntuacion.puntos());
        juego.reiniciar();
        assertEquals(0, juego.puntuacion.puntos());
        assertEquals(0, juego.puntuacion.atropellos());
        for (PeatonNpc p : juego.peatones) {
            assertFalse(p.impactado());
            assertTrue(p.sobreAcera());
        }
    }

    public void testTraficoCedeAntePeatonYReanudaSinPenalizarJugador() {
        Juego juego = new Juego();
        PeatonNpc peaton = juego.peatones.get(3);
        for (int i = 0; i < 129; i++) peaton.actualizar(0.05f, p -> true);
        VehiculoNpc npc = juego.trafico[2];
        for (int i = 0; i < 1500; i++) juego.actualizarTrafico(1f / 60);
        assertEquals(0f, npc.velocidad(), 0f);
        VehiculoNpc.Posicion detenido = npc.posicion();
        assertFalse(juego.colisionPeaton(peaton.posicion(), detenido.x(), detenido.z(), detenido.angulo()));
        assertEquals(0, juego.puntuacion.puntos());
        peaton.reiniciar();
        for (int i = 0; i < 120; i++) juego.actualizarTrafico(1f / 60);
        assertTrue(npc.velocidad() > 0);
        assertTrue(Math.hypot(npc.posicion().x() - detenido.x(), npc.posicion().z() - detenido.z()) > 2);
    }
}
