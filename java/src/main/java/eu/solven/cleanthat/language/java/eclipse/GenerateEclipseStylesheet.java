package eu.solven.cleanthat.language.java.eclipse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This helps generating a proper Eclipse Stylesheet, based on the existing codebase: it will generate a stylesheet
 * minifying impacts over the codebase (supposing the codebase is well formatted)
 * 
 * @author Benoit Lacelle
 *
 */
// Convert from Checkstyle
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/jobs/TransformCheckstyleRulesJob.java
public class GenerateEclipseStylesheet {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEclipseStylesheet.class);

	protected GenerateEclipseStylesheet() {
		// hidden
	}

	public static void main(String[] args) {
		LOGGER.info("TODO");
	}
}
