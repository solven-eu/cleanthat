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
package eu.solven.cleanthat.engine.java.refactorer.meta;

import java.util.Optional;
import java.util.Set;

/**
 * Helps understand why a rule is relevant, given other systems implementing the rule
 *
 * @author Benoit Lacelle
 */
public interface IMutatorExternalReferences {

	/**
	 * We default to the className as it is the one mainly referred by the documentation.
	 * 
	 * @return an id crafted by CleanThat. Useful when no 3rd-party linter has already a rule ID
	 */
	default String getCleanthatId() {
		return getClass().getSimpleName();
	}

	/**
	 * A rule may see its identifiers changing through time (e.g. {@link #getCleanthatId()} on a class name change).
	 * 
	 * @return the {@link Set} of legacy identifiers. They may end being removed (e.g. on a major release).
	 */
	default Set<String> getLegacyIds() {
		return Set.of();
	}

	default Optional<String> getPmdId() {
		// return getPmdIds().stream().findFirst();
		return Optional.empty();
	}

	default Set<String> getPmdIds() {
		// return getPmdId().stream().collect(Collectors.toSet());
		return Set.of();
	}

	default String pmdUrl() {
		return "";
	}

	default Optional<String> getSonarId() {
		// RSPEC-XXX
		return Optional.empty();
	}

	default String sonarUrl() {
		// https://sonarsource.atlassian.net/browse/RSPEC-XXX
		// https://sonarcloud.io/organizations/default/rules?languages=java&open=java%3ASXXX&q=SXXX
		return getSonarId().map(id -> "https://rules.sonarsource.com/java/" + id).orElse("");
	}

	default Optional<String> getCheckstyleId() {
		return Optional.empty();
	}

	default String checkstyleUrl() {
		return "";
	}

	default Optional<String> getErrorProneId() {
		return Optional.empty();
	}

	default String errorProneUrl() {
		// e.g. https://errorprone.info/bugpattern/InlineMeInliner
		return getErrorProneId().map(id -> "https://errorprone.info/bugpattern/" + id).orElse("");
	}

	default Optional<String> getJSparrowId() {
		return Optional.empty();
	}

	default String jSparrowUrl() {
		return "";
	}

	default Optional<String> getSpotBugsId() {
		return Optional.empty();
	}

	default String spotBugsUrl() {
		return "";
	}

	default Set<String> getSeeUrls() {
		return Set.of();
	}
}
