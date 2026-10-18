package com.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/** Verificación optativa con contexto OpenGL real; no forma parte de mvn test. */
public class VerificacionGrafica extends clase4 {
    private int cuadro;
    private boolean conduciendo;
    private boolean probarDerrape;
    private boolean retrocediendo; // Simula reversa para comprobar un choque exclusivo de la chata.
    private BufferedImage nocheInicio;
    private BufferedImage farosEncendidos;
    private BufferedImage mapaVisible;
    private final Path salida = Path.of("target", "verificacion-grafica");

    @Override
    protected boolean pulsada(int key) {
        return (conduciendo && key == GLFW_KEY_W) || (retrocediendo && key == GLFW_KEY_S) ||
            (probarDerrape && (key == GLFW_KEY_A || key == GLFW_KEY_SPACE)); // Conserva controles reales durante las capturas.
    }

    @Override
    protected void actualizar(float deltaTime) {
        cuadro++;
        switch (cuadro) {
            case 5 -> tecla(GLFW_KEY_N);
            case 9 -> tecla(GLFW_KEY_C);
            case 13 -> {
                tecla(GLFW_KEY_N);
                tiempoAnimacion = 5; // Captura también la fase verde.
            }
            case 17 -> {
                tecla(GLFW_KEY_C);
                colocarJugador(-50, 30, -(float) Math.PI / 2); // Coloca también la chata al preparar la calle nueva.
                tiempoAnimacion = 10; // Comprueba el dibujo de la fase amarilla.
            }
            case 21 -> tecla(GLFW_KEY_F);
            case 25 -> tecla(GLFW_KEY_M);
            case 29 -> {
                tecla(GLFW_KEY_M);
                tecla(GLFW_KEY_C);
                tecla(GLFW_KEY_N);
                glfwSetWindowSize(ventana, 480, 800);
            }
            case 33 -> glfwSetWindowSize(ventana, 1280, 600);
            case 37 -> glfwSetWindowSize(ventana, 40, 40);
            case 41 -> glfwSetWindowSize(ventana, 1100, 760);
            case 42 -> glfwIconifyWindow(ventana);
            case 45 -> glfwRestoreWindow(ventana);
            case 46 -> {
                tecla(GLFW_KEY_C);
                tecla(GLFW_KEY_F);
                tecla(GLFW_KEY_R);
                exigir(autoX == INICIO_X && autoZ == INICIO_Z && velocidad == 0 && angulo == 0,
                    "El reinicio no recuperó el estado inicial");
            }
            case 51 -> conduciendo = true;
            case 55 -> recorrerMision();
            case 61 -> tecla(GLFW_KEY_R);
            case 65 -> {
                conduciendo = true;
                tecla(GLFW_KEY_LEFT_SHIFT);
            }
            case 70 -> {
                tecla(GLFW_KEY_R);
                colocarJugador(-30, -30, 0); // Sitúa el conjunto completo antes de demostrar el derrape.
                velocidad = 11;
                probarDerrape = true;
            }
            case 86 -> {
                probarDerrape = false;
                conduciendo = false;
                turboRestante = 0.01; // Verifica el indicador inmediatamente después de terminar el impulso.
            }
            case 91 -> {
                tecla(GLFW_KEY_R); // Recupera la distribución original de los tres conductores.
                colocarJugador(-9, -6, 0); // Observa al NPC con la chata enganchada al jugador.
                noche = false;
                conduciendo = false;
                probarDerrape = false;
            }
            case 95 -> {
                tecla(GLFW_KEY_R);
                for (int i = 0; i < 100; i++) peatones.get(0).actualizar(0.05f, p -> true);
                colocarJugador(10, -11.5f, 0); // Conserva la unión al preparar el encuentro con el peatón.
                noche = false;
                camaraAerea = false;
            }
            case 99 -> {
                conduciendo = true;
                velocidad = 16;
                for (int i = 0; i < 30; i++) super.actualizar(1f / 60f);
                conduciendo = false;
            }
            case 103 -> tecla(GLFW_KEY_R);
            case 107 -> {
                tecla(GLFW_KEY_R); // Prepara una captura dedicada a la articulación de la chata.
                colocarJugador(-30, -30, 0);
                velocidad = 10;
                conduciendo = true;
                probarDerrape = true;
                activarTurbo();
                for (int i = 0; i < 18; i++) super.actualizar(1f / 60); // Usa la física real de derrape con turbo.
                conduciendo = false;
                probarDerrape = false;
                velocidad = 0; // Conserva la articulación para inspeccionar visualmente enganche y plataforma.
                faros = false; // Permite apreciar los colores propios de la chata.
            }
            case 111 -> {
                tecla(GLFW_KEY_R);
                colocarJugador(-31.6f, 3, 0); // La barrera queda detrás de la chata y lejos del tractor.
                retrocediendo = true;
                for (int i = 0; i < 120; i++) super.actualizar(1f / 60); // Retrocede hasta que la chata toca la barrera.
                retrocediendo = false;
            }
            case 115 -> {
                tecla(GLFW_KEY_R); // Demuestra el restablecimiento de pose, enganche y orientación.
                camaraAerea = true;
            }
            case 119 -> glfwSetWindowShouldClose(ventana, true);
            default -> { }
        }
        super.actualizar(1f / 60f);
        if (cuadro >= 20 && cuadro <= 28) tiempoAnimacion = 10; // Congela la baliza al comparar el mismo cuadro con/sin mapa.
        if (cuadro >= 20 && cuadro <= 28) for (VehiculoNpc npc : trafico) npc.reiniciar(); // Compara también el tráfico en posiciones idénticas.
        if (cuadro >= 20 && cuadro <= 28) for (PeatonNpc peaton : peatones) peaton.reiniciar();
    }

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        exigir(viewport[0] == 0 && viewport[1] == 0 && viewport[2] == ancho && viewport[3] == alto,
            "El minimapa no restauró el viewport");
        exigir(!glIsEnabled(GL_SCISSOR_TEST) && !vistaMapa, "El minimapa dejó un estado de dibujo activo");
        exigir(glGetUniformi(programa, uniform("uMapa")) == 0, "Proyección principal sin restaurar");
        switch (cuadro) {
            case 4 -> nocheInicio = capturar("01-seguimiento-noche");
            case 8 -> exigir(diferencias(nocheInicio, capturar("02-seguimiento-dia"), false) > 1000,
                "El cambio de iluminación no afecta superficies visibles");
            case 12 -> capturar("03-ciudad-completa-dia");
            case 16 -> capturar("04-ciudad-completa-noche");
            case 20 -> farosEncendidos = capturar("05-barrio-nuevo-faros-on");
            case 24 -> {
                mapaVisible = capturar("06-barrio-nuevo-faros-off");
                exigir(diferencias(farosEncendidos, mapaVisible, false) > 50,
                    "Los faros no iluminan superficies visibles");
            }
            case 28 -> exigir(diferencias(mapaVisible, capturar("07-minimapa-oculto"), true) == 0,
                "El minimapa altera píxeles fuera de su recuadro");
            case 32 -> capturar("08-ventana-vertical");
            case 36 -> capturar("09-ventana-panoramica");
            case 50 -> capturar("10-restauracion-y-reinicio");
            case 54 -> {
                exigir(mision.estado() == Contrarreloj.Estado.EN_CURSO, "La misión no arrancó al moverse");
                capturar("11-carrera-en-curso");
            }
            case 58 -> {
                exigir(mision.estado() == Contrarreloj.Estado.COMPLETADO, "La meta no se activó");
                capturar("12-meta-alcanzada");
            }
            case 64 -> {
                exigir(mision.estado() == Contrarreloj.Estado.LISTO && Double.isFinite(mision.mejor()),
                    "El reinicio perdió la mejor marca");
                capturar("13-nuevo-intento-con-record");
            }
            case 68 -> {
                exigir(turboRestante > 0 && velocidad > 0, "El impulso no se activó");
                capturar("14-turbo-activo");
            }
            case 84 -> {
                exigir(derrapando && Math.abs(desfaseDerrape) > 0.1f, "El derrape no conserva inercia lateral");
                capturar("15-derrape-y-marcas");
            }
            case 90 -> {
                exigir(turboRestante == 0 && turboEspera > 0, "Falta el indicador de cooldown");
                capturar("16-turbo-en-cooldown");
            }
            case 92 -> {
                exigir(trafico.length == 3 && trafico[2].velocidad() > 0, "El tráfico no está activo");
                capturar("17-trafico-npc"); // Registra un NPC en la vista principal y los tres indicadores del minimapa.
            }
            case 96 -> capturar("18-cebras-y-peatones");
            case 100 -> {
                exigir(puntuacion.puntos() == -100 && puntuacion.atropellos() == 1, "El contacto no registró una única penalización");
                capturar("19-penalizacion-atropello");
            }
            case 104 -> {
                exigir(puntuacion.puntos() == 0 && peatones.stream().noneMatch(PeatonNpc::impactado), "R no reinició peatones y puntos");
                capturar("20-reinicio-peatones");
            }
            case 108 -> {
                exigir(Math.abs(chata.posicion().angulo() - angulo) > 0.15f, "La chata no se articula durante el derrape");
                capturar("21-chata-articulada"); // Muestra la plataforma, las ruedas y la lanza en una curva.
            }
            case 112 -> {
                exigir(velocidad == 0 && autoZ > 3.1f && autoZ < 3.71f, "La chata no detuvo al tractor ante la barrera");
                exigir(puedeCircularSinTrafico(autoX, autoZ, angulo), "El contacto debía afectar solo al remolque");
                capturar("22-chata-bloqueada-por-barrera"); // Evidencia el contacto trasero y el espacio libre del tractor.
            }
            case 116 -> {
                exigir(chata.posicion().angulo() == 0 && autoX == INICIO_X && autoZ == INICIO_Z, "R no restauró el conjunto");
                capturar("23-chata-reinicio-y-minimapa"); // Registra la chata y los tres NPC sin remolque.
            }
            default -> { }
        }
        exigir(glGetError() == GL_NO_ERROR, "Error OpenGL durante la verificación");
    }

    /** Recorre calles con la física real; cambia la orientación en intersecciones despejadas. */
    private void recorrerMision() {
        tecla(GLFW_KEY_R);
        conduciendo = true;
        colocarJugador(autoX, autoZ, -(float) Math.PI / 2); // Alinea el conjunto al preparar este tramo automatizado.
        int pasos = 0;
        while (autoX < -28 && pasos++ < 600) super.actualizar(1f / 60f);
        exigir(autoX >= -28, "No alcanza el carril libre");
        velocidad = 0;
        colocarJugador(autoX, autoZ, 0); // Las maniobras de giro real se verifican en ChataRemolqueTest.
        pasos = 0;
        while (autoZ > -50 && pasos++ < 1200) super.actualizar(1f / 60f);
        exigir(autoZ <= -50, "La calle hacia el norte está bloqueada");
        velocidad = 0;
        colocarJugador(autoX, autoZ, -(float) Math.PI / 2); // Prepara el último tramo con el enganche alineado.
        pasos = 0;
        while (mision.estado() != Contrarreloj.Estado.COMPLETADO && pasos++ < 1200) super.actualizar(1f / 60f);
        exigir(mision.estado() == Contrarreloj.Estado.COMPLETADO, "No pudo completar el recorrido");
        conduciendo = false;
        velocidad = 0;
    }

    private BufferedImage capturar(String nombre) {
        ByteBuffer pixeles = BufferUtils.createByteBuffer(ancho * alto * 4);
        glReadBuffer(GL_BACK);
        glReadPixels(0, 0, ancho, alto, GL_RGBA, GL_UNSIGNED_BYTE, pixeles);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int indice = (y * ancho + x) * 4;
                int rgb = ((pixeles.get(indice) & 255) << 16) |
                    ((pixeles.get(indice + 1) & 255) << 8) | (pixeles.get(indice + 2) & 255);
                imagen.setRGB(x, alto - 1 - y, rgb);
            }
        }
        try {
            Files.createDirectories(salida);
            ImageIO.write(imagen, "png", salida.resolve(nombre + ".png").toFile());
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo guardar la captura", error);
        }
        System.out.println("Captura: " + nombre + " (" + ancho + "x" + alto + ")");
        return imagen;
    }

    private int diferencias(BufferedImage a, BufferedImage b, boolean excluirMapa) {
        exigir(a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight(), "Capturas de tamaños distintos");
        int lado = Math.min(260, Math.min(ancho, alto) / 3);
        int margen = Math.min(18, Math.min(ancho, alto) / 20);
        int diferencias = 0;
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (excluirMapa && x >= ancho - lado - margen - 3 && y <= margen + lado + 3) {
                    continue;
                }
                if (a.getRGB(x, y) != b.getRGB(x, y)) diferencias++;
            }
        }
        return diferencias;
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalStateException(mensaje);
    }

    public static void main(String[] args) {
        System.setProperty("demo.frames", "3");
        new clase1().run();
        new clase2().run();
        new clase3().run();
        System.setProperty("demo.frames", "130"); // Incluye las capturas de la chata antes de cerrar automáticamente.
        VerificacionGrafica verificacion = new VerificacionGrafica(); // Conserva el contador para detectar un cierre prematuro de la ventana.
        verificacion.run(); // Ejecuta la secuencia completa, incluidas las maniobras de la chata.
        exigir(verificacion.cuadro >= 119, "La ventana se cerró antes de terminar la verificación gráfica"); // No informa éxito si falta una captura final.
        System.out.println("Verificación gráfica completada: cuatro etapas, controles, peatones, chata y 23 capturas.");
    }
}
