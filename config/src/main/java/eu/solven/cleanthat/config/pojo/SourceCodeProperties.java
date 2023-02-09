/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * The configuration of what is not related to a language/an engine.
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
public class SourceCodeProperties implements ISourceCodeProperties {
	public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();

	// If empty, no file is excluded
	// If multiple, we exclude files matching at least one exclude (OR)
	// see java.nio.file.FileSystem.getPathMatcher(String)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Singular
	private List<String> excludes;

	// If empty, no file is included
	// If multiple, we include files matching at least one include (OR)
	// see java.nio.file.FileSystem.getPathMatcher(String)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@Singular
	private List<String> includes;

	// The encoding of files
	private String encoding;

	// https://stackoverflow.com/questions/51388545/how-to-override-lombok-setter-methods
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	// private Optional<LineEnding> lineEnding;
	private LineEnding lineEnding;

	// Git has some preference to committing LF
	// https://code.revelc.net/formatter-maven-plugin/format-mojo.html#lineEnding
	@JsonIgnore
	@Override
	public LineEnding getLineEndingAsEnum() {
		// return lineEnding.orElse(null);
		return lineEnding;
	}

	public String getLineEnding() {
		// return lineEnding.map(Object::toString).orElse(null);
		return Optional.ofNullable(lineEnding).map(Object::toString).orElse(null);
	}

	@JsonIgnore
	public void setLineEndingAsEnum(LineEnding lineEnding) {
		// this.lineEnding = Optional.ofNullable(lineEnding);
		this.lineEnding = lineEnding;
	}

	public static SourceCodeProperties defaultRoot() {
		return SourceCodeProperties.builder()
				.encoding(DEFAULT_ENCODING)
				// .lineEnding(Optional.of(LineEnding.GIT))
				.lineEnding(LineEnding.GIT)
				.build();
	}

	public static SourceCodeProperties defaultChild() {
		return SourceCodeProperties.builder().build();
	}

}
