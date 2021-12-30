package eu.solven.cleanthat.mvn;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;

/**
 * A default {@link ACodeCleaner} for maven
 * 
 * @author Benoit Lacelle
 *
 */
public class MavenCodeCleaner extends ACodeCleaner {

	public MavenCodeCleaner(Collection<ObjectMapper> objectMappers,
			Collection<ILanguageLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		super(objectMappers, factories, formatterProvider);
	}

}
