package psc.java.autoparallel.testing;

import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

public class TestingInitializer implements ICleanUpOptionsInitializer  {
	
	
	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		/**
		 * if we want our plug-in to be activate : CleanUpOptionsCore.TRUE
		 * else CleanUpOptionsCore.FALSE
		 */
		options.setOption("cleanup.graph_method", CleanUpOptionsCore.TRUE);
	}


}
