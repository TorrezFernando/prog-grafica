package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import java.util.ArrayDeque; // Almacena las marcas de neumáticos en orden de antigüedad.
import java.util.Locale; // Mantiene un formato decimal uniforme para los indicadores.
import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected static final float INICIO_X = -30; // Inicio del recorrido original, conservado dentro del núcleo.
    protected static final float INICIO_Z = 30; // Fila mundial de la salida dentro del núcleo original.
    protected float autoX = INICIO_X; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = INICIO_Z; // Posición inicial sobre una calle del núcleo.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float MITAD_ANCHO_AUTO = 1f; // Incluye las ruedas: la huella real mide 2 unidades.
    protected static final float MITAD_LARGO_AUTO = 1.36f; // Incluye carrocería y luces, sin margen circular invisible.
    protected static final double DURACION_TURBO = 1.0; // Un segundo de impulso por pulsación.
    protected static final double COOLDOWN_TURBO = 2.0; // Dos segundos desde que termina el impulso.
    protected double turboRestante; // Segundos de impulso disponibles en la activación actual.
    protected double turboEspera; // Segundos que faltan para permitir otra activación.
    protected boolean derrapando; // Indica si el freno de mano mantiene una deriva lateral.
    protected float desfaseDerrape; // Dirección de desplazamiento menos orientación de la carrocería.
    private float intervaloMarcas; // Separa temporalmente las marcas para evitar acumularlas en cada paso.
    private final ArrayDeque<Marca> marcas = new ArrayDeque<>(); // Conserva un historial limitado de huellas sobre el asfalto.
    private static final class Marca { // Agrupa posición, orientación y vida de una huella.
        final float x, z, angulo; // Guarda la pose de la huella al tocar el asfalto.
        float vida = 6; // La marca permanece visible durante seis segundos.
        Marca(float x, float z, float angulo) { this.x = x; this.z = z; this.angulo = angulo; } // Conserva la pose sin seguir los movimientos posteriores del auto.
    }

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
        if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) activarTurbo(); // Activa un impulso una sola vez al recibir la pulsación.
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = INICIO_X; // Recupera la coordenada X de inicio.
        autoZ = INICIO_Z; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
        turboRestante = 0; // Termina cualquier impulso en curso.
        turboEspera = 0; // Deja el turbo disponible.
        derrapando = false; // Desactiva el estado de derrape.
        desfaseDerrape = 0; // Alinea desplazamiento y carrocería.
        intervaloMarcas = 0; // Reinicia el intervalo entre huellas.
        marcas.clear(); // Elimina las marcas del intento anterior.
    }

    /** Una pulsación da un segundo de impulso; mantener Shift no lo reactiva automáticamente. */
    protected void activarTurbo() {
        if (turboRestante <= 0 && turboEspera <= 0 && velocidad >= 0) turboRestante = DURACION_TURBO; // Exige disponibilidad y evita impulsar una marcha atrás.
    }

    /** Consume tiempo real y transfiere el sobrante al cooldown al terminar el impulso. */
    private void actualizarTurbo(double segundos) {
        if (turboRestante > 0) { // Consume primero el tiempo del impulso activo.
            double consumido = Math.min(segundos, turboRestante); // Limita el consumo al impulso que realmente queda.
            turboRestante -= consumido; // Descuenta los segundos usados de impulso.
            segundos -= consumido; // Conserva el tiempo sobrante para aplicarlo a la espera.
            if (turboRestante < 0.0000001) { // Tolera el pequeño error de redondeo al agotar el impulso.
                turboRestante = 0; // Termina cualquier impulso en curso.
                turboEspera = COOLDOWN_TURBO; // Inicia los dos segundos de espera al terminar o cancelar el turbo.
            }
        }
        turboEspera = Math.max(0, turboEspera - segundos); // Consume el sobrante sin permitir una espera negativa.
        if (turboEspera < 0.0000001) turboEspera = 0; // Normaliza a cero el final de la espera.
    }

    /** Describe impulso activo, espera restante o disponibilidad para los indicadores. */
    protected String estadoTurbo() {
        if (turboRestante > 0) return "TURBO ACTIVO"; // Distingue el impulso activo en el panel.
        if (turboEspera > 0) return String.format(Locale.ROOT, "Turbo: %.1f s", turboEspera); // Muestra cuánto falta para volver a activarlo.
        return "Turbo listo: Shift"; // Indica la tecla cuando el impulso está disponible.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        boolean freno = pulsada(GLFW_KEY_SPACE); // Freno de mano: permite derrapar al girar con velocidad.
        boolean frenoServicio = pulsada(GLFW_KEY_LEFT_CONTROL) || pulsada(GLFW_KEY_RIGHT_CONTROL); // Acepta ambos Ctrl para frenar con fuerza.
        if (turboRestante > 0 && (frenoServicio || acelerador < 0)) { // Frenar o pedir reversa interrumpe el impulso.
            turboRestante = 0; // Frenar o pedir reversa cancela el impulso sin eludir el cooldown.
            turboEspera = COOLDOWN_TURBO; // Inicia los dos segundos de espera al terminar o cancelar el turbo.
        }
        for (Marca marca : marcas) marca.vida -= Math.max(0, deltaTime); // Envejece las huellas con el tiempo real transcurrido.
        while (!marcas.isEmpty() && marcas.peekFirst().vida <= 0) marcas.removeFirst(); // Retira las huellas expiradas desde la más antigua.
        float restante = Math.max(0, Math.min(deltaTime, 0.05f)); // Conserva la protección frente a pausas largas.
        while (restante > 0) { // Subdivide para mantener un manejo similar a distintas frecuencias de dibujo.
            float paso = Math.min(restante, 1f / 120f); // Divide la integración en intervalos cortos y seguros.
            moverAuto(paso, acelerador, direccion, freno, frenoServicio); // Integra velocidad, giro y desplazamiento de este intervalo.
            actualizarTurbo(paso); // Consume el impulso y su espera en el mismo intervalo.
            restante -= paso; // Descuenta el intervalo que ya se simuló.
        }
        actualizarTurbo(Math.max(0, deltaTime - 0.05f)); // Un cuadro lento no alarga el turbo ni su espera.
        actualizarTitulo(); // Muestra los indicadores después de actualizar el vehículo.
    }

    /** Integra un paso corto y rechaza desplazamientos que invaden obstáculos o bordes. */
    private void moverAuto(float deltaTime, float acelerador, float direccion, boolean freno, boolean frenoServicio) {
        derrapando = freno && !frenoServicio && velocidad > 3 && // Exige velocidad y freno de mano para iniciar la deriva.
            (direccion != 0 || Math.abs(desfaseDerrape) > 0.04f); // Mantiene el derrape mientras se gira o queda inercia lateral.
        boolean turbo = turboRestante > 0; // Selecciona las propiedades del impulso durante este paso.
        float resistencia = frenoServicio || (freno && !derrapando) ? 7 : derrapando ? 1.0f : turbo ? 0.3f : 0.7f; // Aplica frenado fuerte o la resistencia correspondiente al modo de conducción.
        float limite = turbo ? 28 : Math.max(16, velocidad); // Al agotarse el turbo reduce la velocidad gradualmente.
        velocidad += (turbo ? 32 : acelerador * 9) * deltaTime; // Integra la aceleración del motor o del turbo.
        velocidad *= (float) Math.exp(-resistencia * deltaTime); // Reduce la velocidad con una resistencia independiente de los cuadros por segundo.
        velocidad = Math.max(-6, Math.min(limite, velocidad)); // Respeta el límite de avance y de marcha atrás.
        float giro = direccion * velocidad * (derrapando ? 0.18f : 0.11f) * deltaTime; // La carrocería gira más durante el derrape.
        float nuevoAngulo = angulo + giro; // Propone la orientación del siguiente paso.
        if (intentarMoverJugador(autoX, autoZ, nuevoAngulo)) { // Valida también el remolque antes de aceptar el giro.
            angulo = nuevoAngulo; // Acepta el giro si la huella cabe en la vía.
            if (derrapando || Math.abs(desfaseDerrape) > 0.001f) desfaseDerrape -= giro; // Conserva la dirección anterior del desplazamiento al girar la carrocería.
        }
        desfaseDerrape *= (float) Math.exp(-(derrapando ? 1.2f : 7f) * deltaTime); // Recupera el agarre más rápido cuando se suelta el freno de mano.
        desfaseDerrape = Math.max(-1.05f, Math.min(1.05f, desfaseDerrape)); // Evita que la deriva invierta el desplazamiento.
        if (Math.abs(velocidad) < 0.2f || Math.abs(desfaseDerrape) < 0.001f) desfaseDerrape = 0; // Elimina deriva insignificante al detenerse o recuperar el agarre.
        float frenteX = -(float) Math.sin(angulo + desfaseDerrape); // La inercia conserva parte de la dirección anterior.
        float frenteZ = -(float) Math.cos(angulo + desfaseDerrape); // Calcula la componente Z del desplazamiento con inercia.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        if (intentarMoverJugador(siguienteX, siguienteZ, angulo)) { // Comprueba el conjunto antes de mover el auto.
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // Conserva la componente libre al rozar una pared o esquina.
            boolean deslizado = false; // Registra si alguna componente del movimiento sigue libre.
            if (Math.abs(siguienteX - autoX) > 0.00001f && intentarMoverJugador(siguienteX, autoZ, angulo)) { // Comprueba la chata también al deslizar en X.
                autoX = siguienteX; // Acepta la componente horizontal libre.
                deslizado = true; // Conserva velocidad porque el auto aún puede avanzar.
            }
            if (Math.abs(siguienteZ - autoZ) > 0.00001f && intentarMoverJugador(autoX, siguienteZ, angulo)) { // Comprueba la chata también al deslizar en Z.
                autoZ = siguienteZ; // Acepta la componente de profundidad libre.
                deslizado = true; // Conserva velocidad porque el auto aún puede avanzar.
            }
            if (!deslizado) { // Un choque frontal bloquea las dos componentes.
                velocidad = 0; // Detiene el vehículo para permitir después una salida en reversa.
                desfaseDerrape = 0; // Alinea desplazamiento y carrocería.
                derrapando = false; // Desactiva el estado de derrape.
            }
        }
        intervaloMarcas += deltaTime; // Acumula tiempo entre marcas de neumáticos.
        if (derrapando && intervaloMarcas >= 0.06f) { // Dibuja nuevas huellas solo si persiste el derrape y se cumple el intervalo.
            intervaloMarcas = 0; // Reinicia el intervalo entre huellas.
            float c = (float) Math.cos(angulo); // Obtiene el coseno para ubicar las ruedas sobre el asfalto.
            float s = (float) Math.sin(angulo); // Obtiene el seno de la orientación actual.
            for (float lado : new float[] {-0.88f, 0.88f}) { // Marca las ruedas traseras de ambos lados.
                marcas.addLast(new Marca(autoX + c * lado + s * 0.82f, autoZ - s * lado + c * 0.82f, angulo)); // Transforma cada rueda trasera al espacio de la ciudad.
            }
            while (marcas.size() > 160) marcas.removeFirst(); // Limita el historial para mantener estable el uso de memoria.
        }
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    protected void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = "Ciudad " + MAPA.length + "x" + MAPA[0].length + " | " + kilometrosPorHora + " km/h"; // Identifica el tamaño real del mapa y la velocidad.
        titulo += estadoExtra(); // Añade luces y minimapa antes de la ayuda.
        titulo += " | " + estadoTurbo() + (derrapando ? " | DERRAPE" : ""); // Añade el estado del impulso y de la deriva.
        titulo += camaraAerea ? " | C: aerea" : " | C: seguir"; // Identifica la cámara seleccionada.
        titulo += " | R: reiniciar | WASD | Espacio: derrape | Ctrl: freno | ESC"; // Presenta los controles principales en el título.
        if (ventana != 0) { // Permite verificar la lógica sin crear un contexto gráfico.
            glfwSetWindowTitle(ventana, titulo); // Actualiza la ventana después de construir todos los indicadores.
        }
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Valida una maniobra real; clase4 añade aquí la propuesta y confirmación del remolque. */
    protected boolean intentarMoverJugador(float x, float z, float giro) {
        return puedeMoverJugador(x, z, giro); // Las etapas sin chata conservan la conducción original.
    }

    /** Punto de extensión para contactos reales; las consultas de rutas siguen sin efectos secundarios. */
    protected boolean puedeMoverJugador(float x, float z, float giro) {
        return puedeCircular(x, z, giro);
    }

    /** Comprueba la huella del automóvil en su orientación actual. */
    protected boolean puedeCircular(float x, float z) {
        return puedeCircular(x, z, angulo); // Usa la orientación actual para las consultas de movimiento del jugador.
    }

    /** Rectángulo orientado contra los bordes y las aceras dibujadas. */
    protected boolean puedeCircular(float x, float z, float giro) {
        float c = Math.abs((float) Math.cos(giro)); // Proyección absoluta del eje local sobre X.
        float s = Math.abs((float) Math.sin(giro)); // Proyección absoluta del eje local sobre Z.
        float extensionX = c * MITAD_ANCHO_AUTO + s * MITAD_LARGO_AUTO; // Calcula la extensión horizontal de la huella girada.
        float extensionZ = s * MITAD_ANCHO_AUTO + c * MITAD_LARGO_AUTO; // Calcula la extensión de profundidad de la huella girada.
        if (Math.abs(x) + extensionX > LIMITE || Math.abs(z) + extensionZ > LIMITE) return false; // Impide que cualquier esquina salga del suelo.

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float mitad = CELDA / 2; // El obstáculo ocupa la misma parcela que la acera dibujada.
                if (colisionRectangulo(x, z, giro, centroX, centroZ, mitad, mitad)) { // Comprueba la huella contra la parcela ocupada.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /** Teorema de ejes separadores: dos ejes mundiales y dos del automóvil. */
    protected boolean colisionRectangulo(float x, float z, float giro, float centroX, float centroZ,
                                         float mitadX, float mitadZ) {
        float c = (float) Math.cos(giro); // Calcula el coseno del rectángulo móvil.
        float s = (float) Math.sin(giro); // Calcula el seno del rectángulo móvil.
        float ac = Math.abs(c); // Usa magnitudes positivas para proyectar semiejes.
        float as = Math.abs(s); // Conserva la magnitud de la segunda proyección.
        float dx = x - centroX; // Separa los centros sobre el eje X mundial.
        float dz = z - centroZ; // Separa los centros sobre el eje Z mundial.
        float tolerancia = 0.0001f; // Permite el contacto exacto sin bloquear un movimiento paralelo.
        if (Math.abs(dx) >= mitadX + ac * MITAD_ANCHO_AUTO + as * MITAD_LARGO_AUTO - tolerancia) return false; // Un eje X separado descarta la colisión.
        if (Math.abs(dz) >= mitadZ + as * MITAD_ANCHO_AUTO + ac * MITAD_LARGO_AUTO - tolerancia) return false; // Un eje Z separado descarta la colisión.
        if (Math.abs(dx * c - dz * s) >= MITAD_ANCHO_AUTO + ac * mitadX + as * mitadZ - tolerancia) return false; // Comprueba separación sobre el eje del ancho del automóvil.
        if (Math.abs(dx * s + dz * c) >= MITAD_LARGO_AUTO + as * mitadX + ac * mitadZ - tolerancia) return false; // Comprueba separación sobre el eje del largo del automóvil.
        return true; // Sin un eje de separación, las huellas se superponen.
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        if (!vistaMapa) { // Reserva los detalles del asfalto para el pase principal.
            for (Marca marca : marcas) { // Recorre las huellas aún vigentes.
                cajaGirada(marca.x, 0.075f, marca.z, 0.22f, 0.015f, 0.6f, 0.035f, 0.04f, 0.045f, marca.angulo); // Dibuja una huella oscura ligeramente elevada sobre el asfalto.
            }
        }
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
        if (turboRestante > 0 && !vistaMapa) { // Muestra el escape luminoso durante el impulso en la vista 3D.
            entero("uEmision", 1); // Permite ver el color propio sin depender de la iluminación ambiental.
            pieza(-0.45f, 0.45f, 1.65f, 0.22f, 0.25f, 0.65f, 0.10f, 0.65f, 1); // Dibuja el escape azul trasero izquierdo del turbo.
            pieza(0.45f, 0.45f, 1.65f, 0.22f, 0.25f, 0.65f, 0.10f, 0.65f, 1); // Dibuja el escape azul trasero derecho del turbo.
            entero("uEmision", 0); // Recupera la iluminación normal para los objetos siguientes.
        }
    }

    /** Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color RGB. */
    protected void dibujarAuto() {
        dibujarVehiculo(autoX, autoZ, angulo, 0.95f, 0.24f, 0.12f); // El jugador utiliza el mismo modelo reutilizable que los NPC.
    }

    /** Dibuja un vehículo en su propia posición sin modificar el estado del jugador. */
    protected void dibujarVehiculo(float xAuto, float zAuto, float giroAuto, float r, float g, float b) {
        piezaEn(xAuto, zAuto, giroAuto, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, r, g, b); // Carrocería del color elegido.
        piezaEn(xAuto, zAuto, giroAuto, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Cabina acristalada.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                piezaEn(xAuto, zAuto, giroAuto, x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        float[] ladosFaros = {-0.55f, 0.55f}; // Define la separación lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            piezaEn(xAuto, zAuto, giroAuto, x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Dibuja un faro delantero claro.
            piezaEn(xAuto, zAuto, giroAuto, x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Dibuja una luz trasera roja.
        }
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        piezaEn(autoX, autoZ, angulo, x, y, z, sx, sy, sz, r, g, b); // Conserva el acceso cómodo a piezas del jugador.
    }

    /** Transforma una pieza local usando la pose del vehículo indicado. */
    private void piezaEn(float xAuto, float zAuto, float giroAuto, float x, float y, float z,
                         float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(giroAuto); // Calcula el coseno de la orientación de este vehículo.
        float seno = (float) Math.sin(giroAuto); // Calcula el seno de la misma orientación.
        float mundoX = xAuto + coseno * x + seno * z; // Gira la posición local y suma su posición X.
        float mundoZ = zAuto - seno * x + coseno * z; // Gira la posición local y suma su posición Z.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, giroAuto); // Dibuja la pieza con la orientación correcta.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
