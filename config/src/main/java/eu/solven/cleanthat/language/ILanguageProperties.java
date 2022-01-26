package eu.solven.cleanthat.language;

import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.config.ISkippable;
import eu.solven.cleanthat.github.IHasSourceCodeProperties;

/**
 * The common configuration on a per-language basis
 *
 * @author Benoit Lacelle
 */
public interface ILanguageProperties extends ISkippable, IHasSourceCodeProperties {

	String getLanguage();

	String getLanguageVersion();

	List<Map<String, ?>> getProcessors();
}
