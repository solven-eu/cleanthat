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
package eu.solven.cleanthat.engine.java.refactorer.mutators.scanner;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.annotations.VisibleForTesting;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;
import lombok.extern.slf4j.Slf4j;

/**
 * Scans dynamically for available rules
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection
@Slf4j
public final class MutatorsScanner {

	private static final AtomicInteger ERROR_COUNTS = new AtomicInteger();

	private static final Set<Class<? extends IMutator>> SINGLE_MUTATORS;

	static {
		try {
			// noinspection unchecked
			SINGLE_MUTATORS = Set.of(
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ArithmethicAssignment"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ArithmeticOverFloats"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ArraysDotStream"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidFileStream"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidInlineConditionals"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidMultipleUnaryOperators"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidUncheckedExceptionsInSignatures"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.CastMathOperandsBeforeAssignement"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionIndexOfToContains"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionToOptional"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ComparisonWithNaN"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.CreateTempFilesUsingNio"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.EmptyControlStatement"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.EnumsWithoutEquals"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachAddToStreamCollectToCollection"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfBreakElseToStreamTakeWhile"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfBreakToStreamFindFirst"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfToIfStreamAnyMatch"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachToIterableForEach"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaImmutableMapBuilderOverVarargs"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaInlineStringsRepeat"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaStringsIsNullOrEmpty"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ImportQualifiedTokens"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.JUnit4ToJUnit5"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.LoopIntRangeToIntStreamForEach"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ModifierOrder"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.NullCheckToOptionalOfNullable"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectEqualsForPrimitives"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ObjectsHashCodePrimitive"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalMapIdentity"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalNotEmpty"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedIfToFilter"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.PrimitiveWrapperInstantiation"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.RedundantLogicalComplementsInStream"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveAllToClearCollection"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.RemoveExplicitCallToSuper"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanExpression"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanInitialization"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyStartsWith"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StreamAnyMatch"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.StreamFlatMapStreamToFlatMap"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.StreamForEachNestingForLoopToFlatMap"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StreamMapIdentity"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedIfToFilter"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedMethodRefToMap"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedVariableToMap"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StringFromString"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StringIndexOfToContains"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.StringReplaceAllWithQuotableInput"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.StringToString"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.ThreadRunToThreadStart"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryBoxing"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryFullyQualifiedName"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryImport"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryLambdaEnclosingParameters"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryModifier"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessarySemicolon"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseCollectionIsEmpty"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperator"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperatorJdk8"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.UsePredefinedStandardCharset"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseStringIsEmpty"),
					(Class<? extends IMutator>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.UseTextBlocks"),
					(Class<? extends IMutator>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.UseUnderscoresInNumericLiterals"));
		} catch (ClassNotFoundException e) {
			ERROR_COUNTS.incrementAndGet();
			throw new IllegalStateException("Cannot load CleanThat mutators", e);
		}
	}
	private static final Set<Class<? extends IMutator>> COMPOSITE_MUTATORS;

	static {
		try {
			// noinspection unchecked
			COMPOSITE_MUTATORS = Set.of(
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeWalkingMutator"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CheckStyleMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.ErrorProneMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.GuavaMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.JSparrowMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.PMDMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SafeAndConsensualMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SafeButControversialMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SafeButNotConsensualMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SonarMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SpotBugsMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName("eu.solven.cleanthat.engine.java.refactorer.mutators.composite.StreamMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.UnsafeDueToGenerics"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftCompositeMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(
							"eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators"));

		} catch (ClassNotFoundException e) {
			ERROR_COUNTS.incrementAndGet();
			throw new IllegalStateException("Cannot load CleanThat mutators", e);
		}
	}

	private MutatorsScanner() {
		// Prevent instantiation
	}

	/**
	 * 
	 * @return the number of ERRORS which has been logged without failing the process.
	 */
	@VisibleForTesting
	public static int getErrorCount() {
		return ERROR_COUNTS.get();
	}

	/**
	 * The package is not search recursively.
	 * 
	 * @param classes
	 *            The IMutator classes to instantiate
	 * @return a {@link List} of {@link IMutator} detected in given package.
	 */
	public static <T extends IMutator> List<T> instantiate(JavaVersion sourceJdkVersion,
			List<Class<? extends T>> classes) {
		return classes.stream()
				.filter(Objects::nonNull)
				.filter(IMutator.class::isAssignableFrom)
				.map(c -> instantiate(sourceJdkVersion, c))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public static <T extends IMutator> T instantiate(JavaVersion sourceJdkVersion, Class<? extends T> mutatorClass) {
		try {
			if (IConstructorNeedsJdkVersion.class.isAssignableFrom(mutatorClass)) {
				return mutatorClass.getConstructor(JavaVersion.class).newInstance(sourceJdkVersion);
			} else {
				return mutatorClass.getConstructor().newInstance();
			}
		} catch (ReflectiveOperationException e) {
			ERROR_COUNTS.incrementAndGet();
			LOGGER.error("Issue with {}", mutatorClass, e);
			return null;
		}
	}

	@SuppressFBWarnings(value = "MS_EXPOSE_REP",
			justification = "The internal Set is immutable, so it is safe to expose it")
	public static Set<Class<? extends IMutator>> scanSingleMutators() {
		return SINGLE_MUTATORS;
	}

	@SuppressFBWarnings(value = "MS_EXPOSE_REP",
			justification = "The internal Set is immutable, so it is safe to expose it")
	public static Set<Class<? extends IMutator>> scanCompositeMutators() {
		return COMPOSITE_MUTATORS;
	}

}
