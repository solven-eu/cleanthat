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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import com.github.javaparser.resolution.model.typesystem.LazyType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Helps working with {@link ResolvedType}
 * 
 * @author Benoit Lacelle
 *
 */
public class ResolvedTypeHelpers {

	protected ResolvedTypeHelpers() {
		// hidden
	}

	/**
	 * This specifically workaround difficulties around LazyType
	 * 
	 * @param left
	 * @param right
	 * @return true if the 2 resolvedTypes can be considered equivalent
	 */
	public static boolean areSameType(ResolvedType left, ResolvedType right) {
		if (left instanceof LazyType || right instanceof LazyType) {
			// https://github.com/javaparser/javaparser/issues/1983
			return left.describe().equals(right.describe());
		}

		return left.equals(right);
	}

}
