package eu.solven.cleanthat.config;

import java.util.Map;

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
public class RepoInitializerResult {
	final String commitMessage;
	final String prBody;

	@Singular
	final Map<String, String> pathToContents;

}
