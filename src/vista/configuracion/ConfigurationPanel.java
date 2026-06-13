package vista.configuracion;

import servicios.ConfigurationManager;

public interface ConfigurationPanel {

    void load(ConfigurationManager config);

    boolean save(ConfigurationManager config);

    boolean isModified();

    String getTitle();

}
