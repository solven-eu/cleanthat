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

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NumberToValueOf;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIsEmptyOnCollections;

/**
 * Helpers knowing what could be the impact of given rule
 *
 * @author Benoit Lacelle
 */
public interface IMutatorDescriber {

	/**
	 * 
	 * @return true if the rule helps cleaning deprecation notice
	 * 
	 * @see NumberToValueOf
	 */
	default boolean isDeprecationNotice() {
		return false;
	}

	/**
	 * 
	 * @return true if the rule helps improving performances
	 * 
	 * @see UseIsEmptyOnCollections
	 */
	// Relates to https://eslint.org/docs/user-guide/command-line-interface#--fix-type
	default boolean isPerformanceImprovment() {
		return false;
	}

	/**
	 * 
	 * This kind of rules may not fit everybody, as in some cases, exceptions are a feature (even if probably a bad
	 * thing).
	 * 
	 * @return true if the rule helps preventing exceptions.
	 * 
	 * @see UseIsEmptyOnCollections
	 */
	default boolean isPreventingExceptions() {
		return false;
	}

	/**
	 * @return the minimal JDKF version for this rule to trigger
	 */
	default String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}
}
