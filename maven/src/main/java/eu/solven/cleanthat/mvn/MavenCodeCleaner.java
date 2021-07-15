package eu.solven.cleanthat.mvn;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

public class MavenCodeCleaner extends ACodeCleaner {

	public MavenCodeCleaner(ObjectMapper objectMapper, ICodeProviderFormatter formatterProvider) {
		super(objectMapper, formatterProvider);
	}

}
