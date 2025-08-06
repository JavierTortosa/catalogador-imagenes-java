package vista.theme;

public interface ThemeChangeListener {
    /**
     * Se invoca cuando el tema de la aplicación ha cambiado.
     * @param nuevoTema El tema que acaba de ser aplicado.
     */
    void onThemeChanged(Tema nuevoTema);
} // --- FIN DE LA INTERFAZ ThemeChangeListener ---