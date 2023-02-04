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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.eclipse.jgit.lib.ConfigConstants;

/**
 * Used to configure Spotless regarding the simulation of a local .git/config
 * 
 * @author Benoit Lacelle
 * @see ConfigConstants.CONFIG_CORE_SECTION
 */
// https://github.com/diffplug/spotless/tree/main/plugin-gradle#quickstart
// https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart
// see com.diffplug.spotless.extra.GitAttributesLineEndings_InMemory.Runtime.findDefaultLineEnding(Config)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
@Builder
@Jacksonized
public class SpotlessGitProperties {

	// https://git-scm.com/docs/git-config#Documentation/git-config.txt-coreeol
	@Builder.Default
	private String coreEol = "native";

	// https://git-scm.com/docs/git-config#Documentation/git-config.txt-coreautocrlf
	private String coreAutocrlf;

}
