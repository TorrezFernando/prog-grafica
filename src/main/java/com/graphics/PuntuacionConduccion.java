package com.graphics;

/** Penalizaciones del intento, separadas del cronómetro y su récord. */
public final class PuntuacionConduccion {
    public static final int PENALIZACION_ATROPELLO = 100;
    private int atropellos;

    public void registrarAtropello() { atropellos++; }
    public int atropellos() { return atropellos; }
    public int puntos() { return -atropellos * PENALIZACION_ATROPELLO; }
    public void reiniciar() { atropellos = 0; }
}
