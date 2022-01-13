package eu.solven.cleanthat.language;

import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.config.ISkippable;

/**
 * The common configuration on a per-language basis
 *
 * @author Benoit Lacelle
 */
public interface ILanguageProperties extends ISkippable {

    ISourceCodeProperties getSourceCode();

    String getLanguage();

    String getLanguageVersion();

    List<Map<String, ?>> getProcessors();
}
