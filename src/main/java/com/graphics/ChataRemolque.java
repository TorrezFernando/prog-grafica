package com.graphics; // Mantiene el remolque junto al vehículo y la escena.

import java.util.List; // Agrupa las huellas del cajón y la lanza de enganche.

/**
 * Chata de un eje: seguimiento cinemático articulado, geometría de colisión y dibujo.
 * El eje sigue el desplazamiento real del enganche, incluido el derrape y la reversa.
 * Proponer una pose no modifica el estado: se confirma solo después de validar el conjunto.
 */
public final class ChataRemolque {
    public static final float ENGANCHE_AUTO = 1.5f; // Distancia desde el centro del auto hasta la rótula trasera.
    public static final float DISTANCIA_EJE = 3f; // Longitud fija desde la rótula hasta el eje de la chata.
    public static final float MITAD_ANCHO = 1f; // Incluye las dos ruedas, igual que la colisión del auto.
    public static final float MITAD_LARGO = 1.2f; // Incluye plataforma, barandas y luces traseras.
    public static final float ARTICULACION_MAXIMA = (float) Math.toRadians(70); // Evita plegar la chata contra el auto.
    private Posicion posicion; // Pose aceptada del eje; nunca se cambia durante una consulta de colisión.

    /** Pose del eje y de la plataforma; el frente local apunta hacia -Z, como el auto. */
    public record Posicion(float x, float z, float angulo) { }

    /** Rectángulo orientado, utilizado tanto para el cajón como para la lanza estrecha. */
    public record Huella(float x, float z, float angulo, float mitadAncho, float mitadLargo) {
        /** Comprueba los cuatro ejes separadores; permite el contacto sin penetración. */
        public boolean colision(Huella otra) {
            for (float giro : new float[] {angulo, otra.angulo}) { // Usa los ejes locales de ambos rectángulos.
                float c = (float) Math.cos(giro), s = (float) Math.sin(giro); // Obtiene los vectores de ancho y largo.
                if (separadas(otra, c, -s) || separadas(otra, s, c)) return false; // Basta un eje libre para descartar contacto.
            }
            return true; // Las proyecciones se solapan sobre los cuatro ejes.
        }

        /** Proyecta la distancia entre centros y las dimensiones sobre un eje unitario. */
        private boolean separadas(Huella otra, float ejeX, float ejeZ) {
            float distancia = Math.abs((otra.x - x) * ejeX + (otra.z - z) * ejeZ); // Separación de centros proyectada.
            return distancia >= proyeccion(ejeX, ejeZ) + otra.proyeccion(ejeX, ejeZ) - 0.00001f; // Tolera el contacto exacto.
        }

        /** Radio de la huella proyectado sobre el eje indicado. */
        private float proyeccion(float ejeX, float ejeZ) {
            float c = (float) Math.cos(angulo), s = (float) Math.sin(angulo); // Orientación de esta pieza.
            return mitadAncho * Math.abs(c * ejeX - s * ejeZ) + mitadLargo * Math.abs(s * ejeX + c * ejeZ); // Suma ambas contribuciones.
        }

        /** Exige que las cuatro esquinas estén dentro del suelo de la ciudad. */
        public boolean dentroDelLimite(float limite) {
            return Math.abs(x) + proyeccion(1, 0) <= limite && Math.abs(z) + proyeccion(0, 1) <= limite; // Comprueba X y Z.
        }

        /** Reutiliza la colisión circular del peatón con las dimensiones de esta pieza. */
        public boolean colisionPeaton(PasoPeatonal.Punto p) {
            return PeatonNpc.colision(p, x, z, angulo, mitadAncho, mitadLargo); // Respeta la orientación del remolque.
        }
    }

    /** Crea la única chata del jugador, alineada y enganchada al auto. */
    public ChataRemolque(float autoX, float autoZ, float angulo) {
        reiniciar(autoX, autoZ, angulo); // Coloca el eje detrás del enganche inicial.
    }

    /** Restablece la alineación al reiniciar o colocar una escena de prueba. */
    public void reiniciar(float autoX, float autoZ, float angulo) {
        float distancia = ENGANCHE_AUTO + DISTANCIA_EJE; // Separación total entre los centros del auto y la chata.
        posicion = new Posicion(autoX + (float) Math.sin(angulo) * distancia,
            autoZ + (float) Math.cos(angulo) * distancia, angulo); // Transforma el desplazamiento trasero al mundo.
    }

    public Posicion posicion() { return posicion; } // Expone la pose aceptada al dibujo y a los otros actores.
    public void confirmar(Posicion propuesta) { posicion = propuesta; } // Acepta una propuesta ya validada por la escena.

    /**
     * Proyecta el eje anterior sobre el círculo de radio fijo alrededor del nuevo enganche.
     * En pasos cortos aproxima un eje sin deslizamiento lateral: sigue al tirar y se articula al empujar.
     */
    public Posicion proponer(Posicion anterior, float autoX, float autoZ, float anguloAuto) {
        float engancheX = autoX + (float) Math.sin(anguloAuto) * ENGANCHE_AUTO; // Rótula en la parte trasera del auto.
        float engancheZ = autoZ + (float) Math.cos(anguloAuto) * ENGANCHE_AUTO; // El enganche también se desplaza durante un giro.
        float dx = anterior.x - engancheX, dz = anterior.z - engancheZ; // Dirección desde el nuevo enganche al eje anterior.
        float angulo = dx * dx + dz * dz < 0.000001f ? anterior.angulo : (float) Math.atan2(dx, dz); // Conserva orientación si la dirección es degenerada.
        return new Posicion(engancheX + (float) Math.sin(angulo) * DISTANCIA_EJE,
            engancheZ + (float) Math.cos(angulo) * DISTANCIA_EJE, angulo); // Mantiene exactamente la longitud de la lanza.
    }

    /** Limita el ángulo relativo sin teletransportar ni enderezar artificialmente el remolque. */
    public boolean articulacionValida(Posicion p, float anguloAuto) {
        double diferencia = Math.atan2(Math.sin(p.angulo - anguloAuto), Math.cos(p.angulo - anguloAuto)); // Normaliza entre -pi y pi.
        return Math.abs(diferencia) <= ARTICULACION_MAXIMA; // Una maniobra que pliega más el conjunto se rechaza.
    }

    /** La plataforma y la lanza tienen colisiones propias, evitando un rectángulo grande invisible. */
    public List<Huella> huellas(Posicion p) {
        float centroLanza = DISTANCIA_EJE / 2; // La lanza une el eje con la rótula, bajo el piso de la plataforma.
        return List.of(new Huella(p.x, p.z, p.angulo, MITAD_ANCHO, MITAD_LARGO),
            new Huella(p.x - (float) Math.sin(p.angulo) * centroLanza,
                p.z - (float) Math.cos(p.angulo) * centroLanza, p.angulo, 0.09f, centroLanza)); // Incluye la barra de 18 cm de ancho.
    }

    /** Comprueba si alguna pieza de la chata ocupa la huella de otro vehículo. */
    public boolean colision(Posicion p, Huella otra) {
        for (Huella huella : huellas(p)) if (huella.colision(otra)) return true; // Recorre plataforma y lanza.
        return false; // Ninguna pieza toca el obstáculo.
    }

    /** Comprueba peatones junto a la plataforma o la lanza de enganche. */
    public boolean colisionPeaton(Posicion p, PasoPeatonal.Punto peaton) {
        for (Huella huella : huellas(p)) if (huella.colisionPeaton(peaton)) return true; // Comparte el radio peatonal existente.
        return false; // El peatón queda fuera del remolque completo.
    }

    /** Dibuja únicamente esta chata; los modelos reutilizados por los NPC permanecen sin remolque. */
    public void dibujar(clase1 escena) {
        Posicion p = posicion; // Usa exactamente la pose validada por las colisiones.
        if (escena.vistaMapa) { // Representa el remolque detrás del indicador del jugador.
            pieza(escena, p, 0, 24, 0, 2, 0.1f, 2.4f, 0.95f, 0.68f, 0.2f); // Plataforma dorada por encima de los edificios.
            pieza(escena, p, 0, 24, -DISTANCIA_EJE / 2, 0.18f, 0.1f, DISTANCIA_EJE, 0.7f, 0.75f, 0.8f); // Enganche visible en el mapa.
            return; // Omite los detalles pequeños en la vista superior.
        }
        pieza(escena, p, 0, 0.48f, -DISTANCIA_EJE / 2, 0.18f, 0.16f, DISTANCIA_EJE, 0.35f, 0.4f, 0.45f); // Lanza metálica hasta la rótula.
        pieza(escena, p, 0, 0.56f, 0, 1.65f, 0.22f, 2.3f, 0.3f, 0.34f, 0.38f); // Chasis de la plataforma.
        for (int tabla = -3; tabla <= 3; tabla++) { // Construye el piso con siete tablas longitudinales.
            pieza(escena, p, tabla * 0.22f, 0.7f, 0, 0.2f, 0.08f, 2.18f, 0.65f, 0.43f, 0.22f); // Separa ligeramente las tablas.
        }
        for (float lado : new float[] {-1, 1}) { // Añade ruedas y barandas a ambos lados.
            pieza(escena, p, lado * 0.9f, 0.34f, 0, 0.2f, 0.64f, 0.64f, 0.055f, 0.065f, 0.08f); // Ruedas sobre el eje central.
            pieza(escena, p, lado * 0.82f, 0.98f, 0, 0.08f, 0.45f, 2.3f, 0.9f, 0.57f, 0.14f); // Barandas laterales amarillas.
            pieza(escena, p, lado * 0.55f, 0.59f, 1.17f, 0.26f, 0.16f, 0.05f, 0.9f, 0.04f, 0.03f); // Luces rojas traseras.
        }
        pieza(escena, p, 0, 0.98f, -1.1f, 1.65f, 0.45f, 0.08f, 0.9f, 0.57f, 0.14f); // Baranda delantera.
        pieza(escena, p, 0, 0.88f, 1.1f, 1.65f, 0.25f, 0.08f, 0.9f, 0.57f, 0.14f); // Compuerta trasera baja.
    }

    /** Transforma una pieza local con la orientación propia de la chata. */
    private void pieza(clase1 escena, Posicion p, float x, float y, float z,
                       float ancho, float alto, float largo, float r, float g, float b) {
        float c = (float) Math.cos(p.angulo), s = (float) Math.sin(p.angulo); // Rotación compartida por todas las piezas.
        escena.cajaGirada(p.x + c * x + s * z, y, p.z - s * x + c * z,
            ancho, alto, largo, r, g, b, p.angulo); // Lleva posición y orientación al mundo sin alterar al jugador.
    }
}
