/*
 * Copyright 2023-2024 Benoit Lacelle - SOLVEN
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

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaImmutableMapBuilderOverVarargs;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedVariableToMap;

/**
 * This mutator includes all {@link IMutator} considered unsafe as the produced code may be invalid due to generics.
 * They regularly require human-intervention, for instance to resolve manually generics.
 * 
 * @author Benoit Lacelle
 *
 */
public class UnsafeDueToGenerics extends CompositeMutator<IMutator> implements IConstructorNeedsJdkVersion {
	public static final List<IMutator> UNSAFE_GENERICS = ImmutableList.<IMutator>builder()
			.add(
					// See TestStreamWrappedVariableToMapCases.IssueWithGenerics
					new StreamWrappedVariableToMap(),

					// See TestGuavaImmutableMapBuilderOverVarargs.WithGenerics_Wildcard
					new GuavaImmutableMapBuilderOverVarargs())
			.build();

	public UnsafeDueToGenerics(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, UNSAFE_GENERICS));
	}

	@Override
	public String getCleanthatId() {
		return "UnsafeGenerics";
	}
}
