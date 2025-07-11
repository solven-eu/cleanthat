/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.base.Suppliers;

import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;

/**
 * This mutator will apply all {@link IMutator}s,even those considered not production-ready
 * 
 * @author Benoit Lacelle
 *
 */
public class AllIncludingDraftSingleMutators extends CompositeMutator<IMutator> implements IConstructorNeedsJdkVersion {
	// This packageName is not part of the public API

	static final Supplier<List<Class<? extends IMutator>>> ALL_INCLUDINGDRAFT =
			Suppliers.memoize(() -> MutatorsScanner.scanSingleMutators()
					.stream()
					// Sort by className, to always apply mutators in the same order
					.sorted(Comparator.comparing(Class::getName))
					.collect(Collectors.toList()));

	public AllIncludingDraftSingleMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, MutatorsScanner.instantiate(sourceJdkVersion, ALL_INCLUDINGDRAFT.get())));
	}

}
