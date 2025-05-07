package controlador; // o controlador.navegacion

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.JList; // Necesario para sincronizarListaUI
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities; // Para invokeLater en sincronizarListaUI

import modelo.VisorModel;
import vista.VisorView;

public class ListCoordinator {

    // --- Referencias a otros componentes ---
    private final VisorModel model;
    private final VisorView view;
    private final VisorController controller; // Para delegar carga de imagen
   
    // --- Estado Interno ---
    private int indiceOficialSeleccionado = -1; // Índice "maestro" gestionado aquí
    private boolean sincronizandoUI = false; // Flag para evitar bucles al sincronizar

    //TODO se podria especificar en el config.cfg
    // Define el tamaño del salto para Page Up/Down (ajustable)
    private static final int TAMANO_SALTO_BLOQUE = 10; // Salta 10 ítems, por ejemplo
    
    /**
     * Constructor.
     * @param model El modelo de datos.
     * @param view La vista principal.
     * @param controller El controlador principal.
     */
    public ListCoordinator(VisorModel model, VisorView view, VisorController controller) {
        // Validar que las referencias no sean nulas
        this.model = Objects.requireNonNull(model, "VisorModel no puede ser null");
        this.view = Objects.requireNonNull(view, "VisorView no puede ser null");
        this.controller = Objects.requireNonNull(controller, "VisorController no puede ser null");
        
        
        System.out.println("[ListCoordinator] Instancia creada.");
    } //--- FIN ListCoordinator
    
    

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
        if (indiceDeseado < -1 || // -1 es válido para deselección
            (indiceDeseado >= 0 && indiceDeseado >= model.getModeloLista().getSize()))
        {
            System.err.println("    -> Índice inválido. Ignorando selección.");
            return; // Índice fuera de rango
        }

        // 2. Evitar Trabajo Redundante/Bucles
        if (indiceDeseado == this.indiceOficialSeleccionado) {
            System.out.println("    -> Índice deseado ya es el oficial. No se hace nada.");
            // Asegurar visibilidad por si acaso (ej. al refrescar)
             asegurarVisibilidadAmbasListasSiVisibles(indiceDeseado);
            return;
        }
        
        // 3. Evitar bucles si ya estamos sincronizando programáticamente
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
             // Actualizar clave en el modelo ANTES de pedir carga
             model.setSelectedImageKey(claveSeleccionada);
             // Pedir al controller que maneje la carga y visualización
             controller.actualizarImagenPrincipal(indiceDeseado); // Controller llamará a mostrar...
        } else {
             // Si el índice es -1 (deselección), limpiar la imagen
             model.setSelectedImageKey(null);
             controller.limpiarUI(); // Limpiar UI completamente
        }

        // 6. Sincronizar UI de las Listas (Selección y Visibilidad)
        sincronizarListaUI(view.getListaNombres(), indiceDeseado);
        // Para miniaturas, llamamos al método del controller que maneja la ventana deslizante

        if (indiceDeseado != -1) {
             controller.actualizarModeloYVistaMiniaturas(indiceDeseado);
        } else {
             // Si deseleccionamos, limpiar también el modelo de miniaturas
             if (controller.getModeloMiniaturas() != null) controller.getModeloMiniaturas().clear(); // Acceso directo (quizás necesita getter)
             if (view.getListaMiniaturas() != null) view.getListaMiniaturas().repaint();
        }


        
        System.out.println("  [ListCoordinator] Selección procesada para índice: " + indiceDeseado);
        
    } // --- FIN seleccionarImagenPorIndice

    
    /**
     * Selecciona la imagen anterior en la lista principal.
     * Si la imagen actual es la primera, va a la última (wrap around).
     * MODIFICADO: Llama al helper interno.
     */
    public synchronized void seleccionarAnterior() {
        System.out.println(">>> Coordinator: Navegación -> Anterior (Actual Oficial: " + indiceOficialSeleccionado + ")");
         if (model == null || model.getModeloLista() == null) {
              System.err.println("ERROR [seleccionarAnterior]: Modelo no disponible."); return;
         }
         int total = model.getModeloLista().getSize();
         if (total == 0) { System.out.println("    -> Lista vacía."); return; }

         int actual = this.indiceOficialSeleccionado;
         int anterior = (actual == -1) ? total - 1 : actual - 1; // Si no hay selección, ir al último
         if (anterior < 0) { anterior = total - 1; } // Wrap around

         if (anterior != actual) {
             System.out.println("    -> Llamando a helper interno para procesar índice: " + anterior);
             seleccionarIndiceYActualizarUICompleta_Helper(anterior); // <-- LLAMAR AL HELPER
         } else {
             System.out.println("    -> No hay cambio de índice.");
             // Asegurar visibilidad si solo hay un elemento
             if(total == 1) asegurarVisibilidadAmbasListasSiVisibles(actual);
         }
         System.out.println(">>> Coordinator: Fin seleccionarAnterior");
    } // --- FIN seleccionarAnterior ---
    
    
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
     * Asegura que el índice especificado sea visible en ambas JList
     * (listaNombres y listaMiniaturas), si los componentes son actualmente visibles.
     * Es seguro llamar desde cualquier hilo, ya que usa invokeLater internamente si es necesario.
     *
     * @param indice El índice (0-based, relativo al modelo PRINCIPAL) a hacer visible.
     */
     private void asegurarVisibilidadAmbasListasSiVisibles(final int indice) 
     {
         // --- 1. VALIDACIÓN INICIAL ---
         
    	 // 1.1. Comprobar si el índice es válido (-1 no necesita visibilidad).
         if (indice < 0) {
              System.out.println("    [Asegurar Visibilidad] Índice -1, no se hace nada.");
              return;
         }
         
         // 1.2. Log informativo
         System.out.println("    [Asegurar Visibilidad] Solicitud para asegurar visibilidad del índice principal: " + indice);

         // --- 2. EJECUCIÓN EN EDT ---
         
         // 2.1. Usar invokeLater para asegurar que se ejecuta en el hilo de eventos.
         if (!SwingUtilities.isEventDispatchThread()) {
             SwingUtilities.invokeLater(() -> asegurarVisibilidadAmbasListasSiVisibles(indice));
             return;
         }

         // --- 3. LÓGICA DENTRO DEL EDT ---
         
         // 3.1. Validar que la vista exista
         if (view == null) {
             System.err.println("ERROR [Asegurar Visibilidad EDT]: Vista es null.");
             return;
         }

         // 3.2. Asegurar visibilidad en listaNombres
         
         // 3.2.1. Obtener referencias a la lista y su panel contenedor
         JList<String> listaNom = view.getListaNombres();
         JPanel pIzq = view.getPanelIzquierdo();

         // 3.2.2. Comprobar si la lista y el panel existen y son visibles
         if (listaNom != null && pIzq != null && pIzq.isShowing()) { // Usar isShowing() es más robusto
         
        	 // 3.2.3. Obtener modelo actual de la lista de nombres
             ListModel<?> modelNom = listaNom.getModel();
             
             // 3.2.4. Validar índice contra el tamaño del modelo actual
             if (modelNom != null && indice < modelNom.getSize()) {
             
            	 // 3.2.5. Log y llamada a ensureIndexIsVisible
                 System.out.println("      -> Asegurando visibilidad en Nombres para índice " + indice);
                 try {
                     listaNom.ensureIndexIsVisible(indice);
                 } catch (Exception ex) {
                     System.err.println("ERROR [Asegurar Visibilidad Nombres EDT] para índice " + indice + ": " + ex.getMessage());
                 }
             } else {
                 System.out.println("WARN [Asegurar Visibilidad Nombres EDT]: Índice " + indice + " fuera de rango para modelo Nombres.");
             }
         } else {
             System.out.println("      -> Omitiendo visibilidad Nombres (lista/panel nulo o no visible).");
         }

         // 3.3. Asegurar visibilidad en listaMiniaturas (más complejo)
         
         // 3.3.1. Obtener referencias a la lista de miniaturas y su scrollpane
         JList<String> listaMin = view.getListaMiniaturas();
         JScrollPane scrollMinis = view.getScrollListaMiniaturas();

         // 3.3.2. Comprobar si la lista y el scrollpane existen y son visibles
         if (listaMin != null && scrollMinis != null && scrollMinis.isShowing()) { // Usar isShowing()
         
        	 // 3.3.3. Obtener modelo actual de la lista de miniaturas
             ListModel<String> modelMin = listaMin.getModel();
             
             // 3.3.4. TRADUCIR el índice principal al índice RELATIVO en el modelo de miniaturas
             int indiceRelativo = -1;
             if (model != null && model.getModeloLista() != null && indice < model.getModeloLista().getSize() && modelMin != null) {
             
            	 // 3.3.4.1. Obtener la clave del modelo principal
                 String clavePrincipal = model.getModeloLista().getElementAt(indice);
                 
                 // 3.3.4.2. Buscar esa clave en el modelo de miniaturas actual
                 if (clavePrincipal != null) {
                     for (int i = 0; i < modelMin.getSize(); i++) {
                         String claveMini = modelMin.getElementAt(i);
                         if (clavePrincipal.equals(claveMini)) {
                             indiceRelativo = i;
                             break; // Encontrado
                         }
                     }
                 }
             }

             // 3.3.5. Si se encontró un índice relativo válido
             if (indiceRelativo != -1) {
                 
            	 // 3.3.6. Log y llamada a ensureIndexIsVisible con el índice RELATIVO
                 System.out.println("      -> Asegurando visibilidad en Miniaturas para índice relativo " + indiceRelativo + " (Principal " + indice + ")");
                 try {
                     listaMin.ensureIndexIsVisible(indiceRelativo);
                 } catch (Exception ex) {
                     System.err.println("ERROR [Asegurar Visibilidad Miniaturas EDT] para índice relativo " + indiceRelativo + ": " + ex.getMessage());
                 }
             } else {
                 
            	 // 3.3.7. Log si no se encontró el índice relativo (la clave no está en la ventana actual)
                 System.out.println("WARN [Asegurar Visibilidad Miniaturas EDT]: No se encontró índice relativo para índice principal " + indice);
             }
         } else {
              System.out.println("      -> Omitiendo visibilidad Miniaturas (lista/scroll nulo o no visible).");
         }

         // 3.4. Log final
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
    public synchronized void seleccionarIndiceYActualizarUICompleta(int indice) 
    {
        // --- 1. LOG INICIAL Y VALIDACIÓN PREVIA ---
        System.out.println(">>> Coordinator: Solicitud para procesar índice: " + indice + " (Actual Oficial: " + indiceOficialSeleccionado + ")");
    
        // 1.1. Validar índice contra el modelo principal
        int total = (model != null && model.getModeloLista() != null) ? model.getModeloLista().getSize() : 0;
        
        if (indice < -1 || (indice >= 0 && total == 0) || (indice >= total && total > 0)) {
             System.err.println("    -> Índice inválido (" + indice + ") para tamaño de lista (" + total + "). Ignorando.");
             return;
        }
        
        // 1.2. Comprobar si el índice deseado es el mismo que ya está seleccionado oficialmente
        if (indice == this.indiceOficialSeleccionado) {
            System.out.println("    -> Ignorando (mismo índice ya seleccionado oficialmente).");
            
            // Si el índice no cambió, no necesitamos asegurar visibilidad aquí necesariamente,
            // Si la llamada vino de un clic repetido, la UI ya está donde debe.
            //FIXME estudiar por si se refresca la pantalla 
            // Si vino de refresh, tal vez sí sería útil, pero complica la lógica.
            
            return;
        }
        // 1.3. Comprobar si ya hay una operación de sincronización en curso
        if (sincronizandoUI) {
//            System.out.println("    -> Ignorando (operación de sincronización ya en curso).");
            return;
        }

        // --- 2. ACTIVAR FLAG DE SINCRONIZACIÓN ---
        setSincronizandoUI(true); // Activar ANTES de cualquier cambio

        // --- 3. BLOQUE PRINCIPAL DE PROCESAMIENTO (TRY-FINALLY) ---
        try {
        	
            // 3.1. PROCESAR SELECCIÓN INTERNA
            //      Actualiza modelo (índice, clave) y solicita carga/limpieza de imagen principal.
            boolean cambio = seleccionarImagenInterno(indice);

            // 3.2. ACTUALIZAR VISTAS (SI HUBO CAMBIO)
            if (cambio) {
//                System.out.println("      -> [UI Completa] Actualizando UI (Nombres y Miniaturas) para índice " + indice);

                // 3.2.1. Sincronizar la JList de Nombres VISUALMENTE
                //         (Llama a setSelectedIndex/ensureIndexIsVisible internamente)
                //         El listener será ignorado por el flag.
                sincronizarListaUI(view.getListaNombres(), indice);

                // 3.2.2. Sincronizar/Actualizar la JList de Miniaturas
                //         (Reconstruye modelo ventana deslizante y actualiza JList)
                //         El listener será ignorado por el flag.
                if (controller != null) {
                    controller.actualizarModeloYVistaMiniaturas(indice); // <-- ESTO PUEDE CORROMPER EL SHARED SELECTION MODEL
                } else {
                     System.err.println("ERROR CRÍTICO [UI Completa]: Controller es null. No se pueden actualizar miniaturas.");
                }

                // 3.2.3. RESTAURAR EL ÍNDICE CORRECTO EN EL SHARED SELECTION MODEL
                //        Después de que actualizarModeloYVistaMiniaturas potencialmente lo
                //        cambiara con un índice relativo, lo forzamos de vuelta al
                //        índice principal correcto. Hacemos esto ANTES de liberar el flag.
                if (indice != -1 && view != null && view.getListaNombres() != null) { // Solo si el índice es válido y listaNombres existe
                     // Obtener el modelo de selección compartido (desde listaNombres es seguro)
                     javax.swing.ListSelectionModel sharedSelectionModel = view.getListaNombres().getSelectionModel();
                     
                     if (sharedSelectionModel != null) {
                          // Comprobar si el índice actual del shared model es diferente al oficial

                    	 if (sharedSelectionModel.getLeadSelectionIndex() != this.indiceOficialSeleccionado) {
//                              System.out.println("        -> [CORRECCIÓN] Restaurando sharedSelectionModel al índice principal oficial: " + this.indiceOficialSeleccionado);
                              // Establecer el intervalo de selección al índice oficial correcto
                              // Esto también podría disparar el listener, pero el flag aún está activo.
                              sharedSelectionModel.setSelectionInterval(this.indiceOficialSeleccionado, this.indiceOficialSeleccionado);
                          } else {
                              // Ya estaba correcto, no es necesario setSelectionInterval que podría disparar eventos extra
                              System.out.println("        -> [CORRECCIÓN] sharedSelectionModel ya estaba en el índice correcto: " + this.indiceOficialSeleccionado);
                          }
                     } else { 
                    	 System.err.println("ERROR CRÍTICO [UI Completa]: No se pudo obtener sharedSelectionModel para corrección."); 
                     }
                } else if (indice == -1 && view != null && view.getListaNombres() != null){
                     // Si la selección fue -1 (limpiar), asegurarse de que el shared model también lo esté.
                     javax.swing.ListSelectionModel sharedSelectionModel = view.getListaNombres().getSelectionModel();
                     if (sharedSelectionModel != null && !sharedSelectionModel.isSelectionEmpty()) 
                     {
//                          System.out.println("        -> [CORRECCIÓN] Limpiando sharedSelectionModel (-1).");
                          sharedSelectionModel.clearSelection();
                     }
                }
                
                // 3.2.4. La llamada a ensureIndexIsVisible ahora está DENTRO de sincronizarListaUI y actualizarModeloYVistaMiniaturas
                //        No necesitamos una llamada adicional aquí.

            } else {
                 //System.out.println("      -> [UI Completa] No hubo cambio interno, no se actualiza UI adicionalmente.");
                 // No es necesario asegurar visibilidad aquí si no hubo cambio.
            }
        } finally {
            // --- 4. DESACTIVAR FLAG DE SINCRONIZACIÓN ---
            SwingUtilities.invokeLater(() -> setSincronizandoUI(false));
        } // Fin del bloque try-finally

        // --- 5. LOG FINAL DEL MÉTODO ---
        System.out.println(">>> Coordinator: Fin seleccionarIndiceYActualizarUICompleta(" + indice + ")");

    } // --- FIN seleccionarIndiceYActualizarUICompleta ---    
    
    
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
             System.out.println("    -> Delegando limpieza de UI al Controller (índice -1)");
             controller.limpiarUI();
        }

        // --- YA NO SINCRONIZAMOS UI AQUÍ ---
        // La sincronización se hace en los métodos públicos externos

        System.out.println("  [ListCoordinator Interno] Procesamiento interno finalizado para índice: " + indiceDeseado);
        return true; // Hubo cambio
    }
    
    
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
     *    b) Llama a `controller.actualizarModeloYVistaMiniaturas` para reconstruir
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
            sincronizarListaUI(view.getListaNombres(), this.indiceOficialSeleccionado);

            // 2.4. Sincronizar/Actualizar la JList de Miniaturas.
            // 2.4.1. Verificar que el controlador exista.
            if (controller != null) {
                // 2.4.2. Llamar al método del controlador que reconstruye el modelo de la
                //         ventana deslizante de miniaturas y actualiza la JList.
                //         Se le pasa el índice oficial que AHORA tiene el coordinador.
                //         Este método internamente también llamará a `sincronizarListaUI`
                //         para las miniaturas, que gestionará su propio flag.
                controller.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado);
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
        // --- 1. VALIDACIÓN INICIAL ---
        // 1.1. Comprobar si la referencia a la JList es válida.
        if (lista == null) {
            System.err.println("ERROR [Sync UI Interno]: La JList proporcionada es null. No se puede sincronizar.");
            return; // Salir si no hay lista
        }

        // 1.2. Determinar el nombre de la lista para logs
        String nombreListaDeterminado = "Desconocida"; // Variable temporal
        if (view != null) { // Solo si la vista existe
            if (lista == view.getListaNombres()) {
                 nombreListaDeterminado = "Nombres";
            } else if (lista == view.getListaMiniaturas()) {
                 nombreListaDeterminado = "Miniaturas";
            }
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
    
    
    /**
     * Punto de entrada para procesar una selección realizada por el usuario
     * directamente en la lista de nombres (`listaNombres`).
     *
     * Se encarga de:
     * 1. Validar la solicitud (evitar procesar si el índice no cambió).
     * 2. Llamar a `seleccionarImagenInterno` para actualizar el estado del modelo y la imagen principal.
     * 3. Llamar a `controller.actualizarModeloYVistaMiniaturas` para actualizar la ventana deslizante de miniaturas.
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
                System.out.println("    -> Llamando a controller.actualizarModeloYVistaMiniaturas(" + this.indiceOficialSeleccionado + ")");

                // 2.2.3. Llamar al método del controlador que reconstruye el modelo de la
                //         ventana deslizante de miniaturas y actualiza la JList correspondiente.
                //         Este método internamente llamará a `sincronizarListaUI` para las miniaturas,
                //         el cual activará/desactivará el flag.
                controller.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado);
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
     * Selecciona la siguiente imagen en la lista principal.
     * Si la imagen actual es la última, vuelve al principio (wrap around).
     * MODIFICADO: Llama al helper interno.
     */
    public synchronized void seleccionarSiguiente() {
        System.out.println(">>> Coordinator: Navegación -> Siguiente (Actual Oficial: " + indiceOficialSeleccionado + ")");
        if (model == null || model.getModeloLista() == null) {
             System.err.println("ERROR [seleccionarSiguiente]: Modelo no disponible."); return;
        }
        int total = model.getModeloLista().getSize();
        if (total == 0) { System.out.println("    -> Lista vacía."); return; }

        int actual = this.indiceOficialSeleccionado;
        int siguiente = (actual == -1) ? 0 : actual + 1;
        if (siguiente >= total) { siguiente = 0; } // Wrap around

        if (siguiente != actual) {
            System.out.println("    -> Llamando a helper interno para procesar índice: " + siguiente);
            seleccionarIndiceYActualizarUICompleta_Helper(siguiente); // <-- LLAMAR AL HELPER
        } else {
            System.out.println("    -> No hay cambio de índice.");
            // Asegurar visibilidad si solo hay un elemento
            if(total == 1) asegurarVisibilidadAmbasListasSiVisibles(actual);
        }
        System.out.println(">>> Coordinator: Fin seleccionarSiguiente");
    } // --- FIN seleccionarSiguiente ---
    
    
    /**
     * Punto de entrada para procesar una selección realizada por el usuario
     * directamente en la lista de miniaturas (`listaMiniaturas`).
     *
     * Se encarga de:
     * 1. Validar la solicitud (evitar procesar si el índice no cambió).
     * 2. Llamar a `seleccionarImagenInterno` para actualizar el estado del modelo y la imagen principal.
     * 3. Si hubo cambio, llamar a `sincronizarListaUI` para actualizar la selección visual en `listaNombres`.
     * 4. (Opcional pero recomendado) Llamar a `controller.actualizarModeloYVistaMiniaturas` para reajustar
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
            sincronizarListaUI(view.getListaNombres(), this.indiceOficialSeleccionado); // Usa el índice oficial actualizado

            // 2.2.2. (Opcional pero recomendado) Re-Actualizar la Ventana Deslizante de Miniaturas.
            //          Aunque la selección vino de las miniaturas, el rango visible podría necesitar
            //          ajustarse si la selección estaba cerca de los bordes del rango anterior.
            //          Verificar que el controlador esté disponible.
            if (controller != null) {
            
            	// 2.2.2.1. Log indicando la llamada.
                System.out.println("    -> (Re)Llamando a controller.actualizarModeloYVistaMiniaturas(" + this.indiceOficialSeleccionado + ") por seguridad de rango.");
                
                // 2.2.2.2. Llamar al método del controlador. Este método internamente
                //          llamará a `sincronizarListaUI` para las miniaturas, que gestionará su flag.
                controller.actualizarModeloYVistaMiniaturas(this.indiceOficialSeleccionado); // Usa el índice oficial actualizado
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
        int indiceDeseado = (actual == -1) ? TAMANO_SALTO_BLOQUE : actual + TAMANO_SALTO_BLOQUE;

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
        int indiceDeseado = (actual == -1) ? 0 : actual - TAMANO_SALTO_BLOQUE;

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

    
    //FIXME hacer este metodo private
    synchronized void setSincronizandoUI(boolean sincronizando) { // Setter
         System.out.println("    [Coordinator Flag] sincronizandoUI -> " + sincronizando); // Log útil
        this.sincronizandoUI = sincronizando;
    }

    
    /**
     * Navega directamente a un índice específico en la lista principal (listaNombres).
     * Valida el índice proporcionado antes de intentar cambiar la selección.
     * Si el índice es válido y diferente al actual, actualiza la selección
     * en la JList de nombres (view.getListaNombres()), lo que a su vez
     * disparará el ListSelectionListener para cargar la nueva imagen y sincronizar
     * la lista de miniaturas.
     *
     * @param index El índice del elemento (imagen) al que se desea navegar.
     *              Debe estar dentro del rango [0, tamañoLista - 1].
     */
    public void navegarAIndice(int index) {
        // 1. Validar dependencias y estado
        if (model == null || view == null || view.getListaNombres() == null || model.getModeloLista() == null) {
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

        // 3. Obtener índice actual y comparar
        int indiceActual = view.getListaNombres().getSelectedIndex();

        // 4. Actualizar selección en la Vista si el índice es diferente
        if (index != indiceActual) {
            System.out.println("[navegarAIndice] Navegando a índice: " + index);
            view.getListaNombres().setSelectedIndex(index);

             JPanel pIzq = view.getPanelIzquierdo();
             if(pIzq != null && pIzq.isVisible()) {
                  view.getListaNombres().ensureIndexIsVisible(index);
             }
             // Asegurar visibilidad en la lista de miniaturas si es visible
             JScrollPane scrollMinis = view.getScrollListaMiniaturas();
             JList<String> listaMinis = view.getListaMiniaturas();
             if (scrollMinis != null && scrollMinis.isVisible() && listaMinis != null) {
                  listaMinis.ensureIndexIsVisible(index);
             }

        } else {
            System.out.println("[navegarAIndice] El índice solicitado (" + index + ") ya es el actual. No se hace nada.");
        }

    } // --- FIN navegarAIndice ---
    
    
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
    
}    
    
/*
  NOTAS A TENER EN CUENTA
  
Acceso setSincronizandoUI (Reiteración): Como mencioné, hacerlo private sería lo ideal para el encapsulamiento, ya que solo sincronizarListaUI necesita llamarlo. Pero como está ahora (package-private) funciona porque ambos métodos están en la misma clase.
 
Llamadas a asegurarVisibilidadAmbasListasSiVisibles:
En los métodos de navegación (seleccionarSiguiente, etc.) y selección (seleccionarDesdeNombres, etc.), llamas a asegurarVisibilidadAmbasListasSiVisibles solo si el índice no cambió (if (indiceDeseado == this.indiceOficialSeleccionado) o if (indiceDeseado != actual) -> else).
En el helper seleccionarIndiceYActualizarUICompleta_Helper, la llamada a asegurarVisibilidadAmbasListasSiVisibles está dentro del bloque else (cuando cambio es false).
Consideración: ¿Debería asegurarse la visibilidad siempre al final de una operación de selección/navegación, incluso si el índice cambió? Los métodos sincronizarListaUI y actualizarModeloYVistaMiniaturas ya llaman a ensureIndexIsVisible internamente. Quizás las llamadas explícitas a asegurarVisibilidadAmbasListasSiVisibles son redundantes. Podrías probar a quitarlas y ver si el comportamiento sigue siendo correcto (el scroll debería seguir funcionando debido a las llamadas internas). Si decides mantenerlas, no hacen daño, solo añaden una capa extra de seguridad.

Llamada redundante en seleccionarDesdeMiniaturas: Dentro de seleccionarDesdeMiniaturas, si cambioRealizado es true, llamas a sincronizarListaUI(view.getListaNombres(), ...) y también a controller.actualizarModeloYVistaMiniaturas(...). La segunda llamada internamente actualizará la selección y visibilidad de las miniaturas. ¿Es necesaria la segunda llamada siempre, o solo si el rango realmente necesita ajuste? Podría ser una pequeña optimización investigar si se puede evitar esa segunda llamada en algunos casos, aunque mantenerla asegura que la ventana 7+1+7 siempre esté centrada correctamente.

navegarAIndice: Este método aún llama directamente a view.getListaNombres().setSelectedIndex(index) y a ensureIndexIsVisible. Para mayor consistencia, podrías refactorizarlo para que llame a seleccionarIndiceYActualizarUICompleta_Helper(index), como hacen los otros métodos de navegación.
*/
