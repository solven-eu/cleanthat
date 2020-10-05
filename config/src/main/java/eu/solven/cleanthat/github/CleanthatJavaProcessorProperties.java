package eu.solven.cleanthat.github;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.formatter.LineEnding;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
public class CleanthatJavaProcessorProperties {

	// If empty, no file is excluded
	private List<String> excludes = Arrays.asList();

	// If empty, no file is included
	private List<String> includes = Arrays.asList("regex:.*\\.java");

	// The encoding of files
	private String encoding = StandardCharsets.UTF_8.name();

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	private String lineEnding = "LF";

	// The custom configuration of this processor
	private Map<String, ?> configuration;

	public List<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	@JsonIgnore
	public LineEnding getLineEnding() {
		return LineEnding.valueOf(lineEnding);
	}

	@JsonProperty("line_ending")
	public String getRawLineEnding() {
		return lineEnding;
	}

	public Map<String, ?> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, ?> configuration) {
		this.configuration = configuration;
	}
}
