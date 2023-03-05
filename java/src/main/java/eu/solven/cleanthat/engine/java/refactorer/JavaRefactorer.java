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
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * This class is dedicated to refactoring. Most mutators will refactor code to a better (e.g. shorter, faster, safer,
 * etc) but with [strictly|roughly] equivalent runtime behavior.
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class JavaRefactorer extends AAstRefactorer<Node, JavaParser, Node, IJavaparserMutator> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaRefactorer.class);

	// It is ambiguous to give access to access on the classLoader, as it would give insights to custom classes
	public static final boolean JAVAPARSER_JRE_ONLY = true;

	private final IEngineProperties engineProperties;
	private final JavaRefactorerProperties refactorerProperties;

	public static final Set<String> getAllIncluded() {
		return new AllIncludingDraftSingleMutators(JavaVersion.parse(IJdkVersionConstants.LAST)).getUnderlyingIds();
	}

	public JavaRefactorer(IEngineProperties engineProperties, JavaRefactorerProperties properties) {
		super(filterRules(engineProperties, properties).stream()
				.filter(c -> IJavaparserMutator.class.isAssignableFrom(c.getClass()))
				.map(IJavaparserMutator.class::cast)
				.collect(Collectors.toList()));

		this.engineProperties = engineProperties;
		this.refactorerProperties = properties;
	}

	@Override
	public String getId() {
		return JavaRefactorerStep.ID_REFACTORER;
	}

	@Deprecated(since = "Not used anymore. Kept for retrocompatiblity of users (e.g. Spotless)", forRemoval = true)
	public String doFormat(String dirtyCode, LineEnding eol) throws IOException {
		return doFormat(dirtyCode);
	}

	@Override
	public String doFormat(String dirtyCode) throws IOException {
		LOGGER.debug("{}", this.refactorerProperties);
		var cleanCode = applyTransformers(dirtyCode);
		return fixJavaparserUnexpectedChanges(dirtyCode, cleanCode);
	}

	@Override
	public Optional<Node> parseSourceCode(JavaParser parser, String sourceCode) {
		ParseResult<CompilationUnit> parsed = parser.parse(sourceCode);

		if (!parsed.isSuccessful()) {
			// JavaParser does not manage instanceof patterns as of JP:3.25
			LOGGER.warn("Issue parsing this. {} problems. First problem: {}",
					parsed.getProblems().size(),
					parsed.getProblem(0));
			return Optional.empty();
		}

		var compilationUnit = parsed.getResult().get();

		// https://github.com/javaparser/javaparser/issues/3490
		// We register given node for later prettyPrinting
		LexicalPreservingPrinter.setup(compilationUnit);
		return Optional.of(compilationUnit);
	}

	@Override
	protected JavaParser makeAstParser() {
		// TODO Adjust this flag depending on filtered rules
		var isJreOnly = JAVAPARSER_JRE_ONLY;
		var parser = makeDefaultJavaParser(isJreOnly);
		return parser;
	}

	protected String fixJavaparserUnexpectedChanges(String dirtyCode, String cleanCode) throws IOException {
		if (dirtyCode.equals(cleanCode)) {
			// Return the original reference whenever possible
			return dirtyCode;
		}

		var lineEndingChars =
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
					var uniqueTrimmer = unique.iterator().next().trim();
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

	@Override
	protected String toString(Node compilationUnit) {
		return LexicalPreservingPrinter.print(compilationUnit);
	}

	public static TypeSolver makeDefaultTypeSolver(boolean jreOnly) {
		if (jreOnly != JAVAPARSER_JRE_ONLY) {
			LOGGER.warn("We force jreOnly to {}", JAVAPARSER_JRE_ONLY);
			jreOnly = JAVAPARSER_JRE_ONLY;
		}

		var reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);
		var memoryTypeSolver = new MemoryTypeSolver();
		memoryTypeSolver.setParent(reflectionTypeSolver);
		return memoryTypeSolver;
	}

	public static JavaParser makeDefaultJavaParser(boolean jreOnly) {
		var reflectionTypeSolver = makeDefaultTypeSolver(jreOnly);

		var symbolResolver = new JavaSymbolSolver(reflectionTypeSolver);

		var configuration = new ParserConfiguration().setSymbolResolver(symbolResolver);
		var parser = new JavaParser(configuration);
		return parser;
	}

	@Override
	protected boolean isValidResultString(JavaParser parser, String resultAsString) {
		return parser.parse(resultAsString).isSuccessful();
	}
}
