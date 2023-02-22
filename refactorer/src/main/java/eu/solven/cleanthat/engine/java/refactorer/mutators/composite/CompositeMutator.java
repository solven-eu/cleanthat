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
package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This mutator make it easy to composite multiple {@link IMutator}s in a single one.
 * 
 * The extended classes should generally implement a constructor taking a JavaVersions as single argument.
 * 
 * @author Benoit Lacelle
 *
 */
public class CompositeMutator<T extends IMutator> implements IMutator {

	final List<T> mutators;

	protected CompositeMutator(List<T> mutators) {
		this.mutators = ImmutableList.copyOf(mutators);
	}

	public List<T> getUnderlyings() {
		return mutators;
	}

	@Override
	public Set<String> getIds() {
		return mutators.stream()
				.flatMap(ct -> ct.getIds().stream())
				.sorted()
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public static <T extends IMutator> List<T> filterWithJdk(JavaVersion sourceJdkVersion, List<? extends T> mutators) {
		return mutators.stream()
				.filter(m -> sourceJdkVersion.isAtLeast(m.minimalJavaVersion()))
				.collect(Collectors.toList());
	}

}