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
public class CleanthatLanguageProperties implements ILanguageProperties {

	private SourceCodeProperties sourceCode;

	private String language = "none";

	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private String languageVersion = "0";

	// The (ordered) processors to apply
	// @JsonDeserialize(using = ProcessorsDeseralizer.class)
	private List<Map<String, ?>> processors = Arrays.asList();

}
