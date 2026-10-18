package com.graphics;

import java.util.function.Predicate;

/** Bot peatonal básico, independiente de OpenGL y reemplazable por un modelo animado. */
public final class PeatonNpc {
    public static final float RADIO = 0.32f;
    public static final float VELOCIDAD = 1.4f;
    public static final float RECUPERACION = 3;
    private final PasoPeatonal ruta;
    private final float esperaInicial;
    private float distancia, sentido, espera, recuperacion;
    private boolean impactado;

    public PeatonNpc(PasoPeatonal ruta, float esperaInicial) {
        this.ruta = ruta;
        this.esperaInicial = Math.max(0, esperaInicial);
        reiniciar();
    }

    public void reiniciar() {
        distancia = -ruta.extremo();
        sentido = 1;
        espera = esperaInicial;
        recuperacion = 0;
        impactado = false;
    }

    public PasoPeatonal.Punto posicion() { return ruta.punto(distancia); }
    public boolean impactado() { return impactado; }
    public boolean sobreAcera() { return Math.abs(distancia) >= ruta.longitud() / 2; }

    /** Devuelve true una sola vez por incidente, aunque se repitan consultas de movimiento. */
    public boolean registrarImpacto() {
        if (impactado) return false;
        impactado = true;
        recuperacion = RECUPERACION;
        return true;
    }

    /** Camina de acera a acera; espera si un vehículo ocupa su siguiente posición. */
    public void actualizar(float deltaTime, Predicate<PasoPeatonal.Punto> libre) {
        float restante = Math.max(0, Math.min(deltaTime, 0.05f));
        while (restante > 0) {
            float paso = Math.min(restante, 1f / 120f);
            restante -= paso;
            if (impactado) {
                recuperacion = Math.max(0, recuperacion - paso);
                // Reaparece en la acera únicamente cuando hay espacio; nunca dentro de un auto.
                if (recuperacion == 0 && libre.test(ruta.punto(-ruta.extremo()))) reiniciar();
                continue;
            }
            if (espera > 0) { espera = Math.max(0, espera - paso); continue; }
            float siguiente = Math.max(-ruta.extremo(), Math.min(ruta.extremo(), distancia + sentido * VELOCIDAD * paso));
            if (!libre.test(ruta.punto(siguiente))) continue;
            distancia = siguiente;
            if (Math.abs(distancia) >= ruta.extremo()) {
                sentido = -sentido;
                espera = 2;
            }
        }
    }

    /** Círculo contra la huella orientada del auto, sin ampliar artificialmente las esquinas. */
    public static boolean colision(PasoPeatonal.Punto p, float x, float z, float angulo,
                                   float mitadAncho, float mitadLargo) {
        float c = (float) Math.cos(angulo), s = (float) Math.sin(angulo);
        float dx = p.x() - x, dz = p.z() - z;
        float localX = c * dx - s * dz, localZ = s * dx + c * dz;
        float fueraX = Math.max(0, Math.abs(localX) - mitadAncho);
        float fueraZ = Math.max(0, Math.abs(localZ) - mitadLargo);
        return fueraX * fueraX + fueraZ * fueraZ < RADIO * RADIO;
    }
}
