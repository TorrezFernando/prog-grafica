package com.graphics; // Agrupa la misión con las clases del juego.

import java.util.Locale; // Conserva el mismo formato decimal en cualquier equipo.

/** Estado de la misión, independiente de OpenGL para comprobar cronómetro y reinicio. */
public final class Contrarreloj {
    public static final float META_X = 50; // Columna mundial de la meta al noreste.
    public static final float META_Z = -50; // Fila mundial de la meta.
    public static final float RADIO_META = 3; // Distancia máxima al centro para registrar la llegada.
    public enum Estado { LISTO, EN_CURSO, COMPLETADO } // Distingue espera, carrera activa y resultado congelado.
    private Estado estado = Estado.LISTO; // Espera el primer movimiento antes de contar tiempo.
    private double tiempo; // Segundos reales transcurridos en este intento.
    private double mejor = Double.POSITIVE_INFINITY; // Infinito indica que todavía no existe una marca.

    /** Inicia al desplazarse, cuenta el tiempo real y registra una llegada dentro del radio de meta. */
    public void actualizar(double segundos, float anteriorX, float anteriorZ, float x, float z) {
        if (estado == Estado.COMPLETADO) return; // Mantiene el resultado aunque el jugador continúe conduciendo.
        if (estado == Estado.LISTO && Math.hypot(x - anteriorX, z - anteriorZ) > 0.00001) { // Exige desplazamiento real, no solo aceleración.
            estado = Estado.EN_CURSO; // Activa el cronómetro al abandonar la salida.
        }
        if (estado != Estado.EN_CURSO) return; // Consultar el mapa antes de salir no consume tiempo.
        tiempo += Math.max(0, segundos); // Tiempo real: esperar o chocar también cuenta.
        if (Math.hypot(x - META_X, z - META_Z) <= RADIO_META) { // Detecta la llegada sin exigir que el vehículo frene.
            estado = Estado.COMPLETADO; // Congela el tiempo del intento.
            mejor = Math.min(mejor, tiempo); // Conserva el menor tiempo logrado en la sesión.
        }
    }

    /** Prepara otro intento y conserva el récord hasta que se cierre la aplicación. */
    public void reiniciar() {
        estado = Estado.LISTO; // Devuelve la misión a la espera del primer movimiento.
        tiempo = 0; // Conserva la mejor marca para competir contra el intento anterior.
    }

    public Estado estado() { return estado; } // Expone el estado a los indicadores del juego.
    public double tiempo() { return tiempo; } // Expone los segundos del intento actual.
    public double mejor() { return mejor; } // Expone la mejor marca de esta sesión.
    /** Presenta segundos con dos decimales o una marca vacía si aún no hay resultado. */
    public static String formato(double segundos) {
        return Double.isFinite(segundos) ? String.format(Locale.ROOT, "%.2f s", segundos) : "--"; // Evita mostrar infinito en el panel.
    }
}
