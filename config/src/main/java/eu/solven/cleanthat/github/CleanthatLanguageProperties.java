package eu.solven.cleanthat.github;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
public class CleanthatLanguageProperties implements ILanguageProperties {

	private ISourceCodeProperties sourceCodeProperties;

	private String language;

	// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	private String languageVersion;

	// The (ordered) processors to apply
	// @JsonDeserialize(using = ProcessorsDeseralizer.class)
	private List<Map<String, ?>> processors = Arrays.asList();

	@Override
	@JsonProperty("source_code")
	public ISourceCodeProperties getSourceCodeProperties() {
		return sourceCodeProperties;
	}

	@JsonProperty("source_code")
	public void setSourceCodeProperties(SourceCodeProperties sourceCodeProperties) {
		this.sourceCodeProperties = sourceCodeProperties;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public String getLanguageVersion() {
		return languageVersion;
	}

	public void setLanguageVersion(String languageVersion) {
		this.languageVersion = languageVersion;
	}

	@Override
	public List<Map<String, ?>> getProcessors() {
		return processors;
	}

	public void setProcessors(List<Map<String, ?>> processors) {
		this.processors = processors;
	}

}
