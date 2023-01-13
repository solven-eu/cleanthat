package eu.solven.cleanthat.github;

import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Anything wityh a {@link SourceCodeProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IHasSourceCodeProperties {
	ISourceCodeProperties getSourceCode();
}
