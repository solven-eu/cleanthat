package eu.solven.cleanthat.language;

import java.util.List;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * The common configuration on a per-language basis
 *
 * @author Benoit Lacelle
 */
public interface ISourceCodeProperties {

	List<String> getExcludes();

	List<String> getIncludes();

	String getEncoding();

	LineEnding getLineEndingAsEnum();
}
