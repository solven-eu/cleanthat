package eu.solven.cleanthat.any_language;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

public interface ICodeCleaner {

	Map<String, ?> formatCodeGivenConfig(ICodeProviderWriter codeProvider);

}
