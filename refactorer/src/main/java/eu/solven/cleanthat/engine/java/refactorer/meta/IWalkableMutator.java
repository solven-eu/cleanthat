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

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings("PMD.GenericsNaming")
public interface IWalkableMutator<AST, R> extends IMutator {

	@Deprecated(since = "Useful for simplified unitTests")
	default boolean walkAstHasChanged(AST pre) {
		return walkAst(pre).isPresent();
	}

	Optional<R> walkAst(AST pre);
}
