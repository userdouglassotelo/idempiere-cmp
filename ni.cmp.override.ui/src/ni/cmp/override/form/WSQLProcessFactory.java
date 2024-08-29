


package ni.cmp.override.form;

import java.util.logging.Level;

import org.adempiere.webui.factory.DefaultFormFactory;
import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.util.CLogger;

public class WSQLProcessFactory implements IFormFactory {

	private final CLogger log = CLogger.getCLogger(DefaultFormFactory.class);

	@Override
	public ADForm newFormInstance(String formName)
	{

		if (formName.startsWith("ni.cmp.override.form")) 
		{

			Object form = null;
			Class<?> clazz = null;
			ClassLoader loader = Thread.currentThread().getContextClassLoader();

			if (loader != null) {
				try {
					clazz = loader.loadClass(formName);
				} catch (Exception e) {
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, e.getLocalizedMessage(), e);
				}
			}

			if (clazz == null) {
		
				loader = this.getClass().getClassLoader();
				
				try {
					// Create instance w/o parameters
				
					clazz = loader.loadClass(formName);
				
				} catch (Exception e) {
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, e.getLocalizedMessage(), e);
				}
			}

			if (clazz != null) {
				try {
					form = clazz.newInstance();
				} catch (Exception e) {
					if (log.isLoggable(Level.WARNING))
						log.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}

			if (form != null) {
				if (form instanceof ADForm) {
					return (ADForm) form;
				} else if (form instanceof IFormController) {
					IFormController controller = (IFormController) form;
					ADForm adForm = controller.getForm();
					adForm.setICustomForm(controller);
					return adForm;
				}
			}

		}

		if (log.isLoggable(Level.INFO))
			log.info(formName + " not found at extension registry and classpath");

		return null;
	}

}
