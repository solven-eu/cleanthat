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
package eu.solven.cleanthat.spotless.pojo;

import com.diffplug.spotless.LineEnding;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * Used to configure Spotless various plugins
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/diffplug/spotless/tree/main/plugin-gradle#quickstart
// https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
@Jacksonized
public class SpotlessEngineProperties {
	public static final String LATEST_SYNTAX_VERSION = "2023-01-09";
	public static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();
	public static final String DEFAULT_DEFAULT = "default";

	@Builder.Default
	private String syntaxVersion = LATEST_SYNTAX_VERSION;

	// https://github.com/diffplug/spotless/tree/main/plugin-maven#line-endings-and-encodings-invisible-stuff
	@Builder.Default
	private String encoding = DEFAULT_ENCODING;

	@Builder.Default
	private SpotlessGitProperties git = SpotlessGitProperties.builder().build();

	// https://github.com/diffplug/spotless/tree/main/plugin-maven#line-endings-and-encodings-invisible-stuff
	// see com.diffplug.spotless.LineEnding
	@Builder.Default
	private String lineEnding = LineEnding.GIT_ATTRIBUTES.name();

	// The steps of given language
	@Singular
	private List<SpotlessFormatterProperties> formatters;

	/**
	 * 
	 * @return a minimal engine properties with rationale default. It supposed it will process a Git hub repository,
	 *         which is most of the time accompanied by a README.MD at the root
	 */
	public static SpotlessEngineProperties defaultEngineWithMarkdown() {
		SpotlessStepProperties flexmark = SpotlessStepProperties.builder().id("flexmark").build();

		return SpotlessEngineProperties.builder()
				.formatter(SpotlessFormatterProperties.builder().format("markdown").step(flexmark).build())
				.build();
	}
}
