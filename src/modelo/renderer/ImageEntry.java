package modelo.renderer;

public record ImageEntry(String filename, long sizeBytes) {

    @Override
    public String toString() {
        String size;
        if (sizeBytes < 1024) size = sizeBytes + " B";
        else if (sizeBytes < 1024 * 1024) size = String.format("%.1f KB", sizeBytes / 1024.0);
        else size = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        return filename + "  [" + size + "]";
    }

} // --- Fin de la clase ImageEntry ---
