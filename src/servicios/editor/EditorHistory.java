package servicios.editor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import modelo.editor.CanvasModel;
import modelo.editor.ImageLayer;
import modelo.editor.Layer;
import modelo.editor.LayerModel;
import modelo.editor.TextLayer;

/**
 * Historial de undo/redo del documento del Modo Editor (slot EDITOR).
 * <p>
 * Trabaja con snapshots inmutables del documento (lienzo, capas con imagen
 * clonada y selección). Se suscribe a los cambios del {@link LayerModel} para
 * capturar automáticamente las operaciones estructurales (añadir, borrar,
 * duplicar, ordenar, fusionar, pegar, etc.) y recibe el {@code onNotified()}
 * del {@link EditorDocumentManager} al cerrarse cada gesto de herramienta.
 * <p>
 * Los gestos de arrastre se agrupan en un único paso mediante
 * {@link #beginGesture(String, boolean)}: las mutaciones intermedias caen
 * dentro de la transacción y solo al notificarse el fin del gesto se empuja el
 * paso. Las operaciones de píxel (p. ej. bote de pintura) fuerzan el paso
 * porque mutan la imagen in-place y no son detectables por comparación.
 */
public class EditorHistory {

    /** Número máximo de pasos de undo retenidos en memoria. */
    public static final int MAX_PASOS = 50;

    /** Nombre de los pasos capturados sin una operación explícita. */
    public static final String NOMBRE_PASO_GENERICO = "Modificación";

    private final List<Snapshot> pilaUndo = new ArrayList<>();
    private final List<Snapshot> pilaRedo = new ArrayList<>();
    private Snapshot estadoActual;

    private LayerModel layerModel;
    private CanvasModel canvasModel;
    private Runnable cambioCallback;

    private boolean enTransaccion;
    private boolean transaccionPixeles;
    private String transaccionNombre;
    private boolean aplicando;

    private final Runnable modelChangeListener = this::onModelChanged;


    public EditorHistory(LayerModel layerModel, CanvasModel canvasModel) {
        this.layerModel = layerModel;
        this.canvasModel = canvasModel;
        this.estadoActual = snapshot(null);
        if (layerModel != null) {
            layerModel.addChangeListener(modelChangeListener);
        }
    } // --- Fin del constructor EditorHistory ---


    /**
     * Reasigna los modelos del documento (tras {@code setDocument}) y reinicia
     * el historial, manteniendo el callback de cambio de estado.
     *
     * @param layerModel el nuevo modelo de capas
     * @param canvasModel el nuevo modelo de lienzo
     */
    public void rebind(LayerModel layerModel, CanvasModel canvasModel) {
        if (this.layerModel != null) {
            this.layerModel.removeChangeListener(modelChangeListener);
        }
        this.layerModel = layerModel;
        this.canvasModel = canvasModel;
        if (layerModel != null) {
            layerModel.addChangeListener(modelChangeListener);
        }
        clear();
    } // --- Fin del metodo rebind ---


    /**
     * Registra el callback que se invoca al cambiar el estado del historial
     * (empuje de paso, undo o redo) para que la vista actualice los botones.
     *
     * @param cambioCallback el callback a invocar (puede ser {@code null})
     */
    public void setCambioCallback(Runnable cambioCallback) {
        this.cambioCallback = cambioCallback;
    } // --- Fin del metodo setCambioCallback ---


    /**
     * Reinicia el historial vaciando ambas pilas y fijando el estado base.
     */
    public void clear() {
        pilaUndo.clear();
        pilaRedo.clear();
        estadoActual = snapshot(null);
        notificarCambio();
    } // --- Fin del metodo clear ---


    /**
     * Abre una transacción de gesto. Todas las mutaciones hasta que se notifique
     * el fin del gesto se agrupan en un único paso de undo.
     *
     * @param nombre            nombre descriptivo del paso (p. ej. el de la herramienta)
     * @param esOperacionPixeles {@code true} si la operación muta píxeles in-place
     *                          (no detectable por comparación de campos)
     */
    public void beginGesture(String nombre, boolean esOperacionPixeles) {
        enTransaccion = true;
        transaccionNombre = nombre;
        transaccionPixeles = esOperacionPixeles;
    } // --- Fin del metodo beginGesture ---


    /**
     * Cierra manualmente una transacción abierta con {@link #beginGesture}
     * empujando el paso (si el contenido cambió realmente). Útil cuando la
     * operación no genera ninguna notificación del modelo (p. ej. cambios de
     * opacidad desde un slider).
     */
    public void endGesture() {
        if (!enTransaccion) return;
        String nombre = (transaccionNombre != null) ? transaccionNombre : NOMBRE_PASO_GENERICO;
        enTransaccion = false;
        Snapshot nuevo = snapshot(nombre);
        if (transaccionPixeles || !nuevo.mismoContenido(estadoActual)) {
            empujar(nombre, nuevo);
        } else {
            estadoActual = nuevo;
        }
    } // --- Fin del metodo endGesture ---


    /**
     * Ejecuta una operación discreta registrándola como un paso de undo (si el
     * contenido del documento cambia realmente).
     *
     * @param nombre    nombre descriptivo del paso
     * @param operacion la operación a ejecutar
     */
    public void record(String nombre, Runnable operacion) {
        if (enTransaccion) {
            operacion.run();
            return;
        }
        enTransaccion = true;
        try {
            operacion.run();
        } finally {
            enTransaccion = false;
        }
        Snapshot nuevo = snapshot(nombre);
        if (!nuevo.mismoContenido(estadoActual)) {
            empujar(nombre, nuevo);
        } else {
            estadoActual = nuevo;
        }
    } // --- Fin del metodo record ---


    /**
     * Notifica al historial de un cambio del documento. Cierra la transacción de
     * gesto abierta o, si no la hay, intenta una captura automática. Lo invoca
     * {@link EditorDocumentManager#notificarModificacion()}.
     */
    public void onNotified() {
        if (aplicando) return;
        if (enTransaccion) {
            String nombre = (transaccionNombre != null) ? transaccionNombre : NOMBRE_PASO_GENERICO;
            enTransaccion = false;
            Snapshot nuevo = snapshot(nombre);
            if (transaccionPixeles || !nuevo.mismoContenido(estadoActual)) {
                empujar(nombre, nuevo);
            } else {
                estadoActual = nuevo;
            }
        } else {
            autoCommit(NOMBRE_PASO_GENERICO);
        }
    } // --- Fin del metodo onNotified ---


    public boolean canUndo() {
        return !pilaUndo.isEmpty();
    } // --- Fin del metodo canUndo ---


    public boolean canRedo() {
        return !pilaRedo.isEmpty();
    } // --- Fin del metodo canRedo ---


    /**
     * Deshace el último paso restaurando el estado anterior del documento.
     */
    public void undo() {
        if (pilaUndo.isEmpty()) return;
        Snapshot previo = pilaUndo.remove(pilaUndo.size() - 1);
        pilaRedo.add(estadoActual);
        estadoActual = previo;
        aplicar(previo);
        notificarCambio();
    } // --- Fin del metodo undo ---


    /**
     * Rehace el siguiente paso restaurado tras un undo.
     */
    public void redo() {
        if (pilaRedo.isEmpty()) return;
        Snapshot siguiente = pilaRedo.remove(pilaRedo.size() - 1);
        pilaUndo.add(estadoActual);
        estadoActual = siguiente;
        aplicar(siguiente);
        notificarCambio();
    } // --- Fin del metodo redo ---


    /**
     * Devuelve los nombres de los pasos en orden cronológico (desde el más
     * antiguo hasta el más reciente, incluyendo los pasos rehacibles), para el
     * panel de historial.
     *
     * @return lista de nombres de pasos
     */
    public List<String> listaPasos() {
        List<String> pasos = new ArrayList<>();
        for (Snapshot s : pilaUndo) {
            pasos.add(s.nombre != null ? s.nombre : "Inicio");
        }
        pasos.add(estadoActual != null && estadoActual.nombre != null ? estadoActual.nombre : "Inicio");
        for (int i = pilaRedo.size() - 1; i >= 0; i--) {
            Snapshot s = pilaRedo.get(i);
            pasos.add(s.nombre != null ? s.nombre : "Inicio");
        }
        return pasos;
    } // --- Fin del metodo listaPasos ---


    /**
     * Índice (en {@link #listaPasos()}) del estado actual del documento.
     *
     * @return posición del estado actual dentro de la lista de pasos
     */
    public int indiceEstadoActual() {
        return pilaUndo.size();
    } // --- Fin del metodo indiceEstadoActual ---


    /**
     * Salta al paso indicado (según {@link #listaPasos()}) deshaciendo o
     * rehaciendo cuantos pasos sean necesarios.
     *
     * @param indice índice del paso de destino
     */
    public void saltarAPaso(int indice) {
        int actual = indiceEstadoActual();
        while (indice < actual) {
            undo();
            actual--;
        }
        while (indice > actual) {
            redo();
            actual++;
        }
    } // --- Fin del metodo saltarAPaso ---


    // ==================== INTERNO ====================


    private void onModelChanged() {
        if (enTransaccion || aplicando) return;
        autoCommit(NOMBRE_PASO_GENERICO);
    } // --- Fin del metodo onModelChanged ---


    private void autoCommit(String nombre) {
        Snapshot nuevo = snapshot(nombre);
        if (!nuevo.mismoContenido(estadoActual)) {
            empujar(nombre, nuevo);
        } else {
            estadoActual = nuevo;
        }
    } // --- Fin del metodo autoCommit ---


    private void empujar(String nombre, Snapshot nuevo) {
        pilaUndo.add(estadoActual);
        estadoActual = nuevo;
        pilaRedo.clear();
        while (pilaUndo.size() > MAX_PASOS) {
            pilaUndo.remove(0);
        }
        notificarCambio();
    } // --- Fin del metodo empujar ---


    private void aplicar(Snapshot s) {
        aplicando = true;
        try {
            if (canvasModel != null) {
                canvasModel.setSize(s.canvasW, s.canvasH);
                canvasModel.setBackgroundColor(s.canvasBg);
                canvasModel.setTransparent(s.canvasTransparente);
            }
            if (layerModel != null) {
                List<Layer> capas = new ArrayList<>(s.capas.size());
                for (Layer l : s.capas) {
                    capas.add(clonarCapa(l));
                }
                layerModel.restaurarEstado(capas, s.seleccion, s.activa);
            }
        } finally {
            aplicando = false;
        }
    } // --- Fin del metodo aplicar ---


    private Snapshot snapshot(String nombre) {
        List<Layer> capas = new ArrayList<>();
        Set<Integer> seleccion = new LinkedHashSet<>();
        int activa = -1;
        if (layerModel != null) {
            for (Layer l : layerModel.getLayers()) {
                capas.add(clonarCapa(l));
            }
            seleccion.addAll(layerModel.getSelectedIndices());
            activa = layerModel.getActiveIndex();
        }
        return new Snapshot(nombre,
                canvasModel != null ? canvasModel.getWidth() : 0,
                canvasModel != null ? canvasModel.getHeight() : 0,
                canvasModel != null ? canvasModel.getBackgroundColor() : null,
                canvasModel != null && canvasModel.isTransparent(),
                capas, seleccion, activa);
    } // --- Fin del metodo snapshot ---


    private Layer clonarCapa(Layer l) {
        if (l instanceof ImageLayer il) {
            return il.copiaParaHistorial();
        }
        if (l instanceof TextLayer tl) {
            return tl.copiaParaHistorial();
        }
        return l.copy();
    } // --- Fin del metodo clonarCapa ---


    private void notificarCambio() {
        if (cambioCallback != null) {
            cambioCallback.run();
        }
    } // --- Fin del metodo notificarCambio ---


    /** Estado inmutable del documento capturado para el historial. */
    private static final class Snapshot {

        final String nombre;
        final int canvasW;
        final int canvasH;
        final Color canvasBg;
        final boolean canvasTransparente;
        final List<Layer> capas;
        final Set<Integer> seleccion;
        final int activa;

        Snapshot(String nombre, int canvasW, int canvasH, Color canvasBg,
                boolean canvasTransparente, List<Layer> capas,
                Set<Integer> seleccion, int activa) {
            this.nombre = nombre;
            this.canvasW = canvasW;
            this.canvasH = canvasH;
            this.canvasBg = canvasBg;
            this.canvasTransparente = canvasTransparente;
            this.capas = capas;
            this.seleccion = seleccion;
            this.activa = activa;
        } // --- Fin del constructor Snapshot ---


        /**
         * Compara el contenido (lienzo y capas) con otro snapshot, ignorando la
         * selección. La selección se guarda para restaurarla pero no cuenta a la
         * hora de decidir si una operación crea un paso.
         */
        boolean mismoContenido(Snapshot o) {
            if (o == null) return false;
            if (canvasW != o.canvasW || canvasH != o.canvasH
                    || canvasTransparente != o.canvasTransparente
                    || !Objects.equals(canvasBg, o.canvasBg)) {
                return false;
            }
            if (capas.size() != o.capas.size()) return false;
            for (int i = 0; i < capas.size(); i++) {
                if (!capas.get(i).mismoEstado(o.capas.get(i))) return false;
            }
            return true;
        } // --- Fin del metodo mismoContenido ---

    } // --- Fin de la clase Snapshot ---

} // --- Fin de la clase EditorHistory ---
