package eu.solven.cleanthat.github;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.language.CleanthatMetaProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.SourceCodeProperties;
import lombok.Data;

/**
 * The configuration of a formatting job
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
// https://stackoverflow.com/questions/19272830/order-of-json-objects-using-jacksons-objectmapper
@JsonPropertyOrder({ "syntax_version", "meta", "source_code", "languages" })
@Data
public final class CleanthatRepositoryProperties implements IHasSourceCodeProperties {
	public static final String LATEST_SYNTAX_VERSION = "2021-08-02";

	// Not named 'config_version' else it may be unclear if it applies to that config_syntax or the the user_config
	// version
	// AWS IAM policy relies on a field named 'Version' with a localDate as value: it is a source of inspiration
	private String syntaxVersion = LATEST_SYNTAX_VERSION;

	private CleanthatMetaProperties meta = new CleanthatMetaProperties();

	// Properties to apply to each children
	private SourceCodeProperties sourceCode = new SourceCodeProperties();

	// @JsonProperty(index = -999)
	// private List<Map<String, ?>> languages = Arrays.asList();
	private List<LanguageProperties> languages = new ArrayList<>();

}
