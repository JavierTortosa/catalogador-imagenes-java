package servicios.editor;

import java.util.List;

/**
 * DTO ra\u00EDz del documento .edoc que se serializa con Gson.
 * <p>
 * Recoge la versi\u00F3n del esquema, la configuraci\u00F3n del lienzo y la
 * lista de capas. Todos los campos son tipos simples o {@code String} para que
 * Gson pueda serializarlos sin adaptadores especiales.
 */
public final class EditorDoc {

    /** Versi\u00F3n actual del esquema del documento. */
    public static final int VERSION_ACTUAL = 1;

    /** Versi\u00F3n del esquema que produjo este documento. */
    public int version;

    /** Configuraci\u00F3n del lienzo. */
    public CanvasDTO canvas;

    /** Capas del documento en orden de fondo a frente. */
    public List<LayerDTO> layers;

    /** DTO del lienzo. */
    public static final class CanvasDTO {

        public int width;
        public int height;
        /** Color de fondo como int ARGB (0 si el lienzo es transparente). */
        public int backgroundColorArgb;
        public boolean transparent;

    } // --- Fin de la clase CanvasDTO ---

    /**
     * DTO de una capa. El campo {@link #type} discrimina entre IMAGE, SHAPE y
     * TEXT; el resto de campos de forma y texto solo son significativos para su
     * tipo respectivo.
     */
    public static final class LayerDTO {

        public String type;
        public String id;
        public String name;

        // Transformaci\u00F3n y estado com\u00FAn
        public int boundsX;
        public int boundsY;
        public int boundsW;
        public int boundsH;
        public double rotation;
        public boolean visible;
        public boolean locked;
        public float opacity;

        // Com\u00FAn a ImageLayer: origen de imagen (null si se guarda como PNG id)
        public String srcPath;

        // Metadatos de forma (solo LayerType.SHAPE)
        public String shapeType;
        public Integer shapeFillArgb;
        public Integer shapeStrokeArgb;
        public Float shapeStrokeWidth;
        public Integer shapeRenderW;
        public Integer shapeRenderH;

        // Metadatos de texto (solo LayerType.TEXT)
        public String text;
        public String fontFamily;
        public Integer fontStyle;
        public Integer fontSize;
        public Integer textColorArgb;
        public Integer alignment;
        public Boolean vertical;
        public Float lineSpacing;
        public Boolean autoSize;
        public Boolean underline;
        public Boolean strikethrough;
        public Boolean flowColumns;
        public List<TextRunDTO> runs;

    } // --- Fin de la clase LayerDTO ---

    /** DTO de un segmento de texto con estilo (rich text). */
    public static final class TextRunDTO {

        public String fontFamily;
        public Integer fontStyle;
        public Integer fontSize;
        public Integer colorArgb;
        public Boolean underline;
        public Boolean strikethrough;
        public String text;

    } // --- Fin de la clase TextRunDTO ---

} // --- Fin de la clase EditorDoc ---