package eu.solven.cleanthat.language.java.rules.meta;

/**
 * Helps understand why a rule is relevant, given other systems implementing the rule
 *
 * @author Benoit Lacelle
 */
public interface IRuleExternalUrls {

	default String sonarUrl() {
		return "";
	}

	default String pmdUrl() {
		return "";
	}

	default String checkstyleUrl() {
		return "";
	}

	default String jsparrowUrl() {
		return "";
	}
}
