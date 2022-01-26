package eu.solven.cleanthat.language;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * The configuration of what is not related to a language.
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.ImmutableField")
@Data
// Order is defined by fields definition
// @JsonPropertyOrder(alphabetic = true, value = { "language", "language_version", ISkippable.KEY_SKIP })
public class LanguageProperties implements ILanguageProperties {

	private String language = "none";

	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private String languageVersion = "0";

	// By default, we do not skip
	private boolean skip = false;

	private SourceCodeProperties sourceCode = new SourceCodeProperties();

	// The (ordered) processors to apply
	// @JsonDeserialize(using = ProcessorsDeseralizer.class)
	private List<Map<String, ?>> processors = Arrays.asList();

}
