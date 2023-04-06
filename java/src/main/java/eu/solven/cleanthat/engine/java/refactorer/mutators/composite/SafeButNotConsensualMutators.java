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

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ArraysDotStream;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidInlineConditionals;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidUncheckedExceptionsInSignatures;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionIndexOfToContains;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ComparisonWithNaN;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CreateTempFilesUsingNio;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EmptyControlStatement;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectEqualsForPrimitives;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectsHashCodePrimitive;
import eu.solven.cleanthat.engine.java.refactorer.mutators.PrimitiveWrapperInstantiation;
import eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveExplicitCallToSuper;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyStartsWith;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringIndexOfToContains;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryImport;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessarySemicolon;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseTextBlocks;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseUnderscoresInNumericLiterals;

/**
 * This mutator will apply all {@link IMutator} considered safe (e.g. by not impacting the {@link Runtime}, or only with
 * ultra-safe changes). It is not restricted to changes considered as not-consensual.
 * 
 * Example of not consensual mutator: {@link LocalVariableTypeInference} as some people prefer manipulating an object
 * through its interface.
 * 
 * @author Benoit Lacelle
 *
 */
public class SafeButNotConsensualMutators extends CompositeMutator<IMutator> implements IConstructorNeedsJdkVersion {
	public static final List<IMutator> SAFE_BUT_NOT_CONSENSUAL = ImmutableList.<IMutator>builder()
			.add(new ArraysDotStream(),
					// new AvoidFileStream(),
					new AvoidInlineConditionals(),
					new AvoidUncheckedExceptionsInSignatures(),
					new PrimitiveWrapperInstantiation(),
					new ComparisonWithNaN(),
					new CreateTempFilesUsingNio(),
					new EmptyControlStatement(),
					new EnumsWithoutEquals(),
					new LambdaIsMethodReference(),
					new LiteralsFirstInComparisons(),
					new LocalVariableTypeInference(),
					new UnnecessaryImport(),
					new UnnecessarySemicolon(),
					// UseDiamondOperator is too much unstable
					// new UseDiamondOperator(),
					// new UseDiamondOperatorJdk8(),
					// https://github.com/javaparser/javaparser/issues/3936
					new UseUnderscoresInNumericLiterals(),
					new SimplifyStartsWith(),
					new RemoveExplicitCallToSuper(),
					new UseTextBlocks(),
					new ObjectEqualsForPrimitives(),
					new ObjectsHashCodePrimitive(),
					// https://github.com/javaparser/javaparser/pull/3938
					new LambdaReturnsSingleStatement(),
					new CollectionIndexOfToContains(),
					new StringIndexOfToContains())
			.build();

	public SafeButNotConsensualMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, SAFE_BUT_NOT_CONSENSUAL));
	}

	@Override
	public String getCleanthatId() {
		return JavaRefactorerProperties.SAFE_BUT_NOT_CONSENSUAL;
	}
}
