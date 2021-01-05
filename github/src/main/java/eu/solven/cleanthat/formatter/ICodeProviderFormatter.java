package eu.solven.cleanthat.formatter;

import java.util.Map;

import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.event.ICodeProvider;

/**
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeProviderFormatter {

	Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProvider pr);

}
