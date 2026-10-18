package com.graphics;

import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

public class DerrapeTurboTest extends TestCase {
    private static class Juego extends clase4 {
        final Set<Integer> teclas = new HashSet<>();
        @Override protected boolean pulsada(int key) { return teclas.contains(key); }
        void avanzar(double segundos, int fps) {
            for (int i = 0; i < Math.round(segundos * fps); i++) actualizar(1f / fps);
        }
    }
    // Aísla agarre y frecuencias de cuadro de la geometría de las calles.
    private static class PistaLibre extends Juego {
        @Override protected boolean puedeCircular(float x, float z, float giro) { return true; }
        @Override protected boolean intentarMoverJugador(float x, float z, float giro) { return true; } // Aísla el modelo del tractor; ChataRemolqueTest verifica el conjunto.
    }

    public void testTurboAceleraMasQueElMotorNormal() {
        Juego normal = new Juego();
        Juego turbo = new Juego();
        normal.teclas.add(GLFW_KEY_W);
        turbo.teclas.add(GLFW_KEY_W);
        turbo.tecla(GLFW_KEY_LEFT_SHIFT);
        normal.avanzar(0.5, 60);
        turbo.avanzar(0.5, 60);
        assertTrue(turbo.velocidad > normal.velocidad * 2);
        assertTrue(turbo.autoZ < normal.autoZ - 2);
        assertTrue(turbo.turboRestante > 0);
    }

    public void testCooldownDeDosSegundosDesdeElFinDelImpulso() {
        Juego juego = new Juego();
        juego.tecla(GLFW_KEY_RIGHT_SHIFT);
        juego.avanzar(1, 60);
        assertEquals(0.0, juego.turboRestante, 0.00001);
        assertEquals(2.0, juego.turboEspera, 0.00001);
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        assertEquals(0.0, juego.turboRestante, 0.0);
        juego.avanzar(1.9, 60);
        assertTrue(juego.turboEspera > 0);
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        assertEquals(0.0, juego.turboRestante, 0.0);
        juego.avanzar(0.1, 60);
        assertEquals(0.0, juego.turboEspera, 0.00001);
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        assertEquals(1.0, juego.turboRestante, 0.0);
    }

    public void testMantenerShiftNoReactivaNiProlongaTurbo() {
        Juego juego = new Juego();
        juego.teclas.add(GLFW_KEY_LEFT_SHIFT);
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        juego.avanzar(0.5, 60);
        juego.tecla(GLFW_KEY_RIGHT_SHIFT); // Una segunda pulsación durante el impulso tampoco lo alarga.
        assertEquals(0.5, juego.turboRestante, 0.00001);
        juego.avanzar(3.5, 60);
        assertEquals(0.0, juego.turboRestante, 0.0);
        assertEquals(0.0, juego.turboEspera, 0.0);
    }

    public void testCooldownUsaTiempoRealTrasCuadroLento() {
        Juego juego = new Juego();
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        juego.actualizar(2.5f); // Física limitada, pero el reloj debe consumir 1 s de impulso y 1.5 s de espera.
        assertEquals(0.0, juego.turboRestante, 0.0);
        assertEquals(0.5, juego.turboEspera, 0.00001);
        assertTrue(juego.autoZ > 29); // La pausa no produce un salto de 2.5 segundos de movimiento.
    }

    public void testCtrlCancelaImpulsoYRespetaCooldown() {
        Juego juego = new Juego();
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        juego.avanzar(0.2, 60);
        float velocidadAntes = juego.velocidad;
        juego.teclas.add(GLFW_KEY_LEFT_CONTROL);
        juego.avanzar(0.2, 60);
        assertEquals(0.0, juego.turboRestante, 0.0);
        assertTrue(juego.turboEspera > 1.7);
        assertTrue(juego.velocidad < velocidadAntes);
    }

    public void testTurboNoSeActivaMarchaAtras() {
        Juego juego = new Juego();
        juego.velocidad = -3;
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        assertEquals(0.0, juego.turboRestante, 0.0);
    }

    public void testDerrapeSeparaMovimientoYCarroceriaYRecuperaAgarre() {
        PistaLibre juego = new PistaLibre();
        juego.velocidad = 11;
        juego.teclas.add(GLFW_KEY_W);
        juego.teclas.add(GLFW_KEY_A);
        juego.teclas.add(GLFW_KEY_SPACE);
        juego.avanzar(0.3, 60);
        assertTrue(juego.derrapando);
        assertTrue(juego.angulo > 0.3f);
        assertTrue(juego.desfaseDerrape < -0.2f);
        float diferencia = Math.abs(juego.desfaseDerrape);
        juego.teclas.remove(GLFW_KEY_SPACE);
        juego.teclas.remove(GLFW_KEY_A);
        juego.avanzar(0.5, 60);
        assertFalse(juego.derrapando);
        assertTrue(Math.abs(juego.desfaseDerrape) < diferencia * 0.1f);
    }

    public void testTurboYDerrapeNoAtraviesanObstaculos() {
        Juego juego = new Juego();
        juego.colocarJugador(40, 50, 0); // Prepara auto y chata para la colisión frontal.
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        for (int i = 0; i < 120; i++) {
            juego.actualizar(1f / 60);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        }
        assertTrue(juego.autoZ >= 45 + clase2.MITAD_LARGO_AUTO - 0.001);
        juego.reiniciar();
        juego.colocarJugador(-33.6f, -40, 0); // Mantiene el enganche al cambiar el escenario.
        juego.velocidad = 11;
        juego.teclas.add(GLFW_KEY_W);
        juego.teclas.add(GLFW_KEY_A);
        juego.teclas.add(GLFW_KEY_SPACE);
        for (int i = 0; i < 120; i++) {
            juego.actualizar(1f / 60);
            assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        }
    }

    public void testTurboYDerrapeComparablesA30Y144Fps() {
        PistaLibre a = new PistaLibre();
        PistaLibre b = new PistaLibre();
        for (PistaLibre juego : new PistaLibre[] {a, b}) {
            juego.velocidad = 10;
            juego.teclas.add(GLFW_KEY_W);
            juego.teclas.add(GLFW_KEY_A);
            juego.teclas.add(GLFW_KEY_SPACE);
            juego.tecla(GLFW_KEY_LEFT_SHIFT);
        }
        a.avanzar(1, 30);
        b.avanzar(1, 144);
        assertEquals(a.velocidad, b.velocidad, 0.06f);
        assertEquals(a.angulo, b.angulo, 0.03f);
        assertEquals(a.autoX, b.autoX, 0.15f);
        assertEquals(a.autoZ, b.autoZ, 0.15f);
        assertEquals(a.turboEspera, b.turboEspera, 0.0001);
    }

    public void testRRestableceTurboYEliminaInercia() {
        Juego juego = new Juego();
        juego.turboEspera = 1;
        juego.desfaseDerrape = -0.6f;
        juego.derrapando = true;
        juego.tecla(GLFW_KEY_R);
        assertEquals(0f, juego.desfaseDerrape, 0f);
        assertFalse(juego.derrapando);
        assertEquals(0.0, juego.turboRestante, 0.0);
        assertEquals(0.0, juego.turboEspera, 0.0);
        juego.tecla(GLFW_KEY_LEFT_SHIFT);
        assertEquals(1.0, juego.turboRestante, 0.0);
    }
}
