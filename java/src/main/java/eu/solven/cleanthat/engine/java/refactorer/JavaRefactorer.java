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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftCompositeMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * This class is dedicated to refactoring. Most mutators will refactor code to a better (e.g. shorter, faster, safer,
 * etc) but with [strictly|roughly] equivalent runtime behavior.
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class JavaRefactorer implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaRefactorer.class);

	public static final String ID_REFACTORER = "refactorer";

	private final IEngineProperties engineProperties;
	private final JavaRefactorerProperties refactorerProperties;

	private final List<IMutator> mutators;

	public static final Set<String> getAllIncluded() {
		return new AllIncludingDraftSingleMutators(JavaVersion.parse(IJdkVersionConstants.LAST)).getIds();
	}

	public JavaRefactorer(IEngineProperties engineProperties, JavaRefactorerProperties properties) {
		this.engineProperties = engineProperties;
		this.refactorerProperties = properties;

		this.mutators = filterRules(engineProperties, properties);

		this.mutators.forEach(ct -> {
			LOGGER.debug("Using transformer: {}", ct.getIds());
		});
	}

	public static List<IMutator> filterRules(IEngineProperties engineProperties, JavaRefactorerProperties properties) {
		JavaVersion engineVersion = JavaVersion.parse(engineProperties.getEngineVersion());

		List<String> includedRules = properties.getIncluded();
		List<String> excludedRules = properties.getExcluded();
		boolean includeDraft = properties.isIncludeDraft();

		// TODO Enable a custom rule in includedRules (e.g. to load from a 3rd party JAR)
		return filterRules(engineVersion, includedRules, excludedRules, includeDraft);
	}

	public static List<IMutator> filterRules(JavaVersion sourceCodeVersion,
			List<String> includedRules,
			List<String> excludedRules,
			boolean includeDraft) {

		List<IMutator> allSingleMutators = new AllIncludingDraftSingleMutators(sourceCodeVersion).getUnderlyings();
		List<IMutator> allCompositeMutators =
				new AllIncludingDraftCompositeMutators(sourceCodeVersion).getUnderlyings();

		List<IMutator> mutatorsMayComposite = includedRules.stream().flatMap(includedRule -> {
			if (JavaRefactorerProperties.WILDCARD.equals(includedRule)) {
				// We suppose there is no mutator from Composite which is not a single mutator
				// Hence we return all single mutators
				return allSingleMutators.stream();
			} else {
				List<IMutator> matchingMutators =
						Stream.concat(allSingleMutators.stream(), allCompositeMutators.stream())
								.filter(someMutator -> someMutator.getIds().contains(includedRule)
										|| someMutator.getClass().getName().equals(includedRule))
								.collect(Collectors.toList());

				if (!matchingMutators.isEmpty()) {
					return matchingMutators.stream();
				}

				Optional<IMutator> optFromClassName = loadMutatorFromClass(sourceCodeVersion, includedRule);

				if (optFromClassName.isPresent()) {
					return optFromClassName.stream();
				}

				LOGGER.warn("includedMutator={} did not matched any mutator", includedRule);
				return Stream.empty();
			}
		}).collect(Collectors.toList());

		// We unroll composite to enable exclusion of included mutators
		List<IMutator> mutatorsNotComposite = unrollCompositeMutators(mutatorsMayComposite);

		// TODO '.distinct()' to handle multiple composites bringing the same mutator
		return mutatorsNotComposite.stream().filter(mutator -> {
			boolean isExcluded = excludedRules.contains(mutator.getClass().getName())
					|| excludedRules.stream().anyMatch(excludedRule -> mutator.getIds().contains(excludedRule));

			// debug as it seems Spotless instantiate this quite often / for each file
			if (isExcluded) {
				LOGGER.debug("We exclude '{}'", mutator.getIds());
			} else {
				LOGGER.debug("We include '{}'", mutator.getIds());
			}

			return !isExcluded;
		}).filter(ct -> {
			if (includeDraft) {
				return true;
			} else if (mutatorsMayComposite.contains(ct)) {
				LOGGER.debug("Draft are not included by default but {} was listed explicitely", ct.getIds());
				return true;
			} else {
				return !ct.isDraft();
			}
		}).collect(Collectors.toList());

	}

	private static Optional<IMutator> loadMutatorFromClass(JavaVersion sourceCodeVersion, String includedRule) {
		try {
			// https://www.baeldung.com/java-check-class-exists
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Class<? extends IMutator> mutatorClass =
					(Class<? extends IMutator>) Class.forName(includedRule, false, classLoader);

			IMutator mutator;
			if (CompositeMutator.class.isAssignableFrom(mutatorClass)) {
				Constructor<? extends IMutator> ctor = mutatorClass.getConstructor(JavaVersion.class);
				mutator = ctor.newInstance(sourceCodeVersion);
			} else {
				Constructor<? extends IMutator> ctor = mutatorClass.getConstructor();
				mutator = ctor.newInstance();
			}

			return Optional.of(mutator);
		} catch (ClassNotFoundException e) {
			LOGGER.debug("includedMutator {} is not present classname", includedRule, e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Unexpected constructor for includedMutator=" + includedRule, e);
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Invalid class for includedMutator=" + includedRule, e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalArgumentException("Issue instanciating includedMutator=" + includedRule, e);
		}
		return Optional.empty();
	}

	private static List<IMutator> unrollCompositeMutators(List<IMutator> mutatorsMayComposite) {
		List<IMutator> mutatorsNotComposite = mutatorsMayComposite;
		while (mutatorsNotComposite.stream().filter(m -> m instanceof CompositeMutator).findAny().isPresent()) {
			mutatorsNotComposite = mutatorsNotComposite.stream().flatMap(m -> {
				if (m instanceof CompositeMutator) {
					return ((CompositeMutator) m).getUnderlyings().stream();
				} else {
					return Stream.of(m);
				}
			}).collect(Collectors.toList());
		}
		return mutatorsNotComposite;
	}

	@Override
	public String getId() {
		return ID_REFACTORER;
	}

	public List<IMutator> getMutators() {
		return mutators;
	}

	@Override
	public String doFormat(String dirtyCode, LineEnding ending) throws IOException {
		LOGGER.debug("{}", this.refactorerProperties);
		String cleanCode = applyTransformers(dirtyCode);
		return fixJavaparserUnexpectedChanges(dirtyCode, cleanCode);
	}

	private String applyTransformers(String dirtyCode) {
		AtomicReference<String> refCleanCode = new AtomicReference<>(dirtyCode);

		// Ensure we compute the compilation-unit only once per String
		AtomicReference<CompilationUnit> optCompilationUnit = new AtomicReference<>();

		JavaParser parser = makeJavaParser();

		mutators.forEach(ct -> {
			LOGGER.debug("Applying {}", ct);

			// Fill cache
			if (optCompilationUnit.get() == null) {
				try {
					String sourceCode = refCleanCode.get();
					CompilationUnit compilationUnit = parseRawCode(parser, sourceCode);
					optCompilationUnit.set(compilationUnit);
				} catch (RuntimeException e) {
					throw new RuntimeException("Issue parsing the code", e);
				}
			}

			CompilationUnit compilationUnit = optCompilationUnit.get();
			boolean walkNodeResult;
			try {
				walkNodeResult = ct.walkNode(compilationUnit);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue with classTransformer: " + ct, e);
			}
			if (walkNodeResult) {
				// Prevent Javaparser polluting the code, as it often impacts comments when building back code from AST,
				// or removing consecutive EOL
				// We rely on javaParser source-code only if the rule has actually impacted the AST
				LOGGER.debug("A rule based on JavaParser actually modified the code");

				// One relevant change: building source-code from the AST
				refCleanCode.set(toString(compilationUnit));

				// Discard cache. It may be useful to prevent issues determining some types in mutated compilationUnits
				optCompilationUnit.set(null);
			}
		});
		return refCleanCode.get();
	}

	public CompilationUnit parseRawCode(JavaParser parser, String sourceCode) {
		ParseResult<CompilationUnit> parsed = parser.parse(sourceCode);
		CompilationUnit compilationUnit = parsed.getResult().get();

		// https://github.com/javaparser/javaparser/issues/3490
		// We register given node for later prettyPrinting
		LexicalPreservingPrinter.setup(compilationUnit);
		return compilationUnit;
	}

	public JavaParser makeJavaParser() {
		// TODO Adjust this flag depending on filtered rules
		boolean isJreOnly = false;
		JavaParser parser = makeDefaultJavaParser(isJreOnly);
		return parser;
	}

	protected String fixJavaparserUnexpectedChanges(String dirtyCode, String cleanCode) throws IOException {
		if (dirtyCode.equals(cleanCode)) {
			// Return the original reference whenever possible
			return dirtyCode;
		}

		String lineEndingChars =
				LineEnding.getOrGuess(engineProperties.getSourceCode().getLineEndingAsEnum(), () -> cleanCode);
		Optional<LineEnding> optLineEnding = LineEnding.determineLineEnding(lineEndingChars);

		if (optLineEnding.isEmpty()) {
			// Unable to guess the lineEnding: it may be a very small file
			return cleanCode;
		}

		List<String> dirtyRows = Arrays.asList(dirtyCode.split(lineEndingChars, -1));
		List<String> cleanRows = Arrays.asList(cleanCode.split(lineEndingChars, -1));
		Patch<String> diff = DiffUtils.diff(dirtyRows, cleanRows);

		assertPatchIsValid(dirtyRows, cleanRows, diff);

		List<AbstractDelta<String>> fixedDelta = computeFixedDelta(diff);

		if (fixedDelta.isEmpty()) {
			// After discarding irrelevant changes from Javaparser, there is no change left: this ILinterFixer did not
			// change anything
			return dirtyCode;
		}

		Patch<String> fixedPatch = new Patch<>();
		fixedDelta.forEach(fixedPatch::addDelta);

		List<String> fixedPatchApplied;
		try {
			fixedPatchApplied = diff.applyTo(dirtyRows);
		} catch (PatchFailedException e) {
			throw new RuntimeException(e);
		}

		return fixedPatchApplied.stream().collect(Collectors.joining(lineEndingChars));
	}

	public List<AbstractDelta<String>> computeFixedDelta(Patch<String> diff) {
		// We will filter some removed rows as they are not legitimate changes from Javaparser
		// TODO In fact, we should build the patch from the original file, post AST, pre custom modification
		List<AbstractDelta<String>> fixedDelta = diff.getDeltas().stream().filter(p -> {
			if (p.getType() == DeltaType.DELETE) {
				List<String> sourceLines = p.getSource().getLines();
				Set<String> unique = sourceLines.stream().distinct().collect(Collectors.toSet());
				if (unique.size() == 1) {
					String uniqueTrimmer = unique.iterator().next().trim();
					// if empty: it corresponds to consecutive EOL
					// if '*': it corresponds to empty rows in a Javadoc
					if (uniqueTrimmer.isEmpty() || "*".equals(uniqueTrimmer)) {
						return false;
					}
				}

				return true;
			} else {
				return true;
			}
		}).collect(Collectors.toList());
		return fixedDelta;
	}

	// This should probably be removed. We keep it only until we valid the Diff library is working OK
	// We check the patch is valid
	protected void assertPatchIsValid(List<String> dirtyRows, List<String> cleanRows, Patch<String> diff) {
		List<String> patchApplied;
		try {
			patchApplied = diff.applyTo(dirtyRows);
		} catch (PatchFailedException e) {
			throw new RuntimeException(e);
		}
		if (!cleanRows.equals(patchApplied)) {
			throw new IllegalArgumentException("Issue aplying the patch");
		}
		List<String> patchRestored = diff.restore(cleanRows);
		if (!dirtyRows.equals(patchRestored)) {
			throw new IllegalArgumentException("Issue restoring the patch");
		}
	}

	protected String toString(CompilationUnit compilationUnit) {
		return LexicalPreservingPrinter.print(compilationUnit);
	}

	public static JavaParser makeDefaultJavaParser(boolean jreOnly) {
		ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);

		JavaSymbolSolver symbolResolver = new JavaSymbolSolver(new CombinedTypeSolver(reflectionTypeSolver));
		ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(symbolResolver);
		JavaParser parser = new JavaParser(configuration);
		return parser;
	}
}
