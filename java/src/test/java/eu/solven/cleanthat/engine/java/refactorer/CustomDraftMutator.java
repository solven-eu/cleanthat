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

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This is used to test the inclusion of a custom {@link IMutator} (e.g. for a third-party jar)
 * 
 * @author Benoit Lacelle
 *
 */
public class CustomDraftMutator implements IJavaparserAstMutator {

	@Override
	public boolean isDraft() {
		return true;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("UnitTest");
	}

	@Override
	public Set<String> getIds() {
		return Set.of("MyDraftCustomMutator");
	}

	@Override
	public Optional<Node> walkAst(Node pre) {
		return Optional.empty();
	}

}
