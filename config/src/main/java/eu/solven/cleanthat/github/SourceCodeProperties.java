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
public class SourceCodeProperties implements ISourceCodeProperties {

	public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();
	public static final String DEFAULT_LINE_ENDING = "LF";

	// If empty, no file is excluded
	// If multiple, we exclude files matching at least one exclude (OR)
	private List<String> excludes = Arrays.asList();

	// If empty, no file is included
	// If multiple, we include files matching at least one include (OR)
	private List<String> includes = Arrays.asList();

	// The encoding of files
	private String encoding = DEFAULT_ENCODING;

	private String lineEnding = DEFAULT_LINE_ENDING;

	@Override
	public List<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	@Override
	public List<String> getIncludes() {
		return includes;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	@JsonIgnore
	@Override
	public LineEnding getLineEnding() {
		return LineEnding.valueOf(lineEnding);
	}

	@JsonProperty("line_ending")
	public String getRawLineEnding() {
		return lineEnding;
	}

	@JsonProperty("line_ending")
	public void setLineEnding(String lineEnding) {
		this.lineEnding = lineEnding;
	}
}
