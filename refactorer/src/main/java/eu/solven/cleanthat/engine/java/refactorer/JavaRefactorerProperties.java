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
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import lombok.Data;

/**
 * The configuration of {@link JavaRefactorer}.
 * 
 * 'excluded' and 'included': we include any rule which is included (by exact match, or if '*' is included), and not
 * excluded (by exact match)
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class JavaRefactorerProperties implements ICleanthatStepParametersProperties {
	@Deprecated(since = "One should rather rely on a CompositeMutator")
	public static final String WILDCARD = "*";

	private String sourceJdk;

	/**
	 * A {@link List} of included rules (by ID). '*' can be used to include all rules
	 */
	private List<String> mutators = List.of(SAFE_AND_CONSENSUAL);

	/**
	 * A {@link List} of excluded rules (by ID)
	 */
	private List<String> excludedMutators = List.of();

	/**
	 * One may activate not-production-ready rules. It may be useful to test a new rule over some external repository
	 */
	@Deprecated
	private boolean includeDraft = false;

	@Override
	public Object getCustomProperty(String key) {
		if ("source_jdk".equalsIgnoreCase(key)) {
			return sourceJdk;
		} else if ("mutators".equalsIgnoreCase(key)) {
			return mutators;
		} else if ("excluded_mutators".equalsIgnoreCase(key)) {
			return excludedMutators;
		} else if ("include_draft".equalsIgnoreCase(key)) {
			return includeDraft;
		}
		return null;
	}

	/**
	 * 
	 * @return a {@link JavaRefactorerProperties} based on {@link SafeAndConsensualMutators}
	 */
	public static JavaRefactorerProperties defaults() {
		return new JavaRefactorerProperties();
	}

	/**
	 * 
	 * @return a {@link JavaRefactorerProperties} based on {@link AllIncludingDraftSingleMutators}
	 */
	public static JavaRefactorerProperties allProductionReady() {
		var properties = new JavaRefactorerProperties();

		properties.setIncludeDraft(false);
		properties.setMutators(Arrays.asList(AllIncludingDraftSingleMutators.class.getName()));

		return properties;
	}

	/**
	 * 
	 * @return a {@link JavaRefactorerProperties} based on {@link AllIncludingDraftSingleMutators}
	 */
	@Deprecated
	public static JavaRefactorerProperties allEvenNotProductionReady() {
		var properties = new JavaRefactorerProperties();

		properties.setIncludeDraft(true);
		properties.setMutators(Arrays.asList(AllIncludingDraftSingleMutators.class.getName()));

		return properties;
	}

	@Deprecated(since = "Use .setMutators")
	public void setIncluded(List<String> included) {
		this.mutators = included;
	}

	@Deprecated(since = "Use .setExcludedMutators")
	public void setExcluded(List<String> excluded) {
		this.excludedMutators = excluded;
	}

}
