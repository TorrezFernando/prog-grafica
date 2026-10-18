package com.graphics; // Mantiene esta lección junto a las clases anteriores.

import java.util.ArrayList; // Acumula las posiciones de las farolas generadas desde el mapa.
import java.util.List; // Describe la colección de posiciones de iluminación.
import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, farolas y focos del vehículo.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class clase3 extends clase2 {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Inicia la escena con iluminación nocturna.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    protected static final float[][] LUCES = crearLuces(); // Una farola por manzana, también en la ampliación.

    /** Sitúa las bombillas dentro de la esquina de cada acera, sin ocupar calles. */
    private static float[][] crearLuces() {
        List<float[]> luces = new ArrayList<>(); // Reúne las farolas de todas las manzanas, incluidas las nuevas.
        float desplazamiento = CELDA / 2 - 1; // Coloca la farola una unidad dentro de la esquina de la acera.
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas de la ciudad completa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las parcelas de cada fila.
                if (MAPA[fila][columna] != 0) { // Añade una luz en cada edificio o parque.
                    luces.add(new float[] {centro(columna) - desplazamiento, 4.5f, centro(fila) - desplazamiento}); // Comparte posición mundial entre farola e iluminación.
                }
            }
        }
        return luces.toArray(new float[0][]); // Devuelve la cantidad real de luces al dibujo y al shader.
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N: " + ambiente + " | F: faros " + estadoFaros; // Devuelve ambos indicadores y sus teclas.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de clase1.
    protected void prepararLuces() {
        entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        for (int indice = 0; indice < LUCES.length; indice++) { // Recorre todas las bombillas de la ciudad.
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        vector("uAuto", autoX, 0.7f, autoZ); // Envía el centro del vehículo a la altura de sus focos.
        vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        for (float[] luz : LUCES) { // Selecciona una farola a la vez.
            float x = luz[0]; // Lee la posición horizontal del poste.
            float y = luz[1]; // Lee la altura de la bombilla.
            float z = luz[2]; // Lee la posición del poste en profundidad.
            caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f); // Dibuja el poste delgado y oscuro.
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            caja(x, y, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Dibuja la bombilla de color cálido.
            entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }

    // ==================== 5. CÁLCULO DE LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que calcula el color iluminado de cada fragmento. */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
            #version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
            in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
            in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
            uniform vec3 uColor; // Recibe el color base de la caja.
            const int NUM_LUCES = @NUM_LUCES@; // Java inserta la cantidad real de farolas antes de compilar.
            uniform vec3 uLuces[NUM_LUCES]; // Geometría y luz comparten las mismas posiciones.
            uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
            uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
            uniform int uNoche; // Vale 1 de noche y 0 de día.
            uniform int uFaros; // Vale 1 cuando los focos están encendidos.
            uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
            uniform int uMapa; // Vale 1 durante el dibujo del minimapa de clase4.
            out vec4 color; // Entrega el color RGBA final al framebuffer.

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                    color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                    return; // Termina el shader sin calcular iluminación.
                }

                vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
                float intensidadSol = 0.65; // Define la fuerza de la iluminación direccional diurna.
                if (uNoche == 1) { // Ajusta el ambiente si es de noche.
                    luz = vec3(0.12, 0.16, 0.24); // Usa una luz ambiental tenue y azulada.
                    intensidadSol = 0.10; // Conserva una pequeña luz direccional nocturna.
                }
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su dirección.
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
                luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

                if (uNoche == 1) { // Calcula la iluminación de las farolas solo de noche.
                    for (int indice = 0; indice < NUM_LUCES; indice++) { // Acumula el aporte de cada bombilla.
                        vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
                        float distancia = length(haciaLuz); // Mide cuántas unidades separan superficie y bombilla.
                        float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Calcula la incidencia de la luz sobre la cara.
                        float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia; // Reduce el alcance con la distancia.
                        vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Define el tono cálido de la farola.
                        float alcance = 1.0 - smoothstep(14.0, 22.0, distancia); // Evita iluminar barrios lejanos.
                        luz += colorFarola * difusa * 3.0 * alcance / atenuacion;
                    }
                }

                if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
                    for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
                        vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Obtiene la dirección hacia el lado derecho del auto.
                        float separacion = -0.55; // Selecciona inicialmente el faro izquierdo.
                        if (indice == 1) { // Comprueba si corresponde calcular el segundo faro.
                            separacion = 0.55; // Desplaza el segundo faro al lado derecho.
                        }
                        vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion; // Ubica el faro delante de la carrocería.
                        vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
                        float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
                        float alineacion = dot(normalize(haciaSuperficie), eje); // Un valor cercano a 1 indica el centro del haz.
                        float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
                        float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Mide cuánto mira la cara hacia el faro.
                        float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
                        vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Define una luz frontal blanca y cálida.
                        float alcance = 1.0 - smoothstep(18.0, 28.0, distancia); // El haz se apaga gradualmente a 28 unidades.
                        luz += colorFaro * cono * difusa * 8.0 * alcance / atenuacion;
                    }
                }

                color = vec4(uColor * luz, 1.0); // Multiplica el material por toda la luz acumulada.
            }
            """.replace("@NUM_LUCES@", Integer.toString(LUCES.length)); // Sincroniza el arreglo GLSL con Java.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        clase3 aplicacion = new clase3(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
