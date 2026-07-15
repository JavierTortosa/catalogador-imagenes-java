package servicios.renderer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import modelo.renderer.Triangle;

/**
 * Parser de archivos STL en formato binario y ASCII.
 * STL (STereoLithography) es un formato de malla de triángulos
 * que no ha cambiado desde 1987.
 * <p>
 * Detecta automáticamente el formato: busca "facet normal" en los primeros
 * 8 KB para identificar ASCII, de lo contrario asume binario.
 */
public class StlParser {

    private static final long MAX_FILE_SIZE = 500L * 1024 * 1024;
    private static final int ASCII_PROBE_SIZE = 8192;

    public static List<Triangle> parse(File file) throws IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("Archivo STL demasiado grande (> 500 MB): " + file.getName());
        }
        if (isAscii(file)) {
            return parseAscii(file);
        } else {
            return parseBinary(file);
        }
    }

    /**
     * Detecta si el archivo es STL ASCII buscando "facet normal" en los primeros 8 KB.
     */
    private static boolean isAscii(File file) throws IOException {
        byte[] probe = new byte[ASCII_PROBE_SIZE];
        int read;
        try (FileInputStream fis = new FileInputStream(file)) {
            read = fis.read(probe);
        }
        if (read <= 0) return false;
        String header = new String(probe, 0, read, StandardCharsets.US_ASCII).toLowerCase();
        return header.contains("facet normal");
    }

    /**
     * Parsea STL ASCII línea por línea (sin cargar todo en memoria).
     */
    private static List<Triangle> parseAscii(File file) throws IOException {
        List<Triangle> triangles = new ArrayList<>();
        float nx = 0, ny = 0, nz = 0;
        float[][] verts = new float[3][];
        int vertexCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.US_ASCII), 65536)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim().toLowerCase();
                if (trimmed.isEmpty()) continue;

                if (trimmed.startsWith("facet normal")) {
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length >= 5) {
                        nx = parseFloat(parts[2]);
                        ny = parseFloat(parts[3]);
                        nz = parseFloat(parts[4]);
                    }
                    vertexCount = 0;
                    verts[0] = verts[1] = verts[2] = null;
                } else if (trimmed.startsWith("vertex")) {
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length >= 4 && vertexCount < 3) {
                        float x = parseFloat(parts[1]);
                        float y = parseFloat(parts[2]);
                        float z = parseFloat(parts[3]);
                        verts[vertexCount] = new float[]{x, y, z};
                        vertexCount++;
                    }
                    if (vertexCount == 3) {
                        triangles.add(new Triangle(verts[0], verts[1], verts[2], nx, ny, nz));
                        vertexCount = 0;
                        verts[0] = verts[1] = verts[2] = null;
                    }
                }
            }
        }
        if (triangles.isEmpty()) {
            throw new IOException("No se encontraron triángulos en el STL ASCII: " + file.getName());
        }
        return triangles;
    }

    /**
     * Parsea STL binario cargando todo el archivo (necesario por acceso aleatorio).
     */
    private static List<Triangle> parseBinary(File file) throws IOException {
        byte[] data;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            data = in.readAllBytes();
        }
        if (data.length < 84) {
            throw new IOException("STL binario demasiado corto: " + data.length + " bytes");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buf.position(80);
        int triangleCount = buf.getInt();
        if (triangleCount <= 0 || triangleCount > 10_000_000) {
            throw new IOException("Número de triángulos inválido: " + triangleCount);
        }
        int expectedSize = 84 + triangleCount * 50;
        if (data.length < expectedSize) {
            throw new IOException("STL binario truncado: esperados " + expectedSize + " bytes, recibidos " + data.length);
        }
        List<Triangle> triangles = new ArrayList<>(triangleCount);
        for (int i = 0; i < triangleCount; i++) {
            float nx = buf.getFloat();
            float ny = buf.getFloat();
            float nz = buf.getFloat();
            float v0x = buf.getFloat();
            float v0y = buf.getFloat();
            float v0z = buf.getFloat();
            float v1x = buf.getFloat();
            float v1y = buf.getFloat();
            float v1z = buf.getFloat();
            float v2x = buf.getFloat();
            float v2y = buf.getFloat();
            float v2z = buf.getFloat();
            buf.getShort();
            triangles.add(new Triangle(
                    new float[]{v0x, v0y, v0z},
                    new float[]{v1x, v1y, v1z},
                    new float[]{v2x, v2y, v2z},
                    nx, ny, nz
            ));
        }
        return triangles;
    }

    private static float parseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

} // --- Fin de la clase StlParser ---
