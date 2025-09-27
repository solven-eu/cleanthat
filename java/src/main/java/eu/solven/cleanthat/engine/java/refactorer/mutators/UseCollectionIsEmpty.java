/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;

/**
 * Migrate from 'm.size() == 0’ to ’m.isEmpty()'. Works with {@link Collection} and {@link Map}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseCollectionIsEmpty extends AUseXIsEmpty {
	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Collection");
	}

	@Override
	public String minimalJavaVersion() {
		// java.util.Collection.isEmpty() exists since 1.2
		return IJdkVersionConstants.JDK_2;
	}

	@Override
	public String pmdUrl() {
		// https://github.com/pmd/pmd/blob/master/pmd-java/src/main/java/net/sourceforge/pmd/lang/java/rule/bestpractices/UseCollectionIsEmptyRule.java
		return "https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#usecollectionisempty";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseCollectionIsEmpty");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1155");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("UseIsEmptyOnCollections");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/use-is-empty-on-collections.html";
	}

	@Override
	protected String getSizeMethod() {
		return "size";
	}

	@Override
	protected Set<Class<?>> getCompatibleTypes() {
		return Set.of(Collection.class, Map.class);
	}

}
