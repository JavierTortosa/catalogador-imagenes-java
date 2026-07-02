package controlador.services;

import controlador.managers.FolderNavigationManager;
import controlador.managers.tree.FolderTreeManager;

/**
 * Servicio encargado de la navegación por el árbol y el historial de carpetas.
 * El GeneralController ya no necesita conocer cómo se navega.
 */
public class NavigationService {
    
    private final FolderNavigationManager folderNavManager;
    private final FolderTreeManager folderTreeManager;

    public NavigationService(FolderNavigationManager folderNavManager, FolderTreeManager folderTreeManager) {
        this.folderNavManager = folderNavManager;
        this.folderTreeManager = folderTreeManager;
    }

    public void navegarCarpetaAnterior() {
        if (folderNavManager != null) {
            folderNavManager.navegarACarpetaPadre();
        }
    }

    public void navegarCarpetaSiguiente() {
        if (folderNavManager != null) {
            folderNavManager.entrarEnSubcarpeta();
        }
    }

    public void navegarRaiz() {
        if (folderNavManager != null) {
            folderNavManager.volverACarpetaRaiz();
        }
    }

    public void salirDeSubcarpeta() {
        if (folderNavManager != null) {
            folderNavManager.salirDeSubcarpetaConHistorial();
        }
    }

    public void abrirCarpetaDesdeArbol() {
        if (folderTreeManager != null) {
            folderTreeManager.handleOpenFolderAction();
        }
    }

    public void entrarEnCarpetaDesdeArbol() {
        if (folderTreeManager != null) {
            folderTreeManager.handleDrillDownFolderAction();
        }
    }
}