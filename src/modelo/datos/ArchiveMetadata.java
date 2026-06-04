package modelo.datos;

public class ArchiveMetadata {
    public String archivePath;
    public int stlCount;
    public int supportedStlCount;
    public int unsupportedStlCount;
    public boolean isMultipart;
    public boolean hasLychee;
    public boolean hasChitubox;
    public double totalSizeMb;
    public long analysisDate;

    public boolean hasSupports() {
        return supportedStlCount > 0 || hasLychee || hasChitubox;
    }
}