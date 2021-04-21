package eu.solven.cleanthat.formatter;

import java.util.Map;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProviderFormatter {

	Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProvider codeProvider);

}
