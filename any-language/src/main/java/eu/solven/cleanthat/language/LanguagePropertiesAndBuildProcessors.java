package eu.solven.cleanthat.language;

import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;

/**
 * Computed processors, to be applicable to any file of a given repository
 * 
 * @author Benoit Lacelle
 *
 */
public class LanguagePropertiesAndBuildProcessors {
	final List<Map.Entry<ILanguageProperties, ISourceCodeFormatter>> languageProcessors;

	public LanguagePropertiesAndBuildProcessors(
			List<Map.Entry<ILanguageProperties, ISourceCodeFormatter>> languageProcessors) {
		this.languageProcessors = languageProcessors;
	}

	public List<Map.Entry<ILanguageProperties, ISourceCodeFormatter>> getProcessors() {
		return languageProcessors;
	}

}
