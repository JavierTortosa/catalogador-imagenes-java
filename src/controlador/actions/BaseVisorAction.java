package controlador.actions;

import javax.swing.AbstractAction;

import controlador.VisorController; // Importa el Controller

// Clase abstracta base para Actions que necesitan el Controller
public abstract class BaseVisorAction extends AbstractAction 
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected VisorController controller; // Referencia al controller

    // Constructor que recibe el nombre de la acción y el controller
    public BaseVisorAction(String name, VisorController controller) {
        super(name); // Llama al constructor de AbstractAction para el nombre
        if (controller == null) {
             throw new IllegalArgumentException("El controlador no puede ser nulo para BaseVisorAction");
        }
        this.controller = controller;
    }

    // Constructor que solo recibe el nombre (si no todas necesitan el controller)
     public BaseVisorAction(String name) {
         super(name);
         this.controller = null; // O lanzar excepción si siempre es requerido
     }

    // Puedes añadir métodos comunes aquí si es necesario
}

