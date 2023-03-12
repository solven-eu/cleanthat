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
import java.util.Optional;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnhancedForLoopToStreamAnyMatch;

/**
 * This mutator will apply all {@link IMutator} considered not-trivial. It is relevant to demonstrate the most
 * complex/useful rules, without polluting the diff with trivial changes.
 * 
 * @author Benoit Lacelle
 *
 */
public class SafeButNotTrivialMutators extends CompositeMutator<IMutator> implements IConstructorNeedsJdkVersion {
	public static final List<IMutator> NOT_TRIVIAL =
			ImmutableList.<IMutator>builder().add(new EnhancedForLoopToStreamAnyMatch()).build();

	public SafeButNotTrivialMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, NOT_TRIVIAL));
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of(JavaRefactorerProperties.SAFE_BUT_CONTROVERSIAL);
	}
}
