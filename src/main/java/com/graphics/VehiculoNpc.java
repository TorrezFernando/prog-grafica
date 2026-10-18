package com.graphics; // Mantiene el tráfico junto a las clases de la ciudad.

import java.util.function.Predicate; // Permite consultar colisiones sin depender de OpenGL.

/** Un automóvil autónomo con ruta cerrada, giros suaves y estado independiente. */
public final class VehiculoNpc {
    /** Posición y orientación que puede consultar el renderizador o el sistema de colisiones. */
    public record Posicion(float x, float z, float angulo) { }

    private static final double RADIO_GIRO = 3; // Redondea las esquinas sin cortar las manzanas.
    private final float limiteRuta; // Distancia del centro de la ciudad a cada lado de esta ruta.
    private final float velocidadCrucero; // Cada NPC mantiene su propia velocidad objetivo.
    private final double distanciaInicial; // Permite restaurar la distribución del tráfico al pulsar R.
    private double distancia; // Distancia recorrida sobre la ruta cerrada.
    private float velocidad; // Velocidad actual; vale cero si debe ceder el paso.
    private Posicion posicion; // Coordenadas y orientación actuales del vehículo.
    public final float rojo, verde, azul; // Color propio del vehículo y de su indicador del minimapa.

    /** Configura tamaño del circuito, velocidad, punto de partida y color de este conductor. */
    public VehiculoNpc(float limiteRuta, float velocidadCrucero, double distanciaInicial, float rojo, float verde, float azul) {
        this.limiteRuta = limiteRuta; // Guarda el tamaño del circuito sobre las calles.
        this.velocidadCrucero = velocidadCrucero; // Guarda la velocidad máxima de este conductor.
        this.distanciaInicial = distanciaInicial; // Guarda el desplazamiento inicial respecto al comienzo de la ruta.
        this.rojo = rojo; // Conserva la componente roja del color.
        this.verde = verde; // Conserva la componente verde del color.
        this.azul = azul; // Conserva la componente azul del color.
        reiniciar(); // Sitúa el vehículo en su posición inicial.
    }

    /** Recupera la posición, orientación y velocidad iniciales sin regenerar la ruta. */
    public void reiniciar() {
        distancia = distanciaInicial; // Restablece el punto de partida sobre el circuito.
        velocidad = 0; // Comienza detenido para acelerar suavemente.
        posicion = posicionEn(distancia); // Calcula la orientación correspondiente a ese punto.
    }

    public Posicion posicion() { return posicion; } // Ofrece una instantánea inmutable al resto del juego.
    public float velocidad() { return velocidad; } // Permite verificar si el NPC está circulando o esperando.

    /** Avanza por tiempo, validando el siguiente paso y una distancia de seguridad por delante. */
    public void actualizar(float deltaTime, Predicate<Posicion> pasoLibre) {
        float restante = Math.max(0, Math.min(deltaTime, 0.05f)); // Conserva la misma protección temporal del jugador.
        while (restante > 0) { // Comprueba las colisiones también durante los giros.
            float paso = Math.min(restante, 1f / 120f); // Evita saltos que atraviesen vehículos u obstáculos.
            float propuesta = Math.min(velocidadCrucero, velocidad + 3 * paso); // Acelera gradualmente al quedar libre la vía.
            Posicion siguiente = posicionEn(distancia + propuesta * paso); // Calcula el desplazamiento inmediato.
            Posicion adelante = posicionEn(distancia + 2 + propuesta * 0.6f); // Observa un margen antes de alcanzar al jugador.
            if (pasoLibre.test(siguiente) && pasoLibre.test(adelante)) { // Avanza solamente si ambos puntos están libres.
                velocidad = propuesta; // Acepta la nueva velocidad.
                distancia = (distancia + velocidad * paso) % longitudRuta(); // Repite el circuito al completar una vuelta.
                posicion = siguiente; // Publica la posición y el giro que se van a dibujar.
            } else { // El jugador u otro objeto ocupa su camino.
                velocidad = 0; // Espera sin empujar ni atravesar el obstáculo.
            }
            restante -= paso; // Descuenta el intervalo que acaba de simular.
        }
    }

    /** Devuelve la distancia de una vuelta completa para repetir el recorrido sin saltos. */
    public double longitudRuta() {
        return 4 * (2 * limiteRuta - 2 * RADIO_GIRO) + 2 * Math.PI * RADIO_GIRO; // Suma cuatro rectas y cuatro cuartos de círculo.
    }

    /** Convierte distancia en posición y tangente sobre un rectángulo de esquinas redondeadas. */
    public Posicion posicionEn(double recorrido) {
        double recta = 2 * limiteRuta - 2 * RADIO_GIRO; // Longitud útil de cada lado sin sus esquinas.
        double arco = Math.PI * RADIO_GIRO / 2; // Longitud de cada giro de noventa grados.
        double tramo = recta + arco; // Un lado seguido de una curva forma un cuarto del recorrido.
        double normalizada = ((recorrido % longitudRuta()) + longitudRuta()) % longitudRuta(); // Admite varias vueltas e índices negativos.
        int lado = Math.min(3, (int) (normalizada / tramo)); // Identifica el cuarto de la ruta.
        double local = normalizada - lado * tramo; // Distancia desde el inicio de ese cuarto.
        double x, z, frenteX, frenteZ; // Coordenadas y tangente antes de girar el cuarto de ruta.
        if (local < recta) { // Primer tramo: calle recta hacia el este.
            x = -limiteRuta + RADIO_GIRO + local; // Recorre el lado norte de izquierda a derecha.
            z = -limiteRuta; // Mantiene la posición sobre esa calle.
            frenteX = 1; // La tangente apunta hacia el este.
            frenteZ = 0; // No cambia de fila durante la recta.
        } else { // Segundo tramo: giro continuo hacia el siguiente lado.
            double giro = -Math.PI / 2 + (local - recta) / RADIO_GIRO; // Ángulo sobre el cuarto de círculo.
            x = limiteRuta - RADIO_GIRO + Math.cos(giro) * RADIO_GIRO; // Posición horizontal sobre la curva.
            z = -limiteRuta + RADIO_GIRO + Math.sin(giro) * RADIO_GIRO; // Posición vertical sobre la curva.
            frenteX = -Math.sin(giro); // Derivada horizontal del arco: dirección instantánea.
            frenteZ = Math.cos(giro); // Derivada vertical del arco.
        }
        double rotacion = lado * Math.PI / 2; // Orienta el mismo trazado en cada uno de los cuatro lados.
        double c = Math.cos(rotacion); // Coseno de la rotación del cuarto.
        double s = Math.sin(rotacion); // Seno de la rotación del cuarto.
        float mundoX = (float) (x * c - z * s); // Transforma la coordenada X al circuito completo.
        float mundoZ = (float) (x * s + z * c); // Transforma la coordenada Z al circuito completo.
        float angulo = (float) Math.atan2(-(frenteX * c - frenteZ * s), -(frenteX * s + frenteZ * c)); // Orienta el frente local -Z sobre la tangente.
        return new Posicion(mundoX, mundoZ, angulo); // Devuelve una pose consistente para dibujo y colisión.
    }
}
