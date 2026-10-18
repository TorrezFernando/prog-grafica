package com.graphics;

import java.util.ArrayList;
import java.util.List;

/** Geometría común para la pintura y las rutas: conecta dos aceras enfrentadas. */
public record PasoPeatonal(float x, float z, boolean cruzaEnX, float longitud) {
    public static final float ANCHO = 2.4f;
    public record Punto(float x, float z) { }
    public record Franja(float x, float z, float ancho, float largo) { }

    /** El extremo de la ruta queda sobre la acera, fuera de la calzada. */
    public Punto punto(float distancia) {
        return new Punto(x + (cruzaEnX ? distancia : 0), z + (cruzaEnX ? 0 : distancia));
    }

    public float extremo() { return longitud / 2 + 0.6f; }

    /** Franjas paralelas al tráfico, distribuidas a lo ancho de toda la calzada. */
    public List<Franja> franjas() {
        List<Franja> resultado = new ArrayList<>();
        int cantidad = Math.max(1, Math.round(longitud));
        float intervalo = longitud / cantidad;
        for (int i = 0; i < cantidad; i++) {
            Punto p = punto(-longitud / 2 + (i + 0.5f) * intervalo);
            resultado.add(new Franja(p.x(), p.z(), cruzaEnX ? intervalo * 0.55f : ANCHO,
                cruzaEnX ? ANCHO : intervalo * 0.55f));
        }
        return List.copyOf(resultado);
    }

    /** Genera cruces cerca de las esquinas, solo donde existen aceras a ambos lados. */
    public static List<PasoPeatonal> generar(int[][] mapa, float celda) {
        List<PasoPeatonal> resultado = new ArrayList<>();
        float origenZ = -(mapa.length - 1) * celda / 2;
        float origenX = -(mapa[0].length - 1) * celda / 2;
        float esquina = celda / 2 - ANCHO / 2 - 0.3f;
        for (int f = 0; f < mapa.length; f++) {
            for (int c = 0; c < mapa[f].length; c++) {
                if (mapa[f][c] != 0) continue;
                float x = origenX + c * celda, z = origenZ + f * celda;
                for (float lado : new float[] {-esquina, esquina}) {
                    if (c > 0 && c + 1 < mapa[f].length && mapa[f][c - 1] != 0 && mapa[f][c + 1] != 0)
                        resultado.add(new PasoPeatonal(x, z + lado, true, celda));
                    if (f > 0 && f + 1 < mapa.length && mapa[f - 1][c] != 0 && mapa[f + 1][c] != 0)
                        resultado.add(new PasoPeatonal(x + lado, z, false, celda));
                }
            }
        }
        return List.copyOf(resultado);
    }
}
