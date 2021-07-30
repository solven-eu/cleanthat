package eu.solven.cleanthat.language;

import java.util.List;
import java.util.Map;

/**
 * The common configuration on a per-language basis
 *
 * @author Benoit Lacelle
 */
public interface ILanguageProperties {

	ISourceCodeProperties getSourceCodeProperties();

	String getLanguage();

	String getLanguageVersion();

	List<Map<String, ?>> getProcessors();
}
