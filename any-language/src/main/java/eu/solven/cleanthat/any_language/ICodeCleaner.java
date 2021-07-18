package eu.solven.cleanthat.any_language;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

/**
 * Knows how to clean code
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleaner {

	Map<String, ?> formatCodeGivenConfig(ICodeProviderWriter codeProvider, boolean dryRun);

}
