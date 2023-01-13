package eu.solven.cleanthat.language.spotless;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.spotless.ExecuteSpotless;
import lombok.Data;

/**
 * The configuration of {@link ExecuteSpotless}.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class SpotlessCleanthatProperties {
	// The default configuration location is the first option amongst the possible locations
	private static final String DEFAULT_CONFIGURATION = CodeProviderHelpers.FILENAMES_CLEANTHAT.get(0);

	private String configuration = DEFAULT_CONFIGURATION;;
}
