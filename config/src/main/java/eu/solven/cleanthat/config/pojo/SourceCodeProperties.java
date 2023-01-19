/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.config.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * The configuration of what is not related to a language/an engine.
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class SourceCodeProperties implements ISourceCodeProperties {

	public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

	public static final LineEnding DEFAULT_LINE_ENDING = LineEnding.UNKNOWN;

	// If empty, no file is excluded
	// If multiple, we exclude files matching at least one exclude (OR)
	// see java.nio.file.FileSystem.getPathMatcher(String)
	private List<String> excludes = Arrays.asList();

	// If empty, no file is included
	// If multiple, we include files matching at least one include (OR)
	// see java.nio.file.FileSystem.getPathMatcher(String)
	private List<String> includes = Arrays.asList();

	// The encoding of files
	private String encoding = DEFAULT_ENCODING;

	// https://stackoverflow.com/questions/51388545/how-to-override-lombok-setter-methods
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private LineEnding lineEnding = DEFAULT_LINE_ENDING;

	private LineEnding parseLineEnding(String lineEnding) {
		return LineEnding.valueOf(lineEnding);
	}

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	@JsonIgnore
	@Override
	public LineEnding getLineEndingAsEnum() {
		return lineEnding;
	}

	public String getLineEnding() {
		return lineEnding.toString();
	}

	@JsonIgnore
	public void setLineEndingAsEnum(LineEnding lineEnding) {
		this.lineEnding = lineEnding;
	}

	public void setLineEnding(String lineEnding) {
		this.lineEnding = parseLineEnding(lineEnding);
	}
}
