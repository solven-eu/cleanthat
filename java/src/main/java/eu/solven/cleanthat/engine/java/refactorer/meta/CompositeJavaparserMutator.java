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

import java.util.Arrays;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeWalkingMutator;

/**
 * This mutator make it easy to composite multiple {@link IJavaparserMutator}s in a single one.
 * 
 * The extended classes should generally implement a constructor taking a JavaVersions as single argument.
 * 
 * @author Benoit Lacelle
 *
 */
@SuppressWarnings("PMD.GenericsNaming")
public class CompositeJavaparserMutator extends CompositeWalkingMutator<Node> implements IJavaparserMutator {

	public CompositeJavaparserMutator() {
		this(Arrays.asList());
	}

	protected CompositeJavaparserMutator(List<IJavaparserMutator> mutators) {
		super(ImmutableList.copyOf(mutators));
	}

}
