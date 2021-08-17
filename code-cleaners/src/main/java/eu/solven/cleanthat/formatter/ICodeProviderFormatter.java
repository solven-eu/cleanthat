package eu.solven.cleanthat.formatter;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProviderFormatter {

	CodeFormatResult formatCode(CleanthatRepositoryProperties properties,
			ICodeProviderWriter codeProvider,
			boolean dryRun);

}
