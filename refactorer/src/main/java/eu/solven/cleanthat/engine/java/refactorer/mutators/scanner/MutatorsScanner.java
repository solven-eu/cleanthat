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

	private static final String PACKAGE_MUTATORS = "eu.solven.cleanthat.engine.java.refactorer.mutators.";
	private static final String PACKAGE_COMPOSITE_MUTATORS = PACKAGE_MUTATORS + "composite.";

	private static final AtomicInteger ERROR_COUNTS = new AtomicInteger();

	private static final Set<Class<? extends IMutator>> SINGLE_MUTATORS;

	static {
		try {
			// noinspection unchecked
			SINGLE_MUTATORS = Set.of(
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "AppendCharacterWithChar"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ArithmethicAssignment"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ArithmeticOverFloats"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ArraysDotStream"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "AvoidFileStream"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "AvoidInlineConditionals"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "AvoidMultipleUnaryOperators"),
					(Class<? extends IMutator>) Class
							.forName(PACKAGE_MUTATORS + "AvoidUncheckedExceptionsInSignatures"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "CastMathOperandsBeforeAssignement"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "CollectionIndexOfToContains"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "CollectionToOptional"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ComparisonWithNaN"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "CreateTempFilesUsingNio"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "EmptyControlStatement"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "EnumsWithoutEquals"),
					(Class<? extends IMutator>) Class
							.forName(PACKAGE_MUTATORS + "ForEachAddToStreamCollectToCollection"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ForEachIfBreakElseToStreamTakeWhile"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ForEachIfBreakToStreamFindFirst"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ForEachIfToIfStreamAnyMatch"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ForEachToIterableForEach"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "GuavaImmutableMapBuilderOverVarargs"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "GuavaInlineStringsRepeat"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "GuavaStringsIsNullOrEmpty"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ImportQualifiedTokens"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "JUnit4ToJUnit5"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "LambdaIsMethodReference"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "LambdaReturnsSingleStatement"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "LiteralsFirstInComparisons"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "LocalVariableTypeInference"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "LoopIntRangeToIntStreamForEach"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ModifierOrder"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "NullCheckToOptionalOfNullable"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ObjectEqualsForPrimitives"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ObjectsHashCodePrimitive"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "OptionalMapIdentity"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "OptionalNotEmpty"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "OptionalWrappedIfToFilter"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "OptionalWrappedVariableToMap"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "PrimitiveWrapperInstantiation"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "RedundantLogicalComplementsInStream"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "RemoveAllToClearCollection"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "RemoveExplicitCallToSuper"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "SimplifyBooleanExpression"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "SimplifyBooleanInitialization"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "SimplifyStartsWith"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamAnyMatch"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamFlatMapStreamToFlatMap"),
					(Class<? extends IMutator>) Class
							.forName(PACKAGE_MUTATORS + "StreamForEachNestingForLoopToFlatMap"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamMapIdentity"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamWrappedIfToFilter"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamWrappedMethodRefToMap"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StreamWrappedVariableToMap"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StringFromString"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StringIndexOfToContains"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StringReplaceAllWithQuotableInput"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "StringToString"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "ThreadRunToThreadStart"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessaryBoxing"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessaryCaseChange"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessaryFullyQualifiedName"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessaryImport"),
					(Class<? extends IMutator>) Class
							.forName(PACKAGE_MUTATORS + "UnnecessaryLambdaEnclosingParameters"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessaryModifier"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UnnecessarySemicolon"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseCollectionIsEmpty"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseDiamondOperator"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseDiamondOperatorJdk8"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseIndexOfChar"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UsePredefinedStandardCharset"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseStringIsEmpty"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseTextBlocks"),
					(Class<? extends IMutator>) Class.forName(PACKAGE_MUTATORS + "UseUnderscoresInNumericLiterals"));
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
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "CompositeWalkingMutator"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "CheckStyleMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "ErrorProneMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(PACKAGE_COMPOSITE_MUTATORS + "GuavaMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "JSparrowMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(PACKAGE_COMPOSITE_MUTATORS + "PMDMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "SafeAndConsensualMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "SafeButControversialMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "SafeButNotConsensualMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(PACKAGE_COMPOSITE_MUTATORS + "SonarMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "SpotBugsMutators"),
					(Class<? extends CompositeMutator<?>>) Class.forName(PACKAGE_COMPOSITE_MUTATORS + "StreamMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "UnsafeDueToGenerics"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "AllIncludingDraftCompositeMutators"),
					(Class<? extends CompositeMutator<?>>) Class
							.forName(PACKAGE_COMPOSITE_MUTATORS + "AllIncludingDraftSingleMutators"));

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
