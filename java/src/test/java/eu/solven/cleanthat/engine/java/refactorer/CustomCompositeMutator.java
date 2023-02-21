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

import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;

/**
 * A custom {@link CompositeMutator} holding both a draft and production-ready {@link IMutator}
 * 
 * @author Benoit Lacelle
 *
 */
public class CustomCompositeMutator extends CompositeMutator<IMutator> {

	public CustomCompositeMutator(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, defaultUnderlyings()));
	}

	public CustomCompositeMutator(List<IMutator> mutators) {
		super(mutators);
	}

	private static List<IMutator> defaultUnderlyings() {
		return Arrays.asList(new CustomMutator(), new CustomDraftMutator());
	}

	public static CustomCompositeMutator customMutators() {
		return new CustomCompositeMutator(defaultUnderlyings());
	}

}
