/*
 * Copyright 2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.meta.mutators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

public class MutatorScannerListsAllMutatorsTest {

	static final String PACKAGE_SINGLE_MUTATORS = "eu.solven.cleanthat.engine.java.refactorer.mutators";

	static final String PACKAGE_COMPOSITE_MUTATORS = "eu.solven.cleanthat.engine.java.refactorer.mutators.composite";

	@Test
	public void mutatorScannerContainsAllSingleMutators() {
		Set<Class<? extends IMutator>> expectedSingleMutators = scanSingleMutators();

		Set<Class<? extends IMutator>> mutatorScannerFound = MutatorsScanner.scanSingleMutators();

		assertThat(mutatorScannerFound).as("MutatorsScanner should list all single mutators")
				.containsExactlyInAnyOrderElementsOf(expectedSingleMutators);
	}

	@Test
	public void mutatorScannerContainsAllCompositeMutators() {
		Set<Class<? extends IMutator>> expectedCompositeMutators = scanCompositeMutators();

		Set<Class<? extends IMutator>> mutatorScannerFound = scanCompositeMutators();

		assertThat(mutatorScannerFound).as("MutatorsScanner should list all composite mutators")
				.containsExactlyInAnyOrderElementsOf(expectedCompositeMutators);
	}

	private Set<Class<? extends IMutator>> scanSingleMutators() {
		return scanMutatorsInPackage(PACKAGE_SINGLE_MUTATORS);
	}

	private Set<Class<? extends IMutator>> scanCompositeMutators() {
		return scanMutatorsInPackage(PACKAGE_COMPOSITE_MUTATORS);
	}

	@SuppressWarnings("unchecked")
	private Set<Class<? extends IMutator>> scanMutatorsInPackage(String packageName) {
		try (ScanResult scanResult =
				new ClassGraph().enableClassInfo().acceptPackagesNonRecursive(packageName).scan()) { // Start the scan
			return scanResult.getClassesImplementing(IMutator.class)
					.stream()
					.filter(classInfo -> !classInfo.isAbstract() && !classInfo.isInterface())
					.map(ClassInfo::loadClass)
					.map(clazz -> (Class<? extends IMutator>) clazz)
					.collect(Collectors.toSet());
		}
	}
}
