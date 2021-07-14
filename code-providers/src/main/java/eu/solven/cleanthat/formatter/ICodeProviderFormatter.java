package eu.solven.cleanthat.formatter;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProviderFormatter {

	Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProviderWriter codeProvider);

}
