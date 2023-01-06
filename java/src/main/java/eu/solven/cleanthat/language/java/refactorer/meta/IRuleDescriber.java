package eu.solven.cleanthat.language.java.refactorer.meta;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.mutators.NumberToValueOf;
import eu.solven.cleanthat.language.java.refactorer.mutators.UseIsEmptyOnCollections;

/**
 * Helpers knowing what could be the impact of given rule
 *
 * @author Benoit Lacelle
 */
public interface IRuleDescriber {

	/**
	 * 
	 * @return true if the rule helps cleaning deprecation notice
	 * 
	 * @see NumberToValueOf
	 */
	default boolean isDeprecationNotice() {
		return false;
	}

	/**
	 * 
	 * @return true if the rule helps improving performances
	 * 
	 * @see UseIsEmptyOnCollections
	 */
	// Relates to https://eslint.org/docs/user-guide/command-line-interface#--fix-type
	default boolean isPerformanceImprovment() {
		return false;
	}

	/**
	 * 
	 * This kind of rules may not fit everybody, as in some cases, exceptions are a feature (even if probably a bad
	 * thing).
	 * 
	 * @return true if the rule helps preventing exceptions.
	 * 
	 * @see UseIsEmptyOnCollections
	 */
	default boolean isPreventingExceptions() {
		return false;
	}

	/**
	 * @return the minimal JDKF version for this rule to trigger
	 */
	default String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}
}
