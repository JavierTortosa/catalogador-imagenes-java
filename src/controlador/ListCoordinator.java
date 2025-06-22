package controlador; 

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList; // Necesario para sincronizarListaUI
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities; // Para invokeLater en sincronizarListaUI

import controlador.commands.AppActionCommands;
import controlador.interfaces.ContextSensitiveAction;
import controlador.managers.interfaces.IListCoordinator;
import controlador.utils.ComponentRegistry;
import modelo.VisorModel;
import servicios.ConfigKeys;
import vista.VisorView;

public class ListCoordinator implements IListCoordinator{

    // --- Referencias a otros componentes ---
	private VisorModel model;
	private VisorView view;
	private VisorController controller; // Para delegar carga de imagen
	private ComponentRegistry registry;
    private List<ContextSensitiveAction> contextSensitiveActions = Collections.emptyList();
    
    // --- Estado Interno ---
    private int indiceOficialSeleccionado = -1; // Índice "maestro" gestionado aquí
    private boolean sincronizandoUI = false; // Flag para evitar bucles al sincronizar

    //TODO se podria especificar en el config.cfg
    // Define el tamaño del salto para Page Up/Down (ajustable)
//    private static final int PAGE_SCROLL_INCREMENT = 10; // Salta 10 ítems, por ejemplo
    private int pageScrollIncrement;
    
    /**
     * Constructor.
     * @param model El modelo de datos.
     * @param view La vista principal.
     * @param controller El controlador principal.
     */
    public ListCoordinator() {
        this.pageScrollIncrement = 10; // Valor por defecto
        System.out.println("[ListCoordinator] Instancia creada con constructor vacío.");
    } //--- FIN ListCoordinator (constructor) ---
    
    

    /**
     * Método principal para seleccionar una imagen por su índice en el modelo principal.
     * Este método centraliza la lógica de actualización y sincronización.
     *
     * @param indiceDeseado El índice (0-based) en model.getModeloLista() que se quiere seleccionar.
     */
    public synchronized void seleccionarImagenPorIndice(int indiceDeseado) {
        System.out.println("  [ListCoordinator] Solicitud para seleccionar índice: " + indiceDeseado
                         + " (Oficial actual: " + this.indiceOficialSeleccionado + ")");

        // 1. Validar Índice Deseado
        if (indiceDeseado < -1 || (indiceDeseado >= 0 && indiceDeseado >= model.getModeloLista().getSize())) {
            System.err.println("    -> Índice inválido. Ignorando selección.");
            return; // Índice fuera de rango
        }

        // 2. Evitar Trabajo Redundante/Bucles
        if (indiceDeseado == this.indiceOficialSeleccionado) {
            System.out.println("    -> Índice deseado ya es el oficial. No se hace nada.");
            asegurarVisibilidadAmbasListasSiVisibles(indiceDeseado);
            return;
        }
        
        if (sincronizandoUI) {
            System.out.println("    -> Ignorando selección (sincronizandoUI=true).");
            return;
        }

        // 4. Actualizar Estado Oficial
        System.out.println("    -> Actualizando índice oficial a: " + indiceDeseado);
        this.indiceOficialSeleccionado = indiceDeseado;

        // 5. Actualizar Modelo (Clave seleccionada) y Cargar Imagen Principal (Delegado al Controller)
        if (indiceDeseado != -1) {
             String claveSeleccionada = model.getModeloLista().getElementAt(indiceDeseado);
             model.setSelectedImageKey(claveSeleccionada);
             controller.actualizarImagenPrincipal(indiceDeseado);
        } else {
             model.setSelectedImageKey(null);
             controller.limpiarUI();
        }

        // 6. Sincronizar UI de las Listas (Selección y Visibilidad)
        
        // <<< CAMBIO: Obtener la lista de nombres desde el registro >>>
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        sincronizarListaUI(listaNombres, indiceDeseado);

        // Actualizar la vista de miniaturas
        if (indiceDeseado != -1) {
             this.actualizarModeloYVistaMiniaturas(indiceDeseado);
        } else {
             // Si deseleccionamos, limpiar también el modelo de miniaturas
             if (controller.getModeloMiniaturas() != null) {
                 controller.getModeloMiniaturas().clear();
             }
             // <<< CAMBIO: Obtener la lista de miniaturas desde el registro para repintar >>>
             JList<String> listaMiniaturas = registry.get("list.miniaturas");
             if (listaMiniaturas != null) {
                 listaMiniaturas.repaint();
             }
        }
        
        System.out.println("  [ListCoordinator] Selección procesada para índice: " + indiceDeseado);
    } // --- FIN seleccionarImagenPorIndice


    /**
     * Selecciona la imagen anterior en la lista principal.
     * El comportamiento en los extremos (primera imagen) depende del estado
     * de 'navegacionCircularActivada' en el VisorModel.
     * Actualiza el estado interno, la UI (incluyendo la imagen principal y miniaturas)
     * y el estado de los botones de navegación.
     */
    public synchronized void seleccionarAnterior() {
        // --- 1. LOG DE INICIO Y VALIDACIÓN DE DEPENDENCIAS ---
        System.out.println(">>> Coordinator: Solicitud de Navegación -> ANTERIOR (Índice Oficial Actual: " + this.indiceOficialSeleccionado + ")");
        if (model == null || model.getModeloLista() == null) {
            System.err.println("    ERROR CRÍTICO [seleccionarAnterior]: VisorModel o su lista interna son nulos. No se puede navegar.");
            return;
        }

        // --- 2. OBTENER ESTADO ACTUAL Y TAMAÑO DE LA LISTA ---
        final DefaultListModel<String> modeloPrincipal = model.getModeloLista();
        final int totalImagenes = modeloPrincipal.getSize();

        if (totalImagenes == 0) {
            System.out.println("    -> Lista de imágenes vacía. No hay nada a qué navegar.");
            // No es necesario llamar a seleccionarIndiceYActualizarUICompleta_Helper si no hay nada.
            // El estado de los botones se actualizará porque el índice oficial no cambiará de -1.
            forzarActualizacionEstadoAcciones(); // Asegura que los botones reflejen la lista vacía
            return;
        }

        // --- 3. CALCULAR EL ÍNDICE ANTERIOR ---
        int indiceActual = this.indiceOficialSeleccionado;
        int indiceAnteriorPropuesto;

        if (indiceActual == -1) {
            // Si no hay nada seleccionado actualmente (ej. al inicio o después de limpiar),
            // "anterior" debería ir al último ítem de la lista.
            indiceAnteriorPropuesto = totalImagenes - 1;
        } else {
            indiceAnteriorPropuesto = indiceActual - 1;
        }

        // --- 4. APLICAR LÓGICA DE NAVEGACIÓN CIRCULAR (WRAP AROUND) ---
        if (model.isNavegacionCircularActivada()) {
            if (indiceAnteriorPropuesto < 0) {
                indiceAnteriorPropuesto = totalImagenes - 1; // Va al último
                System.out.println("    -> Navegación circular activada: Wrap around al final (índice " + indiceAnteriorPropuesto + ")");
            }
        } else { // Navegación NO circular
            if (indiceAnteriorPropuesto < 0) {
                // Si se pasó del inicio y no hay wrap, la selección no debe cambiar si ya estaba en el primero.
                if (indiceActual == 0) {
                    System.out.println("    -> Ya en el primer ítem y navegación no circular. No hay cambio de índice.");
                    // Aunque no cambie el índice, es bueno asegurar la visibilidad
                    asegurarVisibilidadAmbasListasSiVisibles(indiceActual);
                    // El estado de los botones de navegación ya debería ser correcto, pero forzamos por si acaso.
                    forzarActualizacionEstadoAcciones();
                    return; // Salir, no se llama al helper de actualización completa.
                }
                // Si no estaba en el primero pero el cálculo dio <0, se queda en el primero.
                indiceAnteriorPropuesto = 0;
                System.out.println("    -> Navegación no circular: Se detiene en el primer ítem (índice " + indiceAnteriorPropuesto + ")");
            }
        }

        // --- 5. PROCESAR LA SELECCIÓN SI EL ÍNDICE HA CAMBIADO EFECTIVAMENTE ---
        // Solo procesar si el índice calculado es diferente al actual,
        // O si la selección inicial era -1 y ahora se va a un índice válido.
        if (indiceAnteriorPropuesto != indiceActual || (indiceActual == -1 && indiceAnteriorPropuesto == totalImagenes -1) ) {
            System.out.println("    -> Llamando a helper interno para procesar nuevo índice oficial: " + indiceAnteriorPropuesto);
            seleccionarIndiceYActualizarUICompleta_Helper(indiceAnteriorPropuesto);
        } else {
            // Esto podría pasar si solo hay un ítem y la navegación no es circular.
            System.out.println("    -> No hubo cambio efectivo en el índice. Índice oficial permanece: " + this.indiceOficialSeleccionado);
            asegurarVisibilidadAmbasListasSiVisibles(this.indiceOficialSeleccionado);
            // Forzar actualización de botones por si acaso (aunque seleccionarIndiceYActualizarUICompleta_Helper ya lo haría)
            forzarActualizacionEstadoAcciones();
        }
        System.out.println(">>> Coordinator: Fin seleccionarAnterior. Índice Oficial Final: " + this.indiceOficialSeleccionado);
    }// --- FIN seleccionarAnterior ---
    
    
    /**
     * Selecciona la primera imagen (índice 0) en la lista principal.
     * MODIFICADO: Llama al helper interno.
     */
    public synchronized void seleccionarPrimero() {
        System.out.println(">>> Coordinator: Navegación -> Primero (Actual Oficial: " + indiceOficialSeleccionado + ")");
        if (model == null || model.getModeloLista() == null) {
             System.err.println("ERROR [seleccionarPrimero]: Modelo no disponible."); return;
        }
        int total = model.getModeloLista().getSize();
        if (total > 0 && this.indiceOficialSeleccionado != 0) { // Solo si hay elementos y no es ya el primero
            System.out.println("    -> Llamando a helper interno para procesar índice: 0");
            seleccionarIndiceYActualizarUICompleta_Helper(0); // <-- LLAMAR AL HELPER
        } else if (total == 0) {
            System.out.println("    -> Lista vacía.");
        } else {
             System.out.println("    -> Ya está en el primer índice (0).");
             // Asegurar visibilidad por si acaso
             asegurarVisibilidadAmbasListasSiVisibles(0);
        }
        System.out.println(">>> Coordinator: Fin seleccionarPrimero");
    } // --- FIN seleccionarPrimero ---    
    
    
    /**
     * Selecciona la última imagen en la lista principal.
     * MODIFICADO: Llama al helper interno.
     */
    public synchronized void seleccionarUltimo() {
        System.out.println(">>> Coordinator: Navegación -> Último (Actual Oficial: " + indiceOficialSeleccionado + ")");
         if (model == null || model.getModeloLista() == null) {
              System.err.println("ERROR [seleccionarUltimo]: Modelo no disponible."); return;
         }
         int total = model.getModeloLista().getSize();
         int ultimoIndice = total - 1;
         if (total > 0 && this.indiceOficialSeleccionado != ultimoIndice) { // Solo si hay elementos y no es ya el último
             System.out.println("    -> Llamando a helper interno para procesar índice: " + ultimoIndice);
             seleccionarIndiceYActualizarUICompleta_Helper(ultimoIndice); // <-- LLAMAR AL HELPER
         } else if (total == 0) {
             System.out.println("    -> Lista vacía.");
         } else {
              System.out.println("    -> Ya está en el último índice (" + ultimoIndice + ").");
              // Asegurar visibilidad por si acaso
              asegurarVisibilidadAmbasListasSiVisibles(ultimoIndice);
         }
         System.out.println(">>> Coordinator: Fin seleccionarUltimo");
    } // --- FIN seleccionarUltimo ---    

    
    /**
     * Selecciona la siguiente imagen en la lista principal.
     * El comportamiento en los extremos (última imagen) depende del estado
     * de 'navegacionCircularActivada' en el VisorModel.
     * Actualiza el estado interno, la UI (incluyendo la imagen principal y miniaturas)
     * y el estado de los botones de navegación.
     */
    public synchronized void seleccionarSiguiente() {
        // --- 1. LOG DE INICIO Y VALIDACIÓN DE DEPENDENCIAS ---
        System.out.println(">>> Coordinator: Solicitud de Navegación -> SIGUIENTE (Índice Oficial Actual: " + this.indiceOficialSeleccionado + ")");
        if (model == null || model.getModeloLista() == null) {
            System.err.println("    ERROR CRÍTICO [seleccionarSiguiente]: VisorModel o su lista interna son nulos. No se puede navegar.");
            return;
        }

        // --- 2. OBTENER ESTADO ACTUAL Y TAMAÑO DE LA LISTA ---
        final DefaultListModel<String> modeloPrincipal = model.getModeloLista();
        final int totalImagenes = modeloPrincipal.getSize();

        if (totalImagenes == 0) {
            System.out.println("    -> Lista de imágenes vacía. No hay nada a qué navegar.");
            forzarActualizacionEstadoAcciones();
            return;
        }

        // --- 3. CALCULAR EL ÍNDICE SIGUIENTE ---
        int indiceActual = this.indiceOficialSeleccionado;
        int indiceSiguientePropuesto;

        if (indiceActual == -1) {
            // Si no hay nada seleccionado, "siguiente" debería ir al primer ítem.
            indiceSiguientePropuesto = 0;
        } else {
            indiceSiguientePropuesto = indiceActual + 1;
        }

        // --- 4. APLICAR LÓGICA DE NAVEGACIÓN CIRCULAR (WRAP AROUND) ---
        if (model.isNavegacionCircularActivada()) {
            if (indiceSiguientePropuesto >= totalImagenes) {
                indiceSiguientePropuesto = 0; // Va al primero
                System.out.println("    -> Navegación circular activada: Wrap around al inicio (índice " + indiceSiguientePropuesto + ")");
            }
        } else { // Navegación NO circular
            if (indiceSiguientePropuesto >= totalImagenes) {
                // Si se pasó del final y no hay wrap, la selección no debe cambiar si ya estaba en el último.
                if (indiceActual == totalImagenes - 1) {
                    System.out.println("    -> Ya en el último ítem y navegación no circular. No hay cambio de índice.");
                    asegurarVisibilidadAmbasListasSiVisibles(indiceActual);
                    forzarActualizacionEstadoAcciones();
                    return; // Salir, no se llama al helper.
                }
                // Si no estaba en el último pero el cálculo dio >= total, se queda en el último.
                indiceSiguientePropuesto = totalImagenes - 1;
                System.out.println("    -> Navegación no circular: Se detiene en el último ítem (índice " + indiceSiguientePropuesto + ")");
            }
        }

        // --- 5. PROCESAR LA SELECCIÓN SI EL ÍNDICE HA CAMBIADO EFECTIVAMENTE ---
        if (indiceSiguientePropuesto != indiceActual || (indiceActual == -1 && indiceSiguientePropuesto == 0)) {
            System.out.println("    -> Llamando a helper interno para procesar nuevo índice oficial: " + indiceSiguientePropuesto);
            seleccionarIndiceYActualizarUICompleta_Helper(indiceSiguientePropuesto);
        } else {
            System.out.println("    -> No hubo cambio efectivo en el índice. Índice oficial permanece: " + this.indiceOficialSeleccionado);
            asegurarVisibilidadAmbasListasSiVisibles(this.indiceOficialSeleccionado);
            forzarActualizacionEstadoAcciones();
        }
        System.out.println(">>> Coordinator: Fin seleccionarSiguiente. Índice Oficial Final: " + this.indiceOficialSeleccionado);
    }// --- FIN seleccionarSiguiente ---

    
    
    /**
     * Asegura que el índice especificado sea visible en ambas JList
     * (listaNombres y listaMiniaturas), si los componentes son actualmente visibles.
     * Es seguro llamar desde cualquier hilo, ya que usa invokeLater internamente si es necesario.
     *
     * @param indice El índice (0-based, relativo al modelo PRINCIPAL) a hacer visible.
     */
    public void asegurarVisibilidadAmbasListasSiVisibles(final int indice) {
        // 1. Validación inicial y log
        if (indice < 0) {
            return;
        }
        System.out.println("    [Asegurar Visibilidad] Solicitud para asegurar visibilidad del índice principal: " + indice);

        // 2. Ejecución en EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> asegurarVisibilidadAmbasListasSiVisibles(indice));
            return;
        }

        // --- LÓGICA DENTRO DEL EDT ---

        // 3. Asegurar visibilidad en listaNombres
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        JPanel panelIzquierdo = registry.get("panel.izquierdo.listaArchivos");

        if (listaNombres != null && panelIzquierdo != null && panelIzquierdo.isShowing()) {
            ListModel<?> modelNom = listaNombres.getModel();
            if (modelNom != null && indice < modelNom.getSize()) {
                try {
                    listaNombres.ensureIndexIsVisible(indice);
                } catch (Exception ex) {
                    System.err.println("ERROR [Asegurar Visibilidad Nombres EDT] para índice " + indice + ": " + ex.getMessage());
                }
            }
        }

        // 4. Asegurar visibilidad en listaMiniaturas
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        JScrollPane scrollMiniaturas = registry.get("scroll.miniaturas");

        if (listaMiniaturas != null && scrollMiniaturas != null && scrollMiniaturas.isShowing()) {
            ListModel<String> modelMin = listaMiniaturas.getModel();
            
            // Traducir el índice principal al índice relativo del modelo de miniaturas
            int indiceRelativo = -1;
            if (model != null && model.getModeloLista() != null && indice < model.getModeloLista().getSize() && modelMin != null) {
                String clavePrincipal = model.getModeloLista().getElementAt(indice);
                if (clavePrincipal != null) {
                    for (int i = 0; i < modelMin.getSize(); i++) {
                        if (clavePrincipal.equals(modelMin.getElementAt(i))) {
                            indiceRelativo = i;
                            break;
                        }
                    }
                }
            }

            // Si se encontró un índice relativo válido, asegurar su visibilidad
            if (indiceRelativo != -1) {
                try {
                    listaMiniaturas.ensureIndexIsVisible(indiceRelativo);
                } catch (Exception ex) {
                    System.err.println("ERROR [Asegurar Visibilidad Miniaturas EDT] para índice relativo " + indiceRelativo + ": " + ex.getMessage());
                }
            }
        }

        System.out.println("    [Asegurar Visibilidad] Fin del proceso para índice principal: " + indice);
    } // --- FIN asegurarVisibilidadAmbasListasSiVisibles ---


     /**
      * Punto de entrada principal para procesar una nueva selección o navegación.
      * Se encarga de la validación inicial, manejo del flag de sincronización,
      * actualización del estado interno y actualización de TODA la UI necesaria.
      *
      * @param indice Índice (0-based) en el modelo PRINCIPAL a seleccionar.
      *               Puede ser -1 para limpiar la selección.
      */
     public synchronized void seleccionarIndiceYActualizarUICompleta(int indice) {
         // --- 1. LOG INICIAL Y VALIDACIÓN PREVIA ---
         System.out.println(">>> Coordinator: Solicitud para procesar índice: " + indice + " (Actual Oficial: " + indiceOficialSeleccionado + ")");

         int total = (model != null && model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
         
         if (indice < -1 || (indice >= 0 && total == 0) || (indice >= total && total > 0)) {
              System.err.println("    -> Índice inválido (" + indice + ") para tamaño de lista (" + total + "). Ignorando.");
              return;
         }
         
         if (indice == this.indiceOficialSeleccionado) {
             System.out.println("    -> Ignorando (mismo índice ya seleccionado oficialmente).");
             return;
         }

         if (sincronizandoUI) {
             System.out.println("    -> Ignorando (operación de sincronización ya en curso).");
             return;
         }

         // --- 2. ACTIVAR FLAG DE SINCRONIZACIÓN ---
         setSincronizandoUI(true);

         try {
             // --- 3. PROCESAR SELECCIÓN INTERNA ---
             boolean cambio = seleccionarImagenInterno(indice);

             // --- 4. ACTUALIZAR VISTAS (SI HUBO CAMBIO) ---
             if (cambio) {
                 // 4.1. Sincronizar la JList de Nombres VISUALMENTE
                 JList<String> listaNombres = registry.get("list.nombresArchivo");
                 sincronizarListaUI(listaNombres, indice);

                 // 4.2. Sincronizar/Actualizar la JList de Miniaturas
                 if (controller != null) {
                     this.actualizarModeloYVistaMiniaturas(indice);
                 } else {
                      System.err.println("ERROR CRÍTICO [UI Completa]: Controller es null. No se pueden actualizar miniaturas.");
                 }

                 // 4.3. CORRECCIÓN DEL SHARED SELECTION MODEL
                 // Después de que `actualizarModeloYVistaMiniaturas` potencialmente desincronice
                 // el modelo de selección, lo forzamos de vuelta al índice principal correcto.
                 if (listaNombres != null) {
                     javax.swing.ListSelectionModel sharedSelectionModel = listaNombres.getSelectionModel();
                     if (sharedSelectionModel != null) {
                         if (indice != -1) {
                             // Si el índice actual no es el que debería ser, lo corregimos.
                             if (sharedSelectionModel.getLeadSelectionIndex() != this.indiceOficialSeleccionado) {
                                 sharedSelectionModel.setSelectionInterval(this.indiceOficialSeleccionado, this.indiceOficialSeleccionado);
                             }
                         } else {
                             // Si el índice es -1 (deseleccionar) y el modelo de selección no está vacío, lo limpiamos.
                             if (!sharedSelectionModel.isSelectionEmpty()) {
                                 sharedSelectionModel.clearSelection();
                             }
                         }
                     } else {
                         System.err.println("ERROR CRÍTICO [UI Completa]: No se pudo obtener sharedSelectionModel para corrección.");
                     }
                 } else {
                     System.err.println("ERROR CRÍTICO [UI Completa]: 'list.nombresArchivo' no encontrada en el registro para corrección.");
                 }
             }
         } finally {
             // --- 5. DESACTIVAR FLAG DE SINCRONIZACIÓN ---
             SwingUtilities.invokeLater(() -> setSincronizandoUI(false));
         }

         // --- 6. LOG FINAL ---
         System.out.println(">>> Coordinator: Fin seleccionarIndiceYActualizarUICompleta(" + indice + ")");
     } // --- FIN seleccionarIndiceYActualizarUICompleta ---    
    
    
    /**
     * Actualiza el estado 'enabled' de las Actions de navegación
     * (Primera, Anterior, Siguiente, Última) basándose en el tamaño actual
     * de la lista, el índice seleccionado y el estado de la navegación circular.
     * Este método es llamado por `forzarActualizacionEstadoNavegacion()`
     * y también internamente después de que `seleccionarImagenInterno` cambie el índice.
     */
    private void actualizarEstadoEnabledAccionesNavegacion() {
        // --- 1. VALIDACIÓN DE DEPENDENCIAS ---
        if (model == null || model.getModeloLista() == null || controller == null || controller.getActionMap() == null) {
            System.err.println("WARN [LC.actualizarEstadoEnabledAccionesNavegacion]: " +
                               "Modelo, Controller o ActionMap nulos. No se puede actualizar estado enabled.");
            return;
        }

        // --- 2. OBTENER ESTADO ACTUAL ---
        DefaultListModel<String> modeloListaActual = model.getModeloLista();
        boolean hayAlgunaImagen = !modeloListaActual.isEmpty();
        int indiceActual = this.indiceOficialSeleccionado; // El índice maestro gestionado por ListCoordinator
        int ultimoIndicePosible = hayAlgunaImagen ? modeloListaActual.getSize() - 1 : -1;
        boolean navCircular = model.isNavegacionCircularActivada(); // Leer del modelo

        Map<String, Action> actionMap = controller.getActionMap();

        // --- 3. OBTENER REFERENCIAS A LAS ACTIONS DE NAVEGACIÓN ---
        Action firstAction = actionMap.get(AppActionCommands.CMD_NAV_PRIMERA);
        Action prevAction  = actionMap.get(AppActionCommands.CMD_NAV_ANTERIOR);
        Action nextAction  = actionMap.get(AppActionCommands.CMD_NAV_SIGUIENTE);
        Action lastAction  = actionMap.get(AppActionCommands.CMD_NAV_ULTIMA);

        // --- 4. APLICAR LÓGICA PARA HABILITAR/DESHABILITAR ---

        // Botón "Primera Imagen":
        // - Habilitado si hay imágenes Y no estamos ya en la primera.
        if (firstAction != null) {
            firstAction.setEnabled(hayAlgunaImagen && indiceActual > 0);
        }

        // Botón "Última Imagen":
        // - Habilitado si hay imágenes Y no estamos ya en la última.
        if (lastAction != null) {
            lastAction.setEnabled(hayAlgunaImagen && indiceActual < ultimoIndicePosible);
        }

        // Botón "Imagen Anterior":
        // - Habilitado si hay imágenes Y (la navegación circular está activa O no estamos en la primera).
        if (prevAction != null) {
            prevAction.setEnabled(hayAlgunaImagen && (navCircular || indiceActual > 0));
        }

        // Botón "Siguiente Imagen":
        // - Habilitado si hay imágenes Y (la navegación circular está activa O no estamos en la última).
        if (nextAction != null) {
            nextAction.setEnabled(hayAlgunaImagen && (navCircular || indiceActual < ultimoIndicePosible));
        }

        // Log opcional para depuración
        /*
        System.out.println(String.format("  [LC ActualizarNavActions] Estado: hayImg=%b, idx=%d, ultIdx=%d, navCirc=%b -> PrevEn=%b, NextEn=%b, FirstEn=%b, LastEn=%b",
            hayAlgunaImagen, indiceActual, ultimoIndicePosible, navCircular,
            (prevAction != null ? prevAction.isEnabled() : null),
            (nextAction != null ? nextAction.isEnabled() : null),
            (firstAction != null ? firstAction.isEnabled() : null),
            (lastAction != null ? lastAction.isEnabled() : null)
        ));
        */
    }// FIN del metodo actualizarEstadoEnabledAccionesNavegacion
    
    

    
    
    // -------------------------------------------------------- METODOS DE NAVEGACION E IMAGENES
    
    
    /**
     * Lógica central para procesar una nueva selección.
     * Actualiza el estado interno, delega carga de imagen.
     * DEVUELVE true si el índice oficial cambió, false si no.
     */
    private boolean seleccionarImagenInterno(int indiceDeseado) {
        System.out.println("  [ListCoordinator Interno] Procesando índice: " + indiceDeseado
                         + " (Oficial actual: " + this.indiceOficialSeleccionado + ")");

        // 1. Validar Índice
        if (indiceDeseado < -1 || (indiceDeseado >= 0 && indiceDeseado >= model.getModeloLista().getSize())) {
            System.err.println("    -> Índice inválido. Ignorando.");
            return false; // No hubo cambio
        }

        // 2. Evitar Trabajo Redundante/Bucles
        if (indiceDeseado == this.indiceOficialSeleccionado) {
            System.out.println("    -> Índice deseado ya es el oficial. No se hace nada.");
            return false; // No hubo cambio
        }
        
        // 4. Actualizar Estado Oficial
        System.out.println("    -> Actualizando índice oficial a: " + indiceDeseado);
        this.indiceOficialSeleccionado = indiceDeseado;

        // 5. Actualizar Modelo y Delegar Carga de Imagen Principal
        if (indiceDeseado != -1) {
             String claveSeleccionada = model.getModeloLista().getElementAt(indiceDeseado);
             model.setSelectedImageKey(claveSeleccionada);
             System.out.println("    -> Delegando carga de imagen al Controller para índice: " + indiceDeseado);
             controller.actualizarImagenPrincipal(indiceDeseado);
        } else {
             model.setSelectedImageKey(null);
             model.setCurrentImage(null);
             System.out.println("    -> Delegando limpieza de UI al Controller (índice -1)");
             //controller.limpiarUI();
             controller.actualizarImagenPrincipal(-1);
             actualizarEstadoDeTodasLasAccionesContextuales();
        }

        actualizarEstadoEnabledAccionesNavegacion();
        actualizarEstadoDeTodasLasAccionesContextuales();
        
        System.out.println("  [ListCoordinator Interno] Procesamiento interno finalizado para índice: " + indiceDeseado);
        return true; // Hubo cambio
    }
    
    
    /**
     * Reinicia el estado del coordinador y procede a seleccionar un nuevo índice.
     * Este es el punto de entrada recomendado después de una recarga completa de la lista de imágenes.
     * @param indiceDeseado El índice a seleccionar después del reinicio.
     */
    public synchronized void reiniciarYSeleccionarIndice(int indiceDeseado) {
        System.out.println(">>> Coordinator: Solicitud de REINICIO y selección de índice: " + indiceDeseado);
        
        // 1. Reiniciar el estado interno del coordinador.
        this.indiceOficialSeleccionado = -1;
        
        // 2. Limpiar la selección de las listas en la UI por si acaso.
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres != null) {
            listaNombres.clearSelection();
        }
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas != null) {
            listaMiniaturas.clearSelection();
        }
        
        // 3. Forzar actualización del estado 'enabled' de las acciones.
        forzarActualizacionEstadoAcciones();
        
        // 4. Proceder con la selección normal desde un estado limpio.
        seleccionarIndiceYActualizarUICompleta(indiceDeseado);

    } // --- Fin del método reiniciarYSeleccionarIndice ---
    
    
    /**
     * Unifica la navegación de la rueda del ratón. Llama a seleccionarSiguiente()
     * o seleccionarAnterior() basándose en la dirección de la rotación de la rueda.
     *
     * @param wheelRotation El valor de getWheelRotation() del MouseWheelEvent.
     *                      Un valor negativo significa "rueda hacia arriba" (anterior).
     *                      Un valor positivo significa "rueda hacia abajo" (siguiente).
     */
    public void seleccionarSiguienteOAnterior(int wheelRotation) {
        if (wheelRotation < 0) {
            seleccionarAnterior();
        } else {
            seleccionarSiguiente();
        }
    }
    // --- FIN del metodo seleccionarSiguienteOAnterior ---
    
    /**
     * HELPER INTERNO: Procesa la selección y actualiza UI SIN manejar el flag
     * sincronizandoUI directamente (asume que la responsabilidad recae en
     * los métodos que lo llaman o en los métodos de sincronización de UI).
     *
     * Pasos:
     * 1. Llama a `seleccionarImagenInterno` para actualizar el estado lógico del
     *    modelo y solicitar la carga/limpieza de la imagen principal.
     * 2. Si la selección interna indicó un cambio:
     *    a) Llama a `sincronizarListaUI` para actualizar visualmente la `listaNombres`.
     *    b) Llama a `this.actualizarModeloYVistaMiniaturas` para reconstruir
     *       y actualizar la `listaMiniaturas`.
     * 3. Si NO hubo cambio interno, opcionalmente asegura la visibilidad del índice actual.
     *
     * @param indice El índice (0-based, relativo al modelo PRINCIPAL) a seleccionar y mostrar.
     */
    private void seleccionarIndiceYActualizarUICompleta_Helper(int indice) 
    { 
    	// --- 1. PROCESAR SELECCIÓN INTERNA ---
        // 1.1. Llamar al método que actualiza el estado del modelo (indiceOficial, selectedKey)
        //      y solicita la carga/limpieza de la imagen principal al controlador.
        // 1.2. Guarda si el índice oficial realmente cambió como resultado de esta llamada.
        boolean cambio = seleccionarImagenInterno(indice);

        // --- 2. ACTUALIZAR VISTAS (SI HUBO CAMBIO LÓGICO) ---
        // 2.1. Comprobar si la llamada anterior resultó en un cambio del índice oficial.
        if (cambio) {
            // 2.2. Log indicando que se procederá a actualizar las vistas.
            System.out.println("      -> [Helper Interno] Cambio detectado. Actualizando UI (Nombres y Miniaturas) para índice " + this.indiceOficialSeleccionado); // Usa el índice oficial actualizado

            // 2.3. Sincronizar la JList de Nombres VISUALMENTE.
            //      Llama al método `sincronizarListaUI` pasándole la lista de nombres
            //      y el índice oficial que AHORA tiene el coordinador.
            //      `sincronizarListaUI` se encargará de activar/desactivar su propio flag.
            JList<String> listaNombres = registry.get("list.nombresArchivo"); sincronizarListaUI(listaNombres, this.indiceOficialSeleccionado);

            // 2.4. Sincronizar/Actualizar la JList de Miniaturas.
            // 2.4.1. Verificar que el controlador exista.
            if (controller != null) {
                // 2.4.2. Llamar al método del controlador que reconstruye el modelo de la
                //         ventana deslizante de miniaturas y actualiza la JList.
                //         Se le pasa el índice oficial que AHORA tiene el coordinador.
                //         Este método internamente también llamará a `sincronizarListaUI`
                //         para las miniaturas, que gestionará su propio flag.
                this.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado);
            } else {
                 // 2.4.3. Error crítico si no se puede acceder al controlador.
                 System.err.println("ERROR CRÍTICO [Helper Interno]: Controller es null. No se pueden actualizar miniaturas.");
            }
        } else {
             // --- 3. NO HUBO CAMBIO LÓGICO (Índice ya era el seleccionado) ---
             // 3.1. Log indicando que no hubo cambio interno.
             System.out.println("      -> [Helper Interno] No hubo cambio interno (índice ya era " + this.indiceOficialSeleccionado + ").");

             // 3.2. Asegurar visibilidad del índice actual.
             //      Esto es útil si este helper fue llamado por una acción (ej. Home, End,
             //      Siguiente/Anterior repetido en los extremos) que no cambió el índice
             //      lógico pero sí requiere que el elemento sea visible.
             //      Llama al método que se encarga de llamar a ensureIndexIsVisible
             //      en ambas listas si son visibles, usando el índice oficial actual.
             asegurarVisibilidadAmbasListasSiVisibles(this.indiceOficialSeleccionado); // Usa el índice oficial actual
        }

        // --- 4. LOG FINAL DEL HELPER ---
        System.out.println("      -> [Helper Interno] Fin del procesamiento para índice solicitado: " + indice);

    } // --- FIN seleccionarIndiceYActualizarUICompleta_Helper ---    
    
// ****************************************************************************** ACTUALIZACION BARRA DE MINIATURAS

 // En ListCoordinator.java

    /**
     * Orquesta la actualización de la barra de miniaturas. Valida el estado actual,
     * comprueba si la actualización es necesaria y, si lo es, delega la actualización
     * de la UI al Event Dispatch Thread (EDT).
     *
     * @param indiceSeleccionadoPrincipal Índice (0-based) en el modelo PRINCIPAL.
     */
    public void actualizarModeloYVistaMiniaturas(int indiceSeleccionadoPrincipal) {
        System.out.println("\n--- [LC Refactor] Orquestando actualización para índice: " + indiceSeleccionadoPrincipal);

        // SECCIÓN 1 del original: Validaciones y preparación
        if (!esActualizacionRequerida(indiceSeleccionadoPrincipal)) {
            System.out.println("--- [LC Refactor] Actualización no requerida. FIN ---");
            return;
        }

        final DefaultListModel<String> modeloPrincipal = model.getModeloLista();
        final int totalPrincipal = modeloPrincipal.getSize();

        if (totalPrincipal == 0 || indiceSeleccionadoPrincipal < 0 || indiceSeleccionadoPrincipal >= totalPrincipal) {
            System.out.println("  -> [LC] Lista principal vacía o índice inválido. Limpiando UI.");
            limpiarUIMiniaturasEnEDT();
            return;
        }

        // SECCIÓN 2 del original: Programar la actualización en el EDT
        SwingUtilities.invokeLater(() -> {
            ejecutarActualizacionUIEnEDT(indiceSeleccionadoPrincipal, modeloPrincipal, totalPrincipal);
        });

        System.out.println("--- [LC Refactor] Tarea de actualización de UI programada. FIN ---");
    }
    
    
    /**
     * Valida si la actualización es realmente necesaria. Comprueba dependencias críticas
     * y si la selección deseada ya es la actual para evitar trabajo redundante.
     * Corresponde a las secciones 1.2 y 1.2.2 del original.
     *
     * @param indiceDeseado El índice que se quiere seleccionar.
     * @return `true` si se debe proceder con la actualización, `false` en caso contrario.
     */
    private boolean esActualizacionRequerida(int indiceDeseado) {
        if (model == null || model.getModeloLista() == null || view == null || registry.get("list.miniaturas") == null) {
            System.err.println("WARN [LC.esActualizacionRequerida]: Dependencias nulas. Abortando.");
            return false;
        }

        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        if (listaMiniaturas.getModel().getSize() > 0 && listaMiniaturas.getSelectedIndex() != -1) {
            try {
                String claveActualMiniaturas = listaMiniaturas.getSelectedValue();
                String claveDeseadaPrincipal = model.getModeloLista().getElementAt(indiceDeseado);
                
                if (claveActualMiniaturas != null && claveActualMiniaturas.equals(claveDeseadaPrincipal)) {
                    System.out.println("  -> [LC] La selección ya es correcta. Se omite actualización.");
                    asegurarVisibilidadAmbasListasSiVisibles(indiceDeseado); // Solo aseguramos visibilidad
                    return false;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("WARN [LC.esActualizacionRequerida]: Inconsistencia de índice. Se procederá a reconstruir.");
                // Si hay un error, es mejor reconstruir, así que retornamos true.
            }
        }
        return true;
    }

    /**
     * Contiene toda la lógica que debe ejecutarse en el Event Dispatch Thread (EDT).
     * Orquesta el cálculo del rango, la construcción del modelo y la actualización de la JList.
     */
    private void ejecutarActualizacionUIEnEDT(int indiceSeleccionadoPrincipal, DefaultListModel<String> modeloPrincipal, int totalPrincipal) {
        // Re-validación dentro del EDT (buena práctica)
        if (view == null || registry.get("list.miniaturas") == null || model == null) {
            System.err.println("ERROR [LC.ejecutarEnEDT]: Dependencias nulas en EDT. Abortando.");
            return;
        }
        
        setSincronizandoUI(true);
        try {
            // SECCIÓN 4: CÁLCULO DEL RANGO
            VisorController.RangoMiniaturasCalculado rangoDinamico = controller.calcularNumMiniaturasDinamicas();
            final int inicioRango = Math.max(0, indiceSeleccionadoPrincipal - rangoDinamico.antes);
            final int finRango = Math.min(totalPrincipal - 1, indiceSeleccionadoPrincipal + rangoDinamico.despues);
            System.out.println("   -> [EDT] Rango final en modelo principal: [" + inicioRango + ".." + finRango + "]");

            // SECCIÓN 5: CONSTRUCCIÓN DEL NUEVO MODELO
            DefaultListModel<String> nuevoModeloMiniaturas = construirModeloParaRango(modeloPrincipal, inicioRango, finRango);
            
            // SECCIÓN 6: PRE-CALENTAMIENTO DEL CACHÉ
            precalentarCacheParaRango(modeloPrincipal, inicioRango, finRango);

            // SECCIÓN 7: ACTUALIZACIÓN DE LA JLIST
            actualizarComponenteJList(nuevoModeloMiniaturas, indiceSeleccionadoPrincipal, inicioRango);

        } finally {
            SwingUtilities.invokeLater(() -> setSincronizandoUI(false));
        }
    }

    /**
     * Construye y devuelve un nuevo DefaultListModel con las claves del rango especificado.
     * Corresponde a la sección 5 del original.
     * 
     * @param modeloPrincipal El modelo de datos completo.
     * @param inicio El índice de inicio del rango.
     * @param fin El índice de fin del rango.
     * @return Un nuevo DefaultListModel poblado con los elementos del rango.
     */
    private DefaultListModel<String> construirModeloParaRango(DefaultListModel<String> modeloPrincipal, int inicio, int fin) {
        DefaultListModel<String> nuevoModelo = new DefaultListModel<>();
        for (int i = inicio; i <= fin; i++) {
            nuevoModelo.addElement(modeloPrincipal.getElementAt(i));
        }
        System.out.println("   -> [EDT] Nuevo modelo de miniaturas construido. Tamaño: " + nuevoModelo.getSize());
        return nuevoModelo;
    }

    /**
     * Lanza el precalentamiento asíncrono del caché para las imágenes del rango visible.
     * Corresponde a la sección 6 del original.
     *
     * @param modeloPrincipal El modelo de datos completo.
     * @param inicio El índice de inicio del rango.
     * @param fin El índice de fin del rango.
     */
    private void precalentarCacheParaRango(DefaultListModel<String> modeloPrincipal, int inicio, int fin) {
        List<Path> rutasEnRango = new ArrayList<>();
        for (int i = inicio; i <= fin; i++) {
            Path ruta = model.getRutaCompleta(modeloPrincipal.getElementAt(i));
            if (ruta != null) {
                rutasEnRango.add(ruta);
            }
        }
        controller.precalentarCacheMiniaturasAsync(rutasEnRango);
    }

    /**
     * Aplica el nuevo modelo a la JList, ajusta su tamaño preferido, y establece la selección correcta.
     * Corresponde a la sección 7 del original.
     *
     * @param nuevoModelo El nuevo modelo a asignar a la JList.
     * @param indiceSeleccionadoPrincipal El índice absoluto seleccionado en el modelo principal.
     * @param inicioRango El índice absoluto donde comienza el rango de miniaturas.
     */
    private void actualizarComponenteJList(DefaultListModel<String> nuevoModelo, int indiceSeleccionadoPrincipal, int inicioRango) {
        JList<String> listaMiniaturas = registry.get("list.miniaturas");
        
        // Ajustar el PreferredSize para el centrado del FlowLayout
        int cellWidth = listaMiniaturas.getFixedCellWidth();
        int cellHeight = listaMiniaturas.getFixedCellHeight();
        if (cellWidth > 0 && cellHeight > 0) {
            int nuevoAncho = nuevoModelo.getSize() * cellWidth;
            Dimension nuevoTamano = new Dimension(Math.max(cellWidth, nuevoAncho), cellHeight);
            if (!nuevoTamano.equals(listaMiniaturas.getPreferredSize())) {
                listaMiniaturas.setPreferredSize(nuevoTamano);
                if (listaMiniaturas.getParent() != null) {
                    listaMiniaturas.getParent().revalidate();
                }
            }
        }

        // Asignar modelo y seleccionar ítem
        view.setModeloListaMiniaturas(nuevoModelo);
        int indiceRelativo = indiceSeleccionadoPrincipal - inicioRango;
        
        if (indiceRelativo >= 0 && indiceRelativo < nuevoModelo.getSize()) {
            listaMiniaturas.setSelectedIndex(indiceRelativo);
            listaMiniaturas.ensureIndexIsVisible(indiceRelativo);
        } else {
            listaMiniaturas.clearSelection();
        }
        listaMiniaturas.repaint();
    }

    /**
     * Programa la limpieza de la UI de miniaturas en el EDT en caso de lista vacía o índice inválido.
     * Corresponde a la sección 1.4 del original.
     */
    private void limpiarUIMiniaturasEnEDT() {
        SwingUtilities.invokeLater(() -> {
            if (view != null && registry.get("list.miniaturas") != null) {
                JList<String> lMini = registry.get("list.miniaturas");
                lMini.setPreferredSize(new Dimension(50, 50)); // Evitar colapso
                view.setModeloListaMiniaturas(new DefaultListModel<>());
                if (lMini.getParent() != null) {
                    lMini.getParent().revalidate();
                }
            }
        });
    }
    
    
    
    
    
// ************************************************************************** FIN ACTUALIZACION BARRA DE MINIATURAS    
    
    /**
     * Método helper PRIVADO para sincronizar la selección visual y asegurar la
     * visibilidad de un índice específico en UNA JList determinada.
     * Establece el índice seleccionado en la JList y luego usa ensureIndexIsVisible.
     *
     * **MODIFICADO:** Ahora MANEJA el flag `sincronizandoUI` internamente.
     * Las operaciones que realiza (`setSelectedIndex`, `clearSelection`) SÍ
     * dispararán el ListSelectionListener asociado a la JList, el cual DEBE
     * verificar `listCoordinator.isSincronizandoUI()` para ignorar estos eventos.
     *
     * @param lista  La instancia de JList<String> cuya selección y visibilidad se va a actualizar.
     * @param indice El índice (0-based) que debe seleccionarse en esta lista.
     *               Si es -1, se limpiará la selección. El índice debe ser relativo
     *               al ListModel que ESTA JList específica está usando en este momento.
     */
    private void sincronizarListaUI (JList<String> lista, int indice) 
    {
    	if (lista == null) {
    	    System.err.println("ERROR [Sync UI Interno]: La JList proporcionada es null.");
    	    return;
    	}

    	// 1.2. Determinar el nombre de la lista para logs
    	String nombreListaDeterminado = "Desconocida";

    	// <<< INICIO DEL CAMBIO >>>
    	// Obtenemos ambas listas del registro para poder compararlas
    	JList<String> listaNombresDesdeRegistro = registry.get("list.nombresArchivo");
    	JList<String> listaMiniaturasDesdeRegistro = registry.get("list.miniaturas");

    	if (lista == listaNombresDesdeRegistro) {
    	    nombreListaDeterminado = "Nombres";
    	} else if (lista == listaMiniaturasDesdeRegistro) {
    	    nombreListaDeterminado = "Miniaturas";
    	}
        
        // 1.2.1 Declarar la variable como final para usarla en las lambdas
        final String nombreLista = nombreListaDeterminado;

        // 1.3. Log informativo inicial
        System.out.println("      [Sync UI Interno] Iniciando sincronización para lista '" + nombreLista + "' al índice: " + indice);


        // --- 2. COMPARAR ÍNDICE ACTUAL Y DESEADO ---
        // 2.1. Obtener el índice actualmente seleccionado en la JList.
        int indiceActualLista = lista.getSelectedIndex();
        System.out.println("        -> Índice actual en JList '" + nombreLista + "': " + indiceActualLista);

        // 2.2. Comprobar si el índice deseado es diferente al actual.
        if (indiceActualLista == indice) {
            
        	// 2.2.1. Índice ya correcto. No hay cambio de selección necesario.
            System.out.println("        -> Índice ya correcto. Asegurando solo visibilidad.");
            
            // 2.2.2. Asegurar visibilidad por si acaso (ej. scroll necesario).
            if (indice != -1) { // Solo si el índice es válido
                final int finalIndex = indice; // Variable final para lambda
                final JList<String> finalLista = lista; // Variable final para lambda
            
                // 2.2.3. Programar la llamada a ensureIndexIsVisible en el EDT.
                SwingUtilities.invokeLater(() -> { // Esta lambda ahora puede usar 'nombreLista'
                    if (finalLista != null && finalLista.isShowing()) {
                         ListModel<?> modelActualVisibilidad = finalLista.getModel();
                         if (modelActualVisibilidad != null && finalIndex >= 0 && finalIndex < modelActualVisibilidad.getSize()) {
                              System.out.println("          -> [EDT Visibilidad Solo " + nombreLista + "] Asegurando visibilidad para índice " + finalIndex); // Uso de nombreLista
                              try { finalLista.ensureIndexIsVisible(finalIndex); }
                              catch(Exception ex) { System.err.println("ERROR [EDT Visibilidad Solo " + nombreLista + "] al hacer ensureIndexIsVisible(" + finalIndex + "): " + ex.getMessage()); } // Uso de nombreLista
                         } else {
                              System.out.println("WARN [EDT Visibilidad Solo " + nombreLista + "] Índice " + finalIndex + " fuera de rango del modelo actual (" + (modelActualVisibilidad != null ? modelActualVisibilidad.getSize() : "null") + ")."); // Uso de nombreLista
                         }
                    } else {
                          System.out.println("          -> [EDT Visibilidad Solo " + nombreLista + "] Omitido (Lista no visible o nula)."); // Uso de nombreLista
                    }
                });
            }
            
            // 2.2.4. Salir del método porque no hay cambio de selección necesario.
            System.out.println("      [Sync UI Interno] Fin (sin cambio de selección) para lista '" + nombreLista + "'.");
            return;
        }

        // --- 3. ACTIVAR FLAG, APLICAR CAMBIOS Y DESACTIVAR FLAG ---
        // 3.1. Log indicando que se procederá a sincronizar la selección.
        System.out.println("        -> Índice diferente. Iniciando cambio de selección...");

        // 3.2. ACTIVAR EL FLAG `sincronizandoUI` ANTES de llamar a métodos que disparan eventos.
        this.setSincronizandoUI(true); // El setter imprime log

        // 3.3. Usar un bloque try-finally para ASEGURAR la desactivación del flag.
        try {
        	
            // 3.4. APLICAR CAMBIO DE SELECCIÓN EN LA JLIST
            try {
            
            	// 3.4.1. Si el índice deseado es -1, limpiar la selección.
                if (indice == -1) {
                    System.out.println("          -> Limpiando selección.");
                    lista.clearSelection(); // Dispara evento
                }
                
                // 3.4.2. Si el índice deseado es válido (0 o mayor).
                else {
                    ListModel<?> modelActualLista = lista.getModel();
                    if (modelActualLista != null && indice >= 0 && indice < modelActualLista.getSize()) {
                
                    	// 3.4.2.1.1. Establecer el nuevo índice seleccionado en la JList.
                        System.out.println("          -> Estableciendo índice seleccionado a " + indice);
                        lista.setSelectedIndex(indice); // Dispara evento

                        // 3.4.2.1.2. Programar 'ensureIndexIsVisible' en el EDT para seguridad.
                        final int finalIndex = indice;
                        final JList<String> finalLista = lista;
                        SwingUtilities.invokeLater(() -> { // Esta lambda ahora puede usar 'nombreLista'
                            if (finalLista != null && finalLista.isShowing()) {
                                ListModel<?> modelActualVis = finalLista.getModel();
                                if (modelActualVis != null && finalIndex >= 0 && finalIndex < modelActualVis.getSize()) {
                                     System.out.println("            -> [EDT Visibilidad " + nombreLista + "] Asegurando visibilidad para índice " + finalIndex); // Uso de nombreLista
                                     try { finalLista.ensureIndexIsVisible(finalIndex); }
                                     catch(Exception ex) { System.err.println("ERROR [EDT Visibilidad " + nombreLista + "] al hacer ensureIndexIsVisible(" + finalIndex + "): " + ex.getMessage()); } // Uso de nombreLista
                                } else {
                                     System.out.println("WARN [EDT Visibilidad " + nombreLista + "] Índice " + finalIndex + " fuera de rango del modelo actual (" + (modelActualVis != null ? modelActualVis.getSize() : "null") + ") al asegurar visibilidad."); // Uso de nombreLista
                                }
                            } else {
                                System.out.println("            -> [EDT Visibilidad " + nombreLista + "] Omitido (Lista no visible o nula)."); // Uso de nombreLista
                            }
                        });
                    } else {
                        
                    	// 3.4.2.2. Manejar caso de índice deseado fuera de rango.
                        System.err.println("WARN [Sync UI Interno]: Índice deseado (" + indice + ") está fuera de rango para lista '" + nombreLista + "' (Tamaño modelo: " + (modelActualLista != null ? modelActualLista.getSize() : "N/A") + "). Limpiando selección.");
                        lista.clearSelection(); // Limpiar selección
                    }
                } // Fin else (indice >= 0)

                // 3.4.3. Solicitar repintado de la lista.
                lista.repaint();

            } catch (Exception e) {
                // 3.4.4. Capturar cualquier excepción inesperada durante la sincronización.
                System.err.println("ERROR [Sync UI Interno]: Excepción inesperada sincronizando lista '" + nombreLista + "' a índice " + indice + ": " + e.getMessage());
                e.printStackTrace();
                try { lista.clearSelection(); } catch (Exception ignored) {}
            } // Fin try-catch interno

        } finally {
             // 3.5. DESACTIVAR EL FLAG (Siempre, y en otro invokeLater para seguridad)
             SwingUtilities.invokeLater(() -> this.setSincronizandoUI(false)); // El setter imprime log
        } // Fin try-finally externo

        // --- 4. LOG FINAL ---
        System.out.println("      [Sync UI Interno] Fin sincronización para lista '" + nombreLista + "', índice " + indice);

    } // --- FIN sincronizarListaUI ---    
    
    
//    public synchronized void forzarActualizacionEstadoNavegacion() {
//        System.out.println("  [ListCoordinator] Forzando actualización del estado enabled de navegación.");
//        actualizarEstadoEnabledAccionesNavegacion();
//        actualizarEstadoDeTodasLasAccionesContextuales();
//    }    
    
    
    /**
     * Punto de entrada para procesar una selección realizada por el usuario
     * directamente en la lista de nombres (`listaNombres`).
     *
     * Se encarga de:
     * 1. Validar la solicitud (evitar procesar si el índice no cambió).
     * 2. Llamar a `seleccionarImagenInterno` para actualizar el estado del modelo y la imagen principal.
     * 3. Llamar a `this.actualizarModeloYVistaMiniaturas` para actualizar la ventana deslizante de miniaturas.
     *
     * @param indiceDeseado El índice (0-based, relativo al modelo PRINCIPAL) seleccionado por el usuario en `listaNombres`.
     */
    public synchronized void seleccionarDesdeNombres(int indiceDeseado) {
        // --- 1. LOG INICIAL Y VALIDACIÓN PREVIA ---
        // 1.1. Imprimir información sobre la solicitud recibida
        System.out.println(">>> Coordinator: Recibido de Nombres -> Índice: " + indiceDeseado + " (Actual Oficial: " + indiceOficialSeleccionado + ")");

        // 1.2. Comprobar si el índice deseado es el mismo que ya está seleccionado oficialmente.
        if (indiceDeseado == this.indiceOficialSeleccionado) {
            System.out.println("    -> Ignorando (mismo índice ya seleccionado oficialmente).");
            // 1.2.1. Asegurar visibilidad aquí puede ser útil si el usuario hace clic en el mismo item
            //        para traerlo a la vista si se había ido por scroll.
            asegurarVisibilidadAmbasListasSiVisibles(indiceDeseado); // Llamar al helper
            return; // Salir, no hay nada más que hacer si el índice no cambió.
        }

        // --- 2. PROCESAR SELECCIÓN INTERNA Y ACTUALIZAR OTRA VISTA ---
        // 2.1. Llamar al método interno que actualiza el estado del modelo
        //      (indiceOficial, selectedImageKey) y delega la carga de la imagen
        //      principal al controlador. Devuelve true si el índice oficial cambió.
        boolean cambioRealizado = seleccionarImagenInterno(indiceDeseado);

        // 2.2. Si hubo un cambio lógico en la selección:
        if (cambioRealizado) {
            // 2.2.1. Verificar que el controlador esté disponible.
            if (controller != null) {
                // 2.2.2. Log antes de llamar al controlador para actualizar miniaturas.
                System.out.println("    -> Llamando a this.actualizarModeloYVistaMiniaturas(" + this.indiceOficialSeleccionado + ")");

                // 2.2.3. Llamar al método del controlador que reconstruye el modelo de la
                //         ventana deslizante de miniaturas y actualiza la JList correspondiente.
                //         Este método internamente llamará a `sincronizarListaUI` para las miniaturas,
                //         el cual activará/desactivará el flag.
                this.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado);
            } else {
                
            	// 2.2.4. Error crítico si falta el controlador.
                System.err.println("ERROR CRÍTICO: Controller es null en seleccionarDesdeNombres.");
                // FIXME Considerar cómo manejar esta situación (¿lanzar excepción?).
            }
        } else {
             // 2.3. Log si no hubo cambio lógico (inesperado si ya pasó el chequeo inicial)
              System.out.println("    -> seleccionarImagenInterno indicó que no hubo cambio (inesperado aquí).");
        }


        // --- 3. LOG FINAL ---
        System.out.println(">>> Coordinator: Fin seleccionarDesdeNombres(" + indiceDeseado + ")");

    } // --- FIN seleccionarDesdeNombres ---
    
    

    
    
    
    
    
    /**
     * Punto de entrada para procesar una selección realizada por el usuario
     * directamente en la lista de miniaturas (`listaMiniaturas`).
     *
     * Se encarga de:
     * 1. Validar la solicitud (evitar procesar si el índice no cambió).
     * 2. Llamar a `seleccionarImagenInterno` para actualizar el estado del modelo y la imagen principal.
     * 3. Si hubo cambio, llamar a `sincronizarListaUI` para actualizar la selección visual en `listaNombres`.
     * 4. (Opcional pero recomendado) Llamar a `this.actualizarModeloYVistaMiniaturas` para reajustar
     *    la ventana deslizante si fuera necesario (aunque la selección vino de ella).
     *
     * @param indicePrincipalDeseado El índice (0-based, relativo al modelo PRINCIPAL) correspondiente
     *                                a la miniatura seleccionada por el usuario. Este índice ya
     *                                debe haber sido traducido por el listener de miniaturas.
     */
    public synchronized void seleccionarDesdeMiniaturas(int indicePrincipalDeseado) {
        // --- 1. LOG INICIAL Y VALIDACIÓN PREVIA ---
        // 1.1. Imprimir información sobre la solicitud recibida, incluyendo el índice principal
        //      traducido y el índice oficial actual.
        System.out.println(">>> Coordinator: Recibido de Miniaturas -> Índice Principal: " + indicePrincipalDeseado + " (Actual Oficial: " + indiceOficialSeleccionado + ")");

        // 1.2. Comprobar si el índice deseado es el mismo que ya está seleccionado oficialmente.
        if (indicePrincipalDeseado == this.indiceOficialSeleccionado) {
            System.out.println("    -> Ignorando (mismo índice ya seleccionado oficialmente).");

            // 1.2.1. Asegurar visibilidad, especialmente importante para la lista de Nombres,
            //        ya que el usuario hizo clic en Miniaturas.
            asegurarVisibilidadAmbasListasSiVisibles(indicePrincipalDeseado); // Llamar al helper
            return; // Salir, no hay cambio lógico que procesar.
        }
        
        // 1.3. No necesitamos comprobar 'sincronizandoUI' aquí, porque si este método
        //      fue llamado, el listener de Miniaturas ya verificó que no se estaba sincronizando.

        // --- 2. PROCESAR SELECCIÓN INTERNA Y ACTUALIZAR OTRA VISTA ---
        
        // 2.1. Llamar al método interno que actualiza el estado lógico del modelo
        //      (indiceOficial, selectedImageKey) y delega la carga de la imagen
        //      principal al controlador. Devuelve true si el índice oficial cambió.
        boolean cambioRealizado = seleccionarImagenInterno(indicePrincipalDeseado);

        // 2.2. Si hubo un cambio lógico en la selección:
        if (cambioRealizado) {
        
        	// 2.2.1. Sincronizar la JList de NOMBRES.
            //         Llama al método interno que hace setSelectedIndex/ensureIndexIsVisible en listaNombres.
            //         Esta llamada manejará el flag `sincronizandoUI` internamente.
            System.out.println("    -> Llamando a sincronizarListaUI(Nombres, " + this.indiceOficialSeleccionado + ")");
            JList<String> listaNombres = registry.get("list.nombresArchivo"); sincronizarListaUI(listaNombres, this.indiceOficialSeleccionado); // Usa el índice oficial actualizado

            // 2.2.2. (Opcional pero recomendado) Re-Actualizar la Ventana Deslizante de Miniaturas.
            //          Aunque la selección vino de las miniaturas, el rango visible podría necesitar
            //          ajustarse si la selección estaba cerca de los bordes del rango anterior.
            //          Verificar que el controlador esté disponible.
            if (controller != null) {
            
            	// 2.2.2.1. Log indicando la llamada.
                System.out.println("    -> (Re)Llamando a this.actualizarModeloYVistaMiniaturas(" + this.indiceOficialSeleccionado + ") por seguridad de rango.");
                
                // 2.2.2.2. Llamar al método del controlador. Este método internamente
                //          llamará a `sincronizarListaUI` para las miniaturas, que gestionará su flag.
                this.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado); // Usa el índice oficial actualizado
            } else {
                
            	// 2.2.2.3. Error crítico si falta el controlador.
                System.err.println("ERROR CRÍTICO [Desde Miniaturas]: Controller es null. No se pueden (re)actualizar miniaturas.");
            }
        } else {
        	
            // 2.3. Log si no hubo cambio lógico (inesperado si ya pasó el chequeo inicial).
             System.out.println("    -> seleccionarImagenInterno indicó que no hubo cambio (inesperado aquí).");
        }

        // --- 3. LOG FINAL ---

        // 3.1. Imprimir log indicando el fin del procesamiento para esta solicitud.
        System.out.println(">>> Coordinator: Fin seleccionarDesdeMiniaturas(" + indicePrincipalDeseado + ")");

    } // --- FIN seleccionarDesdeMiniaturas ---    
    
    
    /**
     * Selecciona el siguiente "bloque" de imágenes en la lista principal.
     * Salta hacia adelante un número fijo de ítems (TAMANO_SALTO_BLOQUE).
     * No realiza "wrap around", se detiene en el último elemento.
     * Llama al helper interno para actualizar el estado y la UI.
     */
    public synchronized void seleccionarBloqueSiguiente() {
        // --- 1. LOG INICIAL Y VALIDACIÓN ---
        System.out.println(">>> Coordinator: Navegación -> Bloque Siguiente (Actual Oficial: " + indiceOficialSeleccionado + ")");
        
        if (model == null || model.getModeloLista() == null) {
             System.err.println("ERROR [seleccionarBloqueSiguiente]: Modelo no disponible."); return;
        }

        int total = model.getModeloLista().getSize();
        if (total == 0) { System.out.println("    -> Lista vacía."); return; }

        // --- 2. CALCULAR ÍNDICE DEL BLOQUE SIGUIENTE ---
        int actual = this.indiceOficialSeleccionado;
        // Si no hay selección, empezar desde el principio + salto
        int indiceDeseado = (actual == -1) ? this.pageScrollIncrement : actual + this.pageScrollIncrement;

        // 3. CLAMP (Limitar) al último índice válido. No hacer wrap around.
        int ultimoIndice = total - 1;
        indiceDeseado = Math.min(indiceDeseado, ultimoIndice); // No pasar del último

        // --- 4. LLAMAR AL HELPER SI EL ÍNDICE CAMBIÓ ---
        if (indiceDeseado != actual) {
            System.out.println("    -> Llamando a helper interno para procesar índice: " + indiceDeseado);
            seleccionarIndiceYActualizarUICompleta_Helper(indiceDeseado); // Llama al helper SIN manejo de flag
        } else {
            System.out.println("    -> No hay cambio de índice (ya está en el último bloque o al final).");
            // Asegurar visibilidad si ya estaba al final
            asegurarVisibilidadAmbasListasSiVisibles(actual);
        }
        
        // --- 5. LOG FINAL ---
        System.out.println(">>> Coordinator: Fin seleccionarBloqueSiguiente");
    } // --- FIN seleccionarBloqueSiguiente ---


    /**
     * Selecciona el "bloque" anterior de imágenes en la lista principal.
     * Salta hacia atrás un número fijo de ítems (TAMANO_SALTO_BLOQUE).
     * No realiza "wrap around", se detiene en el primer elemento (índice 0).
     * Llama al helper interno para actualizar el estado y la UI.
     */
    public synchronized void seleccionarBloqueAnterior() 
    {
        // --- 1. LOG INICIAL Y VALIDACIÓN ---
        System.out.println(">>> Coordinator: Navegación -> Bloque Anterior (Actual Oficial: " + indiceOficialSeleccionado + ")");
    
        if (model == null || model.getModeloLista() == null) {
             System.err.println("ERROR [seleccionarBloqueAnterior]: Modelo no disponible."); return;
        }
        
        int total = model.getModeloLista().getSize();
        if (total == 0) { System.out.println("    -> Lista vacía."); return; }

        // --- 2. CALCULAR ÍNDICE DEL BLOQUE ANTERIOR ---
        int actual = this.indiceOficialSeleccionado;
        // Si no hay selección, ir al índice 0
        // Si está en el 0, el bloque anterior es 0
        // Si está en otro, restar el salto
        int indiceDeseado = (actual == -1) ? 0 : actual - this.pageScrollIncrement;

        // 3. CLAMP (Limitar) al primer índice válido (0). No hacer wrap around.
        indiceDeseado = Math.max(0, indiceDeseado); // No pasar de 0

        // --- 4. LLAMAR AL HELPER SI EL ÍNDICE CAMBIÓ ---
        if (indiceDeseado != actual) {
            System.out.println("    -> Llamando a helper interno para procesar índice: " + indiceDeseado);
            seleccionarIndiceYActualizarUICompleta_Helper(indiceDeseado); // Llama al helper SIN manejo de flag
        } else {
            System.out.println("    -> No hay cambio de índice (ya está en el primer bloque o al inicio).");
            // Asegurar visibilidad si ya estaba al inicio
            asegurarVisibilidadAmbasListasSiVisibles(actual);
        }
        
        // --- 5. LOG FINAL ---
        System.out.println(">>> Coordinator: Fin seleccionarBloqueAnterior");
    } // --- FIN seleccionarBloqueAnterior ---	
	
    
    public synchronized boolean isSincronizandoUI() { // Getter
        return sincronizandoUI;
    }

    @Override
    public synchronized void setSincronizandoUI(boolean sincronizando) {
        System.out.println("    [Coordinator Flag] sincronizandoUI -> " + sincronizando);
        this.sincronizandoUI = sincronizando;
    } // --- Fin del método setSincronizandoUI ---

    /**
     * Navega directamente a un índice específico en la lista principal (listaNombres).
     * Valida el índice proporcionado antes de intentar cambiar la selección.
     * Si el índice es válido y diferente al actual, actualiza la selección
     * en la JList de nombres (registry.get("list.nombresArchivo")), lo que a su vez
     * disparará el ListSelectionListener para cargar la nueva imagen y sincronizar
     * la lista de miniaturas.
     *
     * @param index El índice del elemento (imagen) al que se desea navegar.
     *              Debe estar dentro del rango [0, tamañoLista - 1].
     */
    public void navegarAIndice(int index) {
        // 1. Validar dependencias y estado
        if (model == null || view == null || registry.get("list.nombresArchivo") == null || model.getModeloLista() == null) {
            System.err.println("WARN [navegarAIndice]: Modelo, Vista o ListaNombres no inicializados.");
            return;
        }

        DefaultListModel<String> modeloActual = model.getModeloLista();
        int totalImagenes = modeloActual.getSize();

        // 2. Validar el índice proporcionado
        if (modeloActual.isEmpty()) {
            System.out.println("[navegarAIndice] Lista vacía, no se puede navegar al índice " + index + ".");
            return; // No hay elementos
        }
        if (index < 0 || index >= totalImagenes) {
            System.err.println("WARN [navegarAIndice]: Índice solicitado (" + index + ") fuera de rango [0, " + (totalImagenes - 1) + "].");
            return; // Índice inválido
        }

     // 3. Obtener el componente JList y su índice actual desde el registro
        JList<String> listaNombres = registry.get("list.nombresArchivo");
        if (listaNombres == null) {
            System.err.println("ERROR [navegarAIndice]: El componente 'list.nombresArchivo' no se encontró en el registro.");
            return;
        }
        int indiceActual = listaNombres.getSelectedIndex();

        // 4. Actualizar selección en la Vista si el índice es diferente
        if (index != indiceActual) {
            System.out.println("[navegarAIndice] Navegando a índice: " + index);
            
            // Aquí es donde se podría simplificar. En lugar de manipular directamente la JList,
            // es mejor usar el método central del coordinador que ya se encarga de todo.
            // Esto asegura que se actualicen la imagen principal, las miniaturas y todo lo demás.
            seleccionarIndiceYActualizarUICompleta(index);

            // El código que tenías para 'ensureIndexIsVisible' ya está dentro de 
            // seleccionarIndiceYActualizarUICompleta -> sincronizarListaUI y actualizarModeloYVistaMiniaturas.
            // Así que ya no es necesario duplicarlo aquí. La llamada de arriba es suficiente.

        } else {
            System.out.println("[navegarAIndice] El índice solicitado (" + index + ") ya es el actual. No se hace nada.");
        } 
    }// --- FIN navegarAIndice ---
    
    
    /**
     * Imprime en la consola información detallada sobre un ActionEvent recibido.
     * Útil para depurar y entender qué componente/acción generó un evento.
     * Intenta obtener la clase de la fuente, el comando de acción, la clave larga
     * de configuración asociada (si se encuentra) y el nombre del icono (si es un botón).
     *
     * @param e El ActionEvent a analizar.
     */
    public void logActionInfo(ActionEvent e) {
        // 1. Validar el evento
        if (e == null) {
            System.out.println("--- Acción Detectada (Evento Nulo) ---");
            return; // No podemos hacer nada si el evento es nulo
        }

        // 2. Obtener información básica del evento
        Object source = e.getSource();
        String command = e.getActionCommand(); // El comando asociado al evento
        String sourceClass = (source != null) ? source.getClass().getSimpleName() : "null";

        // 3. Intentar obtener información adicional (clave larga, icono)
        //    Estos métodos helper buscan en los mapas de botones/menús de la vista.
        String longConfigKey = controller.findLongKeyForComponent(source); // Puede devolver null
        String iconName = controller.findIconNameForComponent(source);     // Puede devolver null

        // 4. Imprimir la información formateada en la consola
        System.out.println("--- Acción Detectada ---");
        System.out.println("  Fuente     : " + sourceClass + (source != null ? " (ID: "+ System.identityHashCode(source) +")" : "")); // Añadir ID objeto
        System.out.println("  Comando    : " + (command != null ? "'" + command + "'" : "null"));
        System.out.println("  Clave Larga: " + (longConfigKey != null ? "'" + longConfigKey + "'" : "(No encontrada)"));
        // Mostrar icono solo si se encontró
        if (iconName != null) {
             System.out.println("  Icono      : " + iconName);
        }
        // Opcional: Imprimir modificadores si son relevantes (Shift, Ctrl, etc.)
        // String modifiers = ActionEvent.getModifiersText(e.getModifiers());
        // if (!modifiers.isEmpty()) {
        //      System.out.println("  Modificadores: " + modifiers);
        // }
        System.out.println("-------------------------");
    
    } // --- FIN logActionInfo ---



    public synchronized int getIndiceOficialSeleccionado() {
        return this.indiceOficialSeleccionado;
    }

    
 // <<< NUEVO SETTER para ser llamado por AppInitializer
    public void setContextSensitiveActions(List<ContextSensitiveAction> actions) {
        this.contextSensitiveActions = (actions != null) ? actions : Collections.emptyList();
    }
    
    // NUEVO MÉTODO PRIVADO para actualizar todas las acciones contextuales
    private void actualizarEstadoDeTodasLasAccionesContextuales() {
        if (this.contextSensitiveActions != null && !this.contextSensitiveActions.isEmpty() && this.model != null) {
            // System.out.println("  [ListCoordinator] Actualizando estado de " + this.contextSensitiveActions.size() + " acciones contextuales...");
            for (ContextSensitiveAction action : this.contextSensitiveActions) {
                try {
                    action.updateEnabledState(this.model); // Pasa el modelo del ListCoordinator
                } catch (Exception ex) {
                    System.err.println("ERROR actualizando estado de Action " + action.getClass().getSimpleName() + ": " + ex.getMessage());
                    // Considera no detener el bucle por una acción fallida
                }
            }
        }
    }
    
    
    @Override
    public synchronized void forzarActualizacionEstadoAcciones() {
        System.out.println("  [ListCoordinator] Forzando actualización del estado 'enabled' de todas las acciones...");
        actualizarEstadoEnabledAccionesNavegacion();
        actualizarEstadoDeTodasLasAccionesContextuales();
        System.out.println("  [ListCoordinator] Actualización de estado 'enabled' de acciones completada.");
    } // --- Fin del método forzarActualizacionEstadoAcciones ---
    
    
 // --- INICIO DE LA MODIFICACIÓN: Setters para inyección de dependencias ---
    public void setModel(VisorModel model) {
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
    }

    public void setView(VisorView view) {
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null");
    }

    public void setController(VisorController controller) {
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null");
        // Una vez que tenemos el controlador, podemos obtener la configuración
        if (this.controller.getConfigurationManager() != null) {
            this.pageScrollIncrement = this.controller.getConfigurationManager().getInt(
                ConfigKeys.COMPORTAMIENTO_NAVEGACION_SALTO_BLOQUE, 10
            );
        }
    }

    public void setRegistry(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "ComponentRegistry no puede ser null");
    }
    
}    
    
