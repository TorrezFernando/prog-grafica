package com.graphics;

import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

public class MisionYColisionesTest extends TestCase {
    private static class Juego extends clase4 {
        boolean acelerar;
        boolean retroceder;
        boolean girar;
        @Override protected boolean pulsada(int key) {
            return (acelerar && key == GLFW_KEY_W) || (retroceder && key == GLFW_KEY_S) || (girar && key == GLFW_KEY_A);
        }
        void simular(int cuadros) {
            for (int i = 0; i < cuadros; i++) actualizar(1f / 60f);
        }
    }

    public void testPasoCercaDeLaEsquinaSinColisionInvisible() {
        Juego juego = new Juego();
        assertTrue(juego.puedeCircular(-33.85f, -40, 0)); // La carrocería cabe; el antiguo círculo lo rechazaba.
        assertTrue(juego.puedeCircular(-40, -46.45f, 0));
        assertFalse(juego.puedeCircular(-34.1f, -40, 0)); // Una rueda sí invade la acera.
        assertFalse(juego.puedeCircular(-33.85f, -40, (float) Math.PI / 4)); // Un giro excesivo sí choca.
        juego.colocarJugador(-33.85f, -40, 0); // Prepara también el remolque, sin dejarlo en la salida.
        juego.acelerar = true;
        juego.simular(90);
        assertTrue(juego.autoZ < -45);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testRocePermiteDeslizarSinEntrarEnLaManzana() {
        Juego juego = new Juego();
        juego.colocarJugador(-33.6f, -40, 0.2f); // Apunta ligeramente hacia la pared oeste.
        juego.acelerar = true;
        juego.simular(60);
        assertTrue(juego.autoZ < -42);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testPuedeRetrocederTrasChoqueFrontal() {
        Juego juego = new Juego();
        juego.colocarJugador(40, 50, 0); // Coloca el conjunto dentro del borde sur.
        juego.acelerar = true;
        juego.simular(180);
        float detenidoZ = juego.autoZ;
        juego.acelerar = false;
        juego.retroceder = true;
        juego.simular(60);
        assertTrue(juego.autoZ > detenidoZ + 1);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    public void testObstaculosCoincidenConElDibujoYHayPasoLateral() {
        Juego juego = new Juego();
        for (float[] o : clase4.OBSTACULOS) {
            assertFalse(juego.puedeCircular(o[0], o[1], 0));
        }
        assertTrue(juego.puedeCircularSinTrafico(-28, 10, 0)); // Comprueba el paso lateral de la barrera aunque un NPC esté cruzando.
        assertTrue(juego.puedeCircularSinTrafico(8, -28, (float) Math.PI / 2));
        assertTrue(juego.puedeCircularSinTrafico(52, -10, 0));
    }

    public void testRutaCompletaHastaLaMetaEsTransitable() {
        Juego juego = new Juego();
        // Salir hacia el carril derecho y subir hasta la avenida norte; luego ir al este.
        for (float x = -30; x <= -28; x += 0.1f) assertTrue(juego.puedeCircularSinTrafico(x, 30, 0)); // Comprueba la vía fija; los NPC pueden ocuparla temporalmente.
        for (float z = 30; z >= -50; z -= 0.1f) assertTrue(juego.puedeCircularSinTrafico(-28, z, 0));
        for (float giro = 0; giro >= -Math.PI / 2; giro -= 0.02f) assertTrue(juego.puedeCircularSinTrafico(-28, -50, giro));
        for (float x = -28; x <= 50; x += 0.1f) assertTrue(juego.puedeCircularSinTrafico(x, -50, -(float) Math.PI / 2));
    }

    public void testEsperaInicioLlegadaYTiempoCongelado() {
        Contrarreloj mision = new Contrarreloj();
        mision.actualizar(100, -30, 30, -30, 30);
        assertEquals(Contrarreloj.Estado.LISTO, mision.estado());
        assertEquals(0.0, mision.tiempo(), 0.0);
        mision.actualizar(1, -30, 30, -30, 29);
        assertEquals(Contrarreloj.Estado.EN_CURSO, mision.estado());
        mision.actualizar(4, -30, 29, -30, 29); // Esperar o chocar cuenta una vez iniciada la carrera.
        mision.actualizar(20, 46, -50, 49, -50);
        assertEquals(Contrarreloj.Estado.COMPLETADO, mision.estado());
        assertEquals(25.0, mision.tiempo(), 0.0);
        mision.actualizar(99, 49, -50, 0, 0);
        assertEquals(25.0, mision.tiempo(), 0.0);
    }

    public void testReinicioConservaMejorMarca() {
        Contrarreloj mision = new Contrarreloj();
        for (int duracion : new int[] {25, 30, 20}) {
            mision.reiniciar();
            assertEquals(Contrarreloj.Estado.LISTO, mision.estado());
            assertEquals(0.0, mision.tiempo(), 0.0);
            mision.actualizar(duracion, 46, -50, 49, -50);
        }
        assertEquals(20.0, mision.mejor(), 0.0);
        mision.reiniciar();
        assertEquals(20.0, mision.mejor(), 0.0);
    }

    public void testRReiniciaAutoYCarrera() {
        Juego juego = new Juego();
        juego.acelerar = true;
        juego.simular(60);
        assertEquals(Contrarreloj.Estado.EN_CURSO, juego.mision.estado());
        juego.tecla(GLFW_KEY_R);
        assertEquals(Contrarreloj.Estado.LISTO, juego.mision.estado());
        assertEquals(0.0, juego.mision.tiempo(), 0.0);
        assertEquals(clase2.INICIO_X, juego.autoX, 0f);
        assertEquals(0f, juego.velocidad, 0f);
    }

    public void testCompletaLaMisionConConduccionReal() {
        Juego juego = new Juego();
        juego.acelerar = true;
        juego.colocarJugador(juego.autoX, juego.autoZ, -(float) Math.PI / 2); // La prueba prepara cada tramo recto con el conjunto alineado.
        int cuadros = 0;
        while (juego.autoX < -28 && cuadros++ < 600) juego.simular(1);
        assertTrue(juego.autoX >= -28);
        juego.velocidad = 0;
        juego.colocarJugador(juego.autoX, juego.autoZ, 0); // El giro real del remolque tiene pruebas propias.
        cuadros = 0;
        while (juego.autoZ > -50 && cuadros++ < 1200) juego.simular(1);
        assertTrue(juego.autoZ <= -50);
        juego.velocidad = 0;
        juego.colocarJugador(juego.autoX, juego.autoZ, -(float) Math.PI / 2); // Alinea también la chata al preparar la avenida norte.
        cuadros = 0;
        while (juego.mision.estado() != Contrarreloj.Estado.COMPLETADO && cuadros++ < 1200) juego.simular(1);
        assertEquals(Contrarreloj.Estado.COMPLETADO, juego.mision.estado());
        assertTrue(juego.mision.tiempo() > 10);
        assertTrue(Double.isFinite(juego.mision.mejor()));
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }
}
