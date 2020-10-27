package eu.solven.cleanthat.github;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The configuration of a formatting job
 *
 * @author Benoit Lacelle
 */
public class CleanthatRepositoryProperties {

	private CleanthatMetaProperties meta;

	private List<Map<String, ?>> languages = Arrays.asList();

	// Properties to apply to each children
	private ISourceCodeProperties sourceCodeProperties;

	public CleanthatMetaProperties getMeta() {
		return meta;
	}

	public void setMeta(CleanthatMetaProperties meta) {
		this.meta = meta;
	}

	public List<Map<String, ?>> getLanguages() {
		return languages;
	}

	public void setLanguages(List<Map<String, ?>> languages) {
		this.languages = languages;
	}

	public ISourceCodeProperties getSourceCodeProperties() {
		return sourceCodeProperties;
	}

	@JsonProperty("source_code")
	public void setSourceCodeProperties(SourceCodeProperties sourceCodeProperties) {
		this.sourceCodeProperties = sourceCodeProperties;
	}
}
