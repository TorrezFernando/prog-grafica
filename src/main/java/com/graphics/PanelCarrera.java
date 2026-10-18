package com.graphics; // Agrupa el panel con la ciudad y su misión.

import java.awt.Color; // Define los colores del texto y del fondo.
import java.awt.Font; // Selecciona una fuente del sistema sin cargar imágenes externas.
import java.awt.FontMetrics; // Mide las palabras para ajustar las filas al ancho disponible.
import java.awt.Graphics2D; // Rasteriza el panel y sus textos en una imagen de Java.
import java.awt.RenderingHints; // Permite suavizar los bordes de los caracteres.
import java.awt.image.BufferedImage; // Conserva los píxeles ARGB antes de enviarlos a OpenGL.
import java.nio.ByteBuffer; // Almacena píxeles en memoria directa para transferirlos a la GPU.
import java.util.ArrayList; // Construye las filas de texto ajustadas al panel.
import java.util.List; // Describe la colección de filas que se van a dibujar.
import org.lwjgl.BufferUtils; // Reserva buffers directos compatibles con LWJGL.
import static org.lwjgl.opengl.GL33.*; // Accede a shaders, texturas, buffers y estados de OpenGL Core.

/** Panel de misión: rasteriza texto con Java y lo dibuja como textura en OpenGL Core. */
final class PanelCarrera {
    private int programa; // Identificador del programa exclusivo para el panel.
    private int vao; // Describe los vértices del rectángulo del panel.
    private int vbo; // Almacena dos triángulos que cubren el rectángulo.
    private int textura; // Identifica la imagen rasterizada del panel en la GPU.
    private int anchoPanel; // Ancho de la imagen del panel en píxeles.
    private int altoPanel; // Altura calculada a partir de las filas de texto.
    private String contenidoAnterior = ""; // Evita regenerar la textura cuando el texto no ha cambiado.

    /** Superpone el texto y restaura todos los estados de OpenGL que utiliza el panel. */
    void dibujar(int ancho, int alto, String... lineas) {
        int programaAnterior = glGetInteger(GL_CURRENT_PROGRAM); // Guarda el programa de la escena para restaurarlo después.
        int vaoAnterior = glGetInteger(GL_VERTEX_ARRAY_BINDING); // Guarda la geometría que estaba enlazada.
        int bufferAnterior = glGetInteger(GL_ARRAY_BUFFER_BINDING); // Conserva el buffer activo de la escena.
        int unidadAnterior = glGetInteger(GL_ACTIVE_TEXTURE); // Conserva la unidad de textura seleccionada.
        boolean profundidad = glIsEnabled(GL_DEPTH_TEST); // Recuerda si estaba activa la prueba de profundidad.
        boolean mezcla = glIsEnabled(GL_BLEND); // Recuerda si estaba activa la transparencia.
        int origenRgb = glGetInteger(GL_BLEND_SRC_RGB); // Guarda el factor de mezcla RGB de origen.
        int destinoRgb = glGetInteger(GL_BLEND_DST_RGB); // Guarda el factor de mezcla RGB de destino.
        int origenAlpha = glGetInteger(GL_BLEND_SRC_ALPHA); // Guarda el factor de mezcla alfa de origen.
        int destinoAlpha = glGetInteger(GL_BLEND_DST_ALPHA); // Guarda el factor de mezcla alfa de destino.
        glActiveTexture(GL_TEXTURE0); // Usa la unidad cero para la imagen del panel.
        int texturaAnterior = glGetInteger(GL_TEXTURE_BINDING_2D); // Guarda la textura que estaba enlazada a esa unidad.
        try { // Garantiza la restauración de los estados incluso si falla el dibujo.
            if (programa == 0) iniciar(); // Crea los recursos solo en el primer dibujo con contexto válido.
            int nuevoAncho = Math.min(540, ancho - 24); // Adapta el panel dejando márgenes dentro de la ventana.
            String contenido = nuevoAncho + String.join("\n", lineas); // Incluye ancho y texto en la comprobación de cambios.
            glBindTexture(GL_TEXTURE_2D, textura); // Selecciona la imagen propia del panel.
            if (!contenido.equals(contenidoAnterior)) { // Actualiza los píxeles solo cuando cambió el contenido o el ancho.
                actualizarTexto(nuevoAncho, lineas); // Rasteriza las filas y transfiere la imagen a la textura.
                contenidoAnterior = contenido; // Recuerda qué versión del panel está en la GPU.
            }
            glDisable(GL_DEPTH_TEST); // Permite superponer el panel sobre la escena 3D.
            glEnable(GL_BLEND); // Activa la transparencia del fondo y de los caracteres.
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Mezcla los píxeles respetando su canal alfa.
            glUseProgram(programa); // Selecciona el programa de dibujo del panel.
            glBindVertexArray(vao); // Selecciona los dos triángulos del panel.
            glUniform4f(glGetUniformLocation(programa, "rect"), // Envía posición y dimensiones en coordenadas normalizadas.
                -1 + 24f / ancho, -1 + 24f / alto, 2f * anchoPanel / ancho, 2f * altoPanel / alto); // Coloca el panel a doce píxeles de los bordes inferiores.
            glUniform1i(glGetUniformLocation(programa, "imagen"), 0); // Indica que el muestreador utiliza la unidad cero.
            glDrawArrays(GL_TRIANGLES, 0, 6); // Dibuja los seis vértices de los dos triángulos.
        } finally { // Recupera los estados usados por la escena principal.
            glUseProgram(programaAnterior); // Restaura el programa previo al panel.
            glBindVertexArray(vaoAnterior); // Restaura la geometría anterior.
            glBindBuffer(GL_ARRAY_BUFFER, bufferAnterior); // Restaura el enlace del buffer de vértices.
            glBindTexture(GL_TEXTURE_2D, texturaAnterior); // Restaura la textura previa de la unidad cero.
            glActiveTexture(unidadAnterior); // Restaura la unidad activa original.
            if (profundidad) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST); // Recupera el estado anterior de profundidad.
            if (mezcla) glEnable(GL_BLEND); else glDisable(GL_BLEND); // Recupera el estado anterior de transparencia.
            glBlendFuncSeparate(origenRgb, destinoRgb, origenAlpha, destinoAlpha); // Recupera los cuatro factores de mezcla originales.
        }
    }

    /** Prepara los shaders, dos triángulos y una textura para dibujar el panel en OpenGL Core. */
    private void iniciar() {
        // Compila la transformación del rectángulo del panel.
        int vertex = shader(GL_VERTEX_SHADER, """
            #version 330 core // Selecciona GLSL compatible con el contexto del proyecto.
            layout(location=0) in vec2 punto; // Lee la posición local de cada vértice.
            uniform vec4 rect; // Recibe posición XY y dimensiones del panel.
            out vec2 uv; // Entrega coordenadas de textura al fragment shader.
            void main() { // Transforma el cuadrado local al rectángulo de pantalla.
                gl_Position = vec4(rect.xy + punto * rect.zw, 0, 1); // Ubica el vértice en coordenadas normalizadas.
                uv = vec2(punto.x, 1.0 - punto.y); // Invierte Y para respetar las filas de la imagen de Java.
            }
            """);
        int fragment = 0; // Permite liberar el fragment shader solo si llegó a crearse.
        try { // Garantiza la liberación de los shaders intermedios aunque falle el enlace.
            // Compila el muestreo del texto rasterizado.
            fragment = shader(GL_FRAGMENT_SHADER, """
                #version 330 core // Utiliza la misma versión que el vertex shader.
                in vec2 uv; // Recibe la coordenada interpolada sobre el panel.
                uniform sampler2D imagen; // Lee la textura RGBA con el texto.
                out vec4 color; // Entrega color y transparencia de cada píxel.
                void main() { color = texture(imagen, uv); } // Copia el píxel de la imagen rasterizada.
                """);
            programa = glCreateProgram(); // Reserva el programa que enlaza ambos shaders.
            glAttachShader(programa, vertex); // Asocia la transformación de vértices.
            glAttachShader(programa, fragment); // Asocia la lectura de la textura de texto.
            glLinkProgram(programa); // Enlaza las etapas en un programa ejecutable.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) { // Comprueba que el enlazado haya terminado correctamente.
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Expone el diagnóstico del enlazador.
            }
        } finally { // Libera los shaders intermedios que el programa ya ha incorporado.
            glDeleteShader(vertex); // Libera el shader intermedio después de enlazarlo.
            if (fragment != 0) glDeleteShader(fragment); // Libera también el fragment shader si fue creado.
        }
        vao = glGenVertexArrays(); // Reserva la descripción de la geometría del panel.
        vbo = glGenBuffers(); // Reserva el buffer de sus vértices.
        glBindVertexArray(vao); // Selecciona los dos triángulos del panel.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Selecciona el buffer que se va a cargar.
        glBufferData(GL_ARRAY_BUFFER, new float[] {0,0, 1,0, 1,1, 0,0, 1,1, 0,1}, GL_STATIC_DRAW); // Define dos triángulos dentro de un cuadrado de cero a uno.
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8, 0); // Cada vértice tiene dos flotantes consecutivos.
        glEnableVertexAttribArray(0); // Activa el atributo de posición para el shader.
        textura = glGenTextures(); // Reserva la textura RGBA del texto.
        glBindTexture(GL_TEXTURE_2D, textura); // Selecciona la imagen propia del panel.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR); // Suaviza la textura al reducirla.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); // Suaviza la textura al ampliarla.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); // Evita repetir píxeles fuera del borde horizontal.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); // Evita repetir píxeles fuera del borde vertical.
    }

    /** Compila una etapa GLSL y devuelve su identificador o el diagnóstico del controlador. */
    private static int shader(int tipo, String codigo) {
        int shader = glCreateShader(tipo); // Reserva el shader de la etapa solicitada.
        glShaderSource(shader, codigo); // Carga el código GLSL en el shader.
        glCompileShader(shader); // Compila el código para la GPU actual.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Comprueba errores de compilación.
            String mensaje = glGetShaderInfoLog(shader); // Recupera el diagnóstico antes de liberar el recurso.
            glDeleteShader(shader); // Libera el shader fallido.
            throw new IllegalStateException(mensaje); // Detiene el dibujo con una explicación del error.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Ajusta palabras al ancho, rasteriza las filas y carga sus píxeles RGBA en la textura. */
    private void actualizarTexto(int ancho, String[] lineas) {
        Font fuente = new Font(Font.SANS_SERIF, Font.BOLD, ancho < 400 ? 12 : 15); // Adapta la fuente al ancho útil del panel.
        BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); // Crea una superficie mínima para medir texto.
        Graphics2D medir = medida.createGraphics(); // Obtiene las métricas del sistema de dibujo de Java.
        medir.setFont(fuente); // Aplica la misma fuente que se utilizará en la textura.
        FontMetrics metricas = medir.getFontMetrics(); // Consulta ancho de palabras y altura de los caracteres.
        List<String> filas = new ArrayList<>(); // Acumula las líneas ya ajustadas al ancho.
        for (String linea : lineas) { // Procesa cada indicador recibido desde clase4.
            String fila = ""; // Comienza una nueva fila vacía.
            for (String palabra : linea.split(" ")) { // Ajusta el texto por palabras para no cortar caracteres.
                String candidata = fila.isEmpty() ? palabra : fila + " " + palabra; // Propone añadir la siguiente palabra.
                if (!fila.isEmpty() && metricas.stringWidth(candidata) > ancho - 24) { // Comprueba si la propuesta supera el ancho interior.
                    filas.add(fila); // Guarda la fila que ya cabe en el panel.
                    fila = palabra; // Comienza la siguiente fila con la palabra pendiente.
                } else fila = candidata; // Acepta la palabra mientras haya espacio.
            }
            filas.add(fila); // Conserva también la última fila de cada indicador.
        }
        int paso = metricas.getHeight() + 4; // Separa filas usando su altura real y cuatro píxeles de margen.
        medir.dispose(); // Libera el contexto utilizado para medir.
        anchoPanel = ancho; // Guarda el ancho de la textura y del rectángulo.
        altoPanel = filas.size() * paso + 16; // Calcula la altura incluyendo márgenes verticales.
        BufferedImage imagen = new BufferedImage(anchoPanel, altoPanel, BufferedImage.TYPE_INT_ARGB); // Reserva los píxeles con transparencia.
        Graphics2D g = imagen.createGraphics(); // Obtiene el contexto para rasterizar el panel completo.
        g.setColor(new Color(9, 18, 30, 235)); // Elige el fondo oscuro parcialmente transparente.
        g.fillRoundRect(0, 0, anchoPanel, altoPanel, 14, 14); // Pinta el fondo con esquinas redondeadas.
        g.setFont(fuente); // Selecciona la fuente ya utilizada al medir.
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Suaviza los bordes del texto.
        for (int i = 0; i < filas.size(); i++) { // Dibuja todas las filas en orden vertical.
            g.setColor(i == 0 ? new Color(255, 203, 66) : new Color(225, 238, 247)); // Resalta el estado de la misión en dorado.
            g.drawString(filas.get(i), 12, 8 + metricas.getAscent() + i * paso); // Ubica la línea base de cada fila dentro del panel.
        }
        g.dispose(); // Libera el contexto de rasterización.
        ByteBuffer datos = BufferUtils.createByteBuffer(anchoPanel * altoPanel * 4); // Reserva cuatro bytes RGBA por píxel.
        for (int y = 0; y < altoPanel; y++) for (int x = 0; x < anchoPanel; x++) { // Recorre la imagen fila por fila.
            int pixel = imagen.getRGB(x, y); // Lee un píxel ARGB de Java.
            datos.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel).put((byte) (pixel >> 24)); // Reordena sus componentes al formato RGBA de OpenGL.
        }
        datos.flip(); // Prepara el buffer para leer desde su primer byte.
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, anchoPanel, altoPanel, 0, GL_RGBA, GL_UNSIGNED_BYTE, datos); // Transfiere la imagen rasterizada a la textura.
    }

    /** Libera los recursos propios antes de que el juego destruya el contexto gráfico. */
    void cerrar() {
        if (textura != 0) glDeleteTextures(textura); // Libera la imagen del panel en la GPU.
        if (vbo != 0) glDeleteBuffers(vbo); // Libera sus vértices.
        if (vao != 0) glDeleteVertexArrays(vao); // Libera la descripción de sus atributos.
        if (programa != 0) glDeleteProgram(programa); // Libera el programa de dibujo del panel.
    }
}
