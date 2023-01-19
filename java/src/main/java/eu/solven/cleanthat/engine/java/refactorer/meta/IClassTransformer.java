/*
 * Copyright 2023 Solven
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

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
public interface IClassTransformer {

	// For java, prefer Checkstyle name, else PMD name
	@Deprecated
	default String getId() {
		return "TODO";
	}

	default Set<String> getIds() {
		Set<String> ids = Stream.of(Optional.of(getId()), getCheckstyleId(), getPmdId())
				.flatMap(Optional::stream)
				.filter(s -> !"TODO".equals(s))
				.collect(Collectors.toSet());

		if (ids.isEmpty()) {
			throw new IllegalStateException("We miss an id for : " + this.getClass());
		}
		return ids;
	}

	default Optional<String> getPmdId() {
		return Optional.empty();
	}

	default Optional<String> getCheckstyleId() {
		return Optional.empty();
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

	default boolean isProductionReady() {
		return true;
	}

	/**
	 * 
	 * @param pre
	 * @return true if the AST has been modified.
	 */
	boolean walkNode(Node pre);

}
