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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.VersionWrapper;
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

	private static final Supplier<List<IMutator>> ALL_TRANSFORMERS =
			Suppliers.memoize(() -> ImmutableList.copyOf(new MutatorsScanner().getMutators()));

	private final List<IMutator> transformers;

	public static final Set<String> getAllIncluded() {
		return ALL_TRANSFORMERS.get()
				.stream()
				.flatMap(ct -> ct.getIds().stream())
				.sorted()
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public JavaRefactorer(IEngineProperties engineProperties, JavaRefactorerProperties properties) {
		this.engineProperties = engineProperties;
		this.refactorerProperties = properties;

		VersionWrapper engineVersion = new VersionWrapper(engineProperties.getEngineVersion());

		List<String> includedRules = properties.getIncluded();
		List<String> excludedRules = properties.getExcluded();
		boolean productionReadyOnly = properties.isProductionReadyOnly();

		// TODO Enable a custom rule in includedRules (e.g. to load from a 3rd party JAR)
		this.transformers = ALL_TRANSFORMERS.get().stream().filter(ct -> {
			VersionWrapper transformerVersion = new VersionWrapper(ct.minimalJavaVersion());

			// Ensure the code has higher-or-equal version than the rule minimalVersion
			return engineVersion.compareTo(transformerVersion) >= 0;
		}).filter(ct -> {
			boolean isExcluded = excludedRules.stream().anyMatch(excludedRule -> ct.getIds().contains(excludedRule));

			// If the inclusion list if
			boolean isIncluded = includedRules.isEmpty() || includedRules.stream()
					.filter(includedRule -> JavaRefactorerProperties.WILDCARD.equals(includedRule)
							|| ct.getIds().contains(includedRule))
					.findAny()
					.isPresent();

			if (isExcluded) {
				LOGGER.info("We exclude '{}'", ct.getIds());
			} else if (!isIncluded) {
				LOGGER.info("We do not include '{}'", ct.getIds());
			}

			return !isExcluded && isIncluded;
		}).filter(ct -> {
			if (productionReadyOnly) {
				return ct.isProductionReady();
			} else {
				return true;
			}
		}).collect(Collectors.toList());

		this.transformers.forEach(ct -> {
			LOGGER.debug("Using transformer: {}", ct.getIds());
		});
	}

	@Override
	public String getId() {
		return ID_REFACTORER;
	}

	public List<IMutator> getMutators() {
		return transformers;
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

		transformers.stream().filter(ct -> {
			JavaVersion ruleMinimal = JavaVersion.parse(ct.minimalJavaVersion());
			JavaVersion codeVersion = JavaVersion.parse(engineProperties.getEngineVersion());

			if (codeVersion.isBefore(ruleMinimal)) {
				LOGGER.debug("We skip {} as {} < {}", ct, codeVersion, ruleMinimal);
				return false;
			}

			return true;
		}).forEach(ct -> {
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
