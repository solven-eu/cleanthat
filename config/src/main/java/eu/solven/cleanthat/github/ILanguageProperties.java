package eu.solven.cleanthat.github;

import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * The common configuration on a per-language basis
 * 
 * @author Benoit Lacelle
 *
 */
public interface ILanguageProperties {

	List<String> getExcludes();

	List<String> getIncludes();

	String getEncoding();

	LineEnding getLineEnding();

	List<Map<String, ?>> getProcessors();

}
