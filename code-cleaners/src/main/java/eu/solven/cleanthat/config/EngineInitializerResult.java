package eu.solven.cleanthat.config;

import java.util.Map;

import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * The result of preparing a reasonnable config for CleanThat.
 * 
 * @author Benoit Lacelle
 *
 */
@Data
@Builder
public class EngineInitializerResult {
	final CleanthatRepositoryProperties repoProperties;

	@Singular
	final Map<String, String> pathToContents;

}
