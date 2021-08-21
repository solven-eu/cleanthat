package eu.solven.cleanthat.language;

import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.formatter.ILintFixer;

/**
 * Computed processors, to be applicable to any file of a given repository
 * 
 * @author Benoit Lacelle
 *
 */
public class LanguagePropertiesAndBuildProcessors {
	final List<Map.Entry<ILanguageProperties, ILintFixer>> languageProcessors;

	public LanguagePropertiesAndBuildProcessors(List<Map.Entry<ILanguageProperties, ILintFixer>> languageProcessors) {
		this.languageProcessors = languageProcessors;
	}

	public List<Map.Entry<ILanguageProperties, ILintFixer>> getProcessors() {
		return languageProcessors;
	}

}
