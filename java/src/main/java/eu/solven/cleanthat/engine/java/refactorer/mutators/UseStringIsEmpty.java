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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;
import java.util.Set;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;

/**
 * Migrate from 'm.length() == 0’ to ’m.isEmpty()'. Works with {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseStringIsEmpty extends AUseXIsEmpty {
	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String minimalJavaVersion() {
		// java.lang.String.isEmpty() exists since 1.6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public Optional<String> getCleanthatId() {
		// Naming similar to UseCollectionIsEmpty
		return Optional.of("UseStringIsEmpty");
	}

	@Override
	protected String getSizeMethod() {
		return "length";
	}

	@Override
	protected Set<Class<?>> getCompatibleTypes() {
		return Set.of(String.class);
	}

}
