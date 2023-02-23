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
package eu.solven.cleanthat.engine.java.refactorer.test;

import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

/**
 * Base class for the test-cases of a {@link IWalkingMutator}
 * 
 * @author Benoit Lacelle
 *
 * @param <AST>
 * @param <R>
 * @param <M>
 */
@SuppressWarnings("PMD.GenericsNaming")
public abstract class ARefactorerCases<AST, R, M extends IWalkingMutator<AST, R>> {
	public String getId() {
		return getTransformer().getClass().getName();
	}

	public abstract IWalkingMutator<AST, R> getTransformer();

}
