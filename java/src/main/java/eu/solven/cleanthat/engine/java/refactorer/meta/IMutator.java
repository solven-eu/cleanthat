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
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
public interface IMutator extends IRuleExternalReferences {

	@Deprecated(since = "This is for tests purposes")
	String ID_NOOP = "NoOp";

	// For java, prefer Checkstyle name, else PMD name
	@Deprecated
	default String getId() {
		return "TODO";
	}

	default Set<String> getIds() {
		Set<String> ids = Stream.of(Optional.of(getId()), getCheckstyleId(), getPmdId(), getSonarId(), getCleanthatId())
				.flatMap(Optional::stream)
				.filter(s -> !"TODO".equals(s))
				// Not sorted to privilege PMD over SONAR
				// .sorted()
				.collect(ImmutableSet.toImmutableSet());

		if (ids.isEmpty()) {
			throw new IllegalStateException("We miss an id for : " + this.getClass());
		}
		return ids;
	}

	/**
	 * @return true if this rule process only jre standard classes
	 */
	default boolean isJreOnly() {
		return true;
	}

	/**
	 * 
	 * @return the minimal JDK for which this rule is applicable. For instance, any rule related with diamond operator
	 *         requires JDK1.5
	 */
	default String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	/**
	 * Draft mutators are excluded by default from {@link CompositeMutator}. They may be included to check the
	 * {@link IMutator} behavior on the author code, until being considered production-grade for all users.
	 * 
	 * @return true if this mutator is considered draft.
	 */
	default boolean isDraft() {
		// default is true so mutators are by default excluded. This is a safety mechanism.
		return true;
	}

}
