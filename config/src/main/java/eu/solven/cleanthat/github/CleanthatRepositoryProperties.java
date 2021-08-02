package eu.solven.cleanthat.github;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.language.CleanthatMetaProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * The configuration of a formatting job
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CleanthatRepositoryProperties {
	public static final String LATEST_SYNTAX_VERSION = "2021-08-02";

	// Not named 'config_version' else it may be unclear if it applies to that config_syntax or the the user_config
	// version
	// AWS IAM policy relies on a field named 'Version' with a localDate as value: it is a source of inspiration
	private String syntaxVersion = LATEST_SYNTAX_VERSION;

	private CleanthatMetaProperties meta = new CleanthatMetaProperties();

	// Properties to apply to each children
	private ISourceCodeProperties sourceCodeProperties = new SourceCodeProperties();

	private List<Map<String, ?>> languages = Arrays.asList();

	public String getSyntaxVersion() {
		return syntaxVersion;
	}

	public void setSyntaxVersion(String syntaxVersion) {
		this.syntaxVersion = syntaxVersion;
	}

	public CleanthatMetaProperties getMeta() {
		return meta;
	}

	public void setMeta(CleanthatMetaProperties meta) {
		this.meta = meta;
	}

	public ISourceCodeProperties getSourceCodeProperties() {
		return sourceCodeProperties;
	}

	@JsonProperty("source_code")
	public void setSourceCodeProperties(SourceCodeProperties sourceCodeProperties) {
		this.sourceCodeProperties = sourceCodeProperties;
	}

	public List<Map<String, ?>> getLanguages() {
		return languages;
	}

	public void setLanguages(List<Map<String, ?>> languages) {
		this.languages = languages;
	}

}
