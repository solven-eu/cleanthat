package eu.solven.cleanthat.github;

import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * Anything wityh a {@link SourceCodeProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IHasSourceCodeProperties {
	ISourceCodeProperties getSourceCode();
}
