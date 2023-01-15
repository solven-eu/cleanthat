package eu.solven.cleanthat.spotless;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.diffplug.spotless.LineEnding;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Used to configure Spotless various plugins
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class SpotlessProperties {
	public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

	// The encoding of files
	private String encoding = DEFAULT_ENCODING;

	// The lineEnding. See LineEnding
	private String lineEnding = LineEnding.GIT_ATTRIBUTES.name();

	// java, json
	private final String language;

	private List<String> includes = new ArrayList<>();
	private List<String> excludes = new ArrayList<>();

	private List<SpotlessStepProperties> steps = new ArrayList<>();
}
