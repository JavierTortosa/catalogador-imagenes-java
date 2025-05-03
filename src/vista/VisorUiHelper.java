package vista;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class VisorUiHelper extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public void ventanaDeMensaje (String mensaje, String tipo)
	{
		String tipoM = tipo.toUpperCase();
		int tipoMensaje=0;
		
		if (tipoM.equals("error".toUpperCase())) tipoMensaje=JOptionPane.ERROR_MESSAGE;
		if (tipoM.equals("info".toUpperCase())) tipoMensaje=JOptionPane.INFORMATION_MESSAGE;
		
		JOptionPane.showMessageDialog (this, mensaje, tipo.toUpperCase(), tipoMensaje);
		
	}
	
	
	
	
}
