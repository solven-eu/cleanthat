package eu.solven.cleanthat.mvn;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * A default {@link ACodeCleaner} for maven
 * 
 * @author Benoit Lacelle
 *
 */
public class MavenCodeCleaner extends ACodeCleaner {

	public MavenCodeCleaner(List<ObjectMapper> objectMappers, ICodeProviderFormatter formatterProvider) {
		super(objectMappers, formatterProvider);
	}

}
