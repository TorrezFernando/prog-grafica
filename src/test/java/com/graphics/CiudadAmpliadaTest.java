package com.graphics;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import static org.lwjgl.glfw.GLFW.*;

/** Pruebas de comportamiento sin ventana: geometría, recorridos, controles y tiempo. */
public class CiudadAmpliadaTest extends TestCase {
    private static class Juego extends clase4 {
        final Set<Integer> teclas = new HashSet<>();

        @Override
        protected boolean pulsada(int tecla) {
            return teclas.contains(tecla);
        }

        void avanzar(float segundos, int fps) {
            for (int i = 0; i < Math.round(segundos * fps); i++) {
                actualizar(1f / fps);
            }
        }
    }

    public void testDimensionesYDistribucion() {
        assertEquals(11, clase1.MAPA.length);
        assertEquals(10f, clase1.CELDA, 0f);
        assertEquals(110f, clase1.TAMANO_CIUDAD, 0f);
        int edificios = 0;
        int parques = 0;
        for (int[] fila : clase1.MAPA) {
            assertEquals(11, fila.length);
            for (int celda : fila) {
                assertTrue(celda >= 0 && celda <= 2);
                edificios += celda == 1 ? 1 : 0;
                parques += celda == 2 ? 1 : 0;
            }
        }
        assertEquals(21, edificios);
        assertEquals(4, parques);
    }

    public void testNucleoOriginalConservado() {
        int[][] original = {
            {0,0,0,0,0,0,0}, {0,1,0,1,0,2,0}, {0,0,0,0,0,0,0},
            {0,2,0,1,0,1,0}, {0,0,0,0,0,0,0}, {0,1,0,2,0,1,0}, {0,0,0,0,0,0,0}
        };
        for (int fila = 0; fila < 7; fila++) {
            assertEquals(-30 + fila * 10f, clase1.centro(fila + 2), 0f);
            for (int columna = 0; columna < 7; columna++) {
                assertEquals(original[fila][columna], clase1.MAPA[fila + 2][columna + 2]);
            }
        }
    }

    public void testTodasLasCallesConectadasYTransitables() {
        clase2 juego = new clase2(); // Red vial base; los obstáculos de misión se prueban aparte.
        int n = clase1.MAPA.length;
        boolean[][] visitado = new boolean[n][n];
        ArrayDeque<int[]> pendientes = new ArrayDeque<>();
        int inicioFila = (int) ((juego.autoZ + clase1.LIMITE) / clase1.CELDA);
        int inicioColumna = (int) ((juego.autoX + clase1.LIMITE) / clase1.CELDA);
        pendientes.add(new int[] {inicioFila, inicioColumna});
        visitado[inicioFila][inicioColumna] = true;
        int[][] direcciones = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        while (!pendientes.isEmpty()) {
            int[] celda = pendientes.remove();
            for (int[] direccion : direcciones) {
                int fila = celda[0] + direccion[0];
                int columna = celda[1] + direccion[1];
                if (fila < 0 || columna < 0 || fila >= n || columna >= n ||
                    clase1.MAPA[fila][columna] != 0 || visitado[fila][columna]) {
                    continue;
                }
                // Comprueba el ancho útil a lo largo de cada conexión, no solo sus centros.
                for (int paso = 0; paso <= 40; paso++) {
                    float x = clase1.centro(celda[1]) + direccion[1] * paso * 0.25f;
                    float z = clase1.centro(celda[0]) + direccion[0] * paso * 0.25f;
                    assertTrue("Conexión bloqueada en " + x + ", " + z, juego.puedeCircular(x, z));
                }
                visitado[fila][columna] = true;
                pendientes.add(new int[] {fila, columna});
            }
        }
        for (int fila = 0; fila < n; fila++) {
            for (int columna = 0; columna < n; columna++) {
                if (clase1.MAPA[fila][columna] == 0) {
                    assertTrue("Calle aislada", visitado[fila][columna]);
                } else {
                    assertFalse(juego.puedeCircular(clase1.centro(columna), clase1.centro(fila)));
                }
            }
        }
    }

    public void testAcerasOriginalesYNuevas() {
        Juego juego = new Juego();
        assertFalse(juego.puedeCircular(-25.5f, -20));
        assertTrue(juego.puedeCircular(-27, -20));
        assertFalse(juego.puedeCircular(45.5f, 40));
        assertTrue(juego.puedeCircular(47, 40));
        assertFalse(juego.puedeCircular(-40, -40)); // Parque añadido.
        assertFalse(juego.puedeCircular(40, 40)); // Edificio añadido.
    }

    public void testCuatroBordesAmpliados() {
        Juego juego = new Juego();
        float permitido = clase1.LIMITE - clase2.MITAD_ANCHO_AUTO;
        float permitidoZ = clase1.LIMITE - clase2.MITAD_LARGO_AUTO;
        for (int signo : new int[] {-1, 1}) {
            assertTrue(juego.puedeCircular(signo * (permitido - 0.01f), 0));
            assertFalse(juego.puedeCircular(signo * (permitido + 0.01f), 0));
            assertTrue(juego.puedeCircular(0, signo * (permitidoZ - 0.01f)));
            assertFalse(juego.puedeCircular(0, signo * (permitidoZ + 0.01f)));
            assertTrue(juego.puedeCircular(signo * 50, signo * 50));
            assertFalse(juego.puedeCircular(signo * 56, 0));
        }
    }

    public void testConducirHastaLaAmpliacion() {
        Juego juego = new Juego();
        juego.colocarJugador(-28, juego.autoZ, 0); // Sitúa todo el conjunto en el carril libre.
        juego.teclas.add(GLFW_KEY_W);
        juego.avanzar(20, 60); // Permite seguir al NPC hasta que gire y deje libre el acceso a la ampliación.
        assertTrue("Debe atravesar el antiguo borde norte", juego.autoZ < -35);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
        assertTrue(juego.autoZ >= -clase1.LIMITE + clase2.MITAD_LARGO_AUTO - 0.001f);
    }

    public void testChoqueConEdificioNuevoYBorde() {
        Juego juego = new Juego();
        juego.colocarJugador(40, 50, 0); // Incluye la chata al preparar el choque contra el edificio.
        juego.teclas.add(GLFW_KEY_W);
        juego.avanzar(5, 30);
        assertTrue(juego.autoZ >= 45 + clase2.MITAD_LARGO_AUTO - 0.001f);
        assertEquals(0f, juego.velocidad, 0.1f);
        juego.colocarJugador(50, -50, 0); // Reubica también el remolque antes de probar el borde.
        juego.avanzar(5, 30);
        assertTrue(juego.autoZ >= -clase1.LIMITE + clase2.MITAD_LARGO_AUTO - 0.001f);
        assertEquals(0f, juego.velocidad, 0.1f);
    }

    public void testFrenadoReversaYGiro() {
        Juego juego = new Juego();
        juego.teclas.add(GLFW_KEY_W);
        juego.avanzar(1, 60);
        assertTrue(juego.velocidad > 0);
        juego.teclas.clear();
        juego.teclas.add(GLFW_KEY_SPACE);
        juego.avanzar(2, 60);
        assertEquals(0f, juego.velocidad, 0.001f);
        juego.teclas.clear();
        juego.teclas.add(GLFW_KEY_S);
        juego.avanzar(1, 60);
        assertTrue(juego.velocidad < 0);
        juego.teclas.add(GLFW_KEY_A);
        juego.avanzar(0.1f, 60);
        assertTrue("El giro invierte su sentido en reversa", juego.angulo < 0);
    }

    public void testMovimientoComparableEntreFrecuencias() {
        Juego a = new Juego();
        Juego b = new Juego();
        a.teclas.add(GLFW_KEY_W);
        b.teclas.add(GLFW_KEY_W);
        a.avanzar(4, 30);
        b.avanzar(4, 144);
        assertEquals(a.autoZ, b.autoZ, 0.10f);
        assertEquals(a.velocidad, b.velocidad, 0.03f);
    }

    public void testReinicioPermiteOtroRecorrido() {
        Juego juego = new Juego();
        juego.autoX = 50;
        juego.autoZ = -50;
        juego.angulo = 2;
        juego.velocidad = 10;
        juego.tiempoAnimacion = 135;
        juego.tecla(GLFW_KEY_R);
        assertEquals(-30f, juego.autoX, 0f);
        assertEquals(30f, juego.autoZ, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0.0, juego.tiempoAnimacion, 0.0);
        juego.teclas.add(GLFW_KEY_W);
        juego.avanzar(1, 60);
        assertTrue(juego.autoZ < 30);
    }

    public void testInterruptoresDeCamaraLucesYMinimapa() {
        Juego juego = new Juego();
        for (int repeticion = 0; repeticion < 2; repeticion++) {
            juego.tecla(GLFW_KEY_C);
            juego.tecla(GLFW_KEY_N);
            juego.tecla(GLFW_KEY_F);
            juego.tecla(GLFW_KEY_M);
            assertEquals(repeticion == 0, juego.camaraAerea);
            assertEquals(repeticion == 1, juego.noche);
            assertEquals(repeticion == 1, juego.faros);
            assertEquals(repeticion == 1, juego.mostrarMapa);
        }
    }

    public void testSemaforosRepitenSinMetas() {
        Juego juego = new Juego();
        double[] segundos = {0, 4.99, 5, 9.99, 10, 11.99, 12, 12005};
        int[] esperados = {0, 0, 2, 2, 1, 1, 0, 2};
        for (int i = 0; i < segundos.length; i++) {
            juego.tiempoAnimacion = segundos[i];
            assertEquals(esperados[i], juego.faseSemaforo());
        }
        juego.avanzar(1, 60);
        assertTrue(juego.tiempoAnimacion > 12005);
    }

    public void testFarolasCubrenNuevosSectoresSinOcuparCalles() {
        Juego juego = new Juego();
        assertEquals(25, clase3.LUCES.length);
        int nuevas = 0;
        for (float[] luz : clase3.LUCES) {
            assertFalse("Poste en calle transitable", juego.puedeCircular(luz[0], luz[2]));
            assertTrue(Math.abs(luz[0]) < clase1.LIMITE && Math.abs(luz[2]) < clase1.LIMITE);
            if (Math.abs(luz[0]) > 35 || Math.abs(luz[2]) > 35) {
                nuevas++;
            }
        }
        assertEquals(16, nuevas);
    }

    public void testCamaraGeneralEncuadraCiudadAlRedimensionar() {
        class Camara extends clase1 {
            float[] ojo;
            float[] objetivo;
            @Override protected void vector(String nombre, float x, float y, float z) {
                if (nombre.equals("uOjo")) ojo = new float[] {x,y,z};
                if (nombre.equals("uObjetivo")) objetivo = new float[] {x,y,z};
            }
            @Override protected void decimal(String nombre, float valor) { }
        }
        Camara camara = new Camara();
        for (int[] dimensiones : new int[][] {{1100,760}, {320,900}, {1600,500}}) {
            camara.ancho = dimensiones[0];
            camara.alto = dimensiones[1];
            for (float orbita : new float[] {0, 0.6f, 1.5f, 3.1f}) {
                camara.orbita = orbita;
                camara.configurarCamara();
                double[] frente = normalizar(new double[] {camara.objetivo[0] - camara.ojo[0],
                    camara.objetivo[1] - camara.ojo[1], camara.objetivo[2] - camara.ojo[2]});
                double[] derecha = normalizar(new double[] {-frente[2], 0, frente[0]});
                double[] arriba = {derecha[1]*frente[2]-derecha[2]*frente[1],
                    derecha[2]*frente[0]-derecha[0]*frente[2], derecha[0]*frente[1]-derecha[1]*frente[0]};
                double tangente = Math.tan(Math.toRadians(clase1.CAMPO_VISUAL / 2));
                for (float x : new float[] {-55,55}) for (float z : new float[] {-55,55}) {
                    for (float y : new float[] {0,14}) {
                        double[] punto = {x-camara.ojo[0], y-camara.ojo[1], z-camara.ojo[2]};
                        double profundidad = producto(punto, frente);
                        assertTrue(profundidad > 0);
                        assertTrue(Math.abs(producto(punto, arriba)) < profundidad * tangente);
                        assertTrue(Math.abs(producto(punto, derecha)) < profundidad * tangente * camara.ancho / camara.alto);
                    }
                }
            }
        }
    }

    private static double producto(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private static double[] normalizar(double[] vector) {
        double longitud = Math.sqrt(producto(vector, vector));
        return new double[] {vector[0]/longitud, vector[1]/longitud, vector[2]/longitud};
    }
}
