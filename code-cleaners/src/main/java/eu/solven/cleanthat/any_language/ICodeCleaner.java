package eu.solven.cleanthat.any_language;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.CodeFormatResult;

/**
 * Knows how to clean code
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleaner {

	CodeFormatResult formatCodeGivenConfig(ICodeProviderWriter codeProvider, boolean dryRun);

}
