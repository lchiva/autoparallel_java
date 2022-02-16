package psc.java.autoparallel.testing;


import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


/**
 * Class use to create the configurationUI Page
 * @author Teillet & Capitanio
 *
 */
public class TestingCleanUpPage implements ICleanUpConfigurationUI {
	/**
	 *  Options of the environment
	 */
	private CleanUpOptions fOptions;
	

	@Override
	public int getCleanUpCount() {
		return 1;
	}
	
	@Override
	public int getSelectedCleanUpCount() {
		return fOptions.isEnabled("cleanup.graph_method") ? 1 : 0; //$NON-NLS-1$
	}
	@Override
	public void setOptions(CleanUpOptions options) {
		this.fOptions = options;
		
	}

	@Override
	public String getPreview() {
		StringBuffer buf = new StringBuffer();
		if(fOptions.isEnabled("cleanup.graph_method")) {
			buf.append("List<Integer> l = new ArrayList<>();\n")
			   .append("stream(l).forEach( l -> System.out.println(l) );\n"); 
		}else {
			buf.append("List<Integer> l = new ArrayList<>();\n")
			   .append("for(Integer i : l){\n") 
			   .append("\tSystem.out.println(i);\n")
			   .append("}\n");
		}
		return buf.toString();

	}
	@Override
	public Composite createContents(Composite parent) {
		//The thing that will contains all the element of our page
		final Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		//The check Button
		final Button b = new Button(c, SWT.CHECK);
		b.setText("Activer");
		
		//The default value of the check box
		if(fOptions.isEnabled("cleanup.graph_method")) b.setSelection(true);
		b.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {} // Useless but have to be here

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(b.getSelection()) {
					fOptions.setOption("cleanup.graph_method", CleanUpOptionsCore.TRUE);
				}else {
					fOptions.setOption("cleanup.graph_method", CleanUpOptionsCore.FALSE);
				}
			}
		});
		
		return c;
	}

}
