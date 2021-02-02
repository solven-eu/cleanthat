package eu.solven.cleanthat.rules;

import eu.solven.cleanthat.rules.framework.ATodoJavaParserRule;
import eu.solven.cleanthat.rules.meta.IRuleDescriber;
import eu.solven.cleanthat.rules.meta.IRuleExternalUrls;

/**
 * Clean empty statements (e.g. 2 consecutive ';')
 * 
 * @author Benoit Lacelle
 *
 */
public class EmptyStatement extends ATodoJavaParserRule implements IRuleExternalUrls, IRuleDescriber {

	@Override
	public String getId() {
		// CheckStyle
		return "EmptyStatement";
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html";
	}
}
