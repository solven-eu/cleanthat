package eu.solven.cleanthat.language.java;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatterProcessorProperties;
import lombok.EqualsAndHashCode;

/**
 * Used as cache Key for Eclipse configuration
 * 
 * @author Benoit Lacelle
 *
 */
@EqualsAndHashCode
public class EclipseFormatterCacheKey {
	// The same URL may map to different configuration (e.g. if the URL is relative to the repository)
	final ICodeProvider codeProvider;
	final ILanguageProperties languageProperties;
	final EclipseJavaFormatterProcessorProperties eclipseJavaFormatterProcessorProperties;

	public EclipseFormatterCacheKey(ICodeProvider codeProvider,
			ILanguageProperties languageProperties,
			EclipseJavaFormatterProcessorProperties eclipseJavaFormatterProcessorProperties) {
		this.codeProvider = codeProvider;
		this.languageProperties = languageProperties;
		this.eclipseJavaFormatterProcessorProperties = eclipseJavaFormatterProcessorProperties;
	}

}
