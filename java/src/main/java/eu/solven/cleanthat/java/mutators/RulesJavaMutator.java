/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.java.mutators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.formatter.ILintFixerHelpedByCodeStyleFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.meta.VersionWrapper;
import eu.solven.cleanthat.language.java.rules.mutators.CreateTempFilesUsingNio;
import eu.solven.cleanthat.language.java.rules.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.language.java.rules.mutators.ModifierOrder;
import eu.solven.cleanthat.language.java.rules.mutators.OptionalNotEmpty;
import eu.solven.cleanthat.language.java.rules.mutators.PrimitiveBoxedForString;
import eu.solven.cleanthat.language.java.rules.mutators.UseDiamondOperator;
import eu.solven.cleanthat.language.java.rules.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.language.java.rules.mutators.UseIsEmptyOnCollections;
import eu.solven.cleanthat.language.java.rules.mutators.VariableEqualsConstant;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class RulesJavaMutator implements ILintFixerHelpedByCodeStyleFixer, ILintFixerWithId {

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesJavaMutator.class);

	private final ILanguageProperties languageProperties;
	private final JavaRulesMutatorProperties properties;

	private static final List<IClassTransformer> ALL_TRANSFORMERS = Arrays.asList(new CreateTempFilesUsingNio(),
			new EnumsWithoutEquals(),
			new PrimitiveBoxedForString(),
			new OptionalNotEmpty(),
			new ModifierOrder(),
			new UseDiamondOperator(),
			new UseDiamondOperatorJdk8(),
			new UseIsEmptyOnCollections(),
			new VariableEqualsConstant());

	private final List<IClassTransformer> transformers;

	private Optional<IStyleEnforcer> optCodeStyleFixer = Optional.empty();

	public RulesJavaMutator(ILanguageProperties languageProperties, JavaRulesMutatorProperties properties) {
		this.languageProperties = languageProperties;
		this.properties = properties;

		VersionWrapper languageVersion = new VersionWrapper(languageProperties.getLanguageVersion());

		List<String> excludedRules = properties.getExcluded();
		boolean productionReadyOnly = properties.isProductionReadyOnly();

		this.transformers = ALL_TRANSFORMERS.stream().filter(ct -> {
			VersionWrapper transformerVersion = new VersionWrapper(ct.minimalJavaVersion());

			// Ensure the code has lower version than the rule minimalVersion
			return languageVersion.compareTo(transformerVersion) >= 0;
		}).filter(ct -> {
			boolean isExclusion = excludedRules.stream()
					.filter(excludedRule -> ct.getIds().contains(excludedRule))
					.findAny()
					.isPresent();

			if (isExclusion) {
				LOGGER.info("We exclude '{}'", ct.getIds());
			}

			return !isExclusion;
		}).filter(ct -> {
			if (!productionReadyOnly) {
				return true;
			} else {
				return ct.isProductionReady();
			}
		}).collect(Collectors.toList());

		this.transformers.forEach(ct -> {
			LOGGER.debug("Using transformer: {}", ct.getIds());
		});
	}

	@Override
	public String getId() {
		return "rules";
	}

	public List<IClassTransformer> getTransformers() {
		return transformers;
	}

	@Override
	public void registerCodeStyleFixer(IStyleEnforcer codeStyleFixer) {
		// TODO This could be in INFO, but it is called once per file (unexpectedly)
		LOGGER.debug("We register {} into {}", codeStyleFixer, this);
		this.optCodeStyleFixer = Optional.of(codeStyleFixer);
	}

	@Override
	public String doFormat(String dirtyCode, LineEnding eolToApply) throws IOException {
		LOGGER.debug("{}", this.properties);
		AtomicReference<String> refCleanCode = new AtomicReference<>(dirtyCode);

		// Ensure we compute the compilation-unit only once per String
		AtomicReference<CompilationUnit> optCompilationUnit = new AtomicReference<>();

		JavaParser parser = makeDefaultJavaParser();

		transformers.forEach(ct -> {
			int ruleMinimal = IJdkVersionConstants.ORDERED.indexOf(ct.minimalJavaVersion());
			int codeVersion = IJdkVersionConstants.ORDERED.indexOf(languageProperties.getLanguageVersion());

			if (ruleMinimal > codeVersion) {
				LOGGER.debug("We skip {} as {} > {}",
						ct,
						ct.minimalJavaVersion(),
						languageProperties.getLanguageVersion());
				return;
			}

			if (!ct.minimalJavaVersion().equals(languageProperties.getLanguageVersion())) {
				LOGGER.debug("TODO Implement a rule to skip incompatible rules");
			}

			LOGGER.debug("Applying {}", ct);

			// Fill cache
			if (optCompilationUnit.get() == null) {
				try {
					String sourceCode = refCleanCode.get();
					ParseResult<CompilationUnit> parsed = parser.parse(sourceCode);
					optCompilationUnit.set(parsed.getResult().get());
				} catch (RuntimeException e) {
					throw new RuntimeException("Issue parsing the code", e);
				}
			}

			CompilationUnit compilationUnit = optCompilationUnit.get();
			if (ct.walkNode(compilationUnit)) {
				// Prevent Javaparser polluting the code, as it often impacts comments when building back code from AST,
				// or removing consecutive EOL
				// We rely on javaParser source-code only if the rule has actually impacted the AST
				LOGGER.info("It is a hit");

				// One relevant change: building source-code from the AST
				refCleanCode.set(toString(compilationUnit));

				// Discard cache. It may be useful to prevent issues determining some types in mutated compilationUnits
				optCompilationUnit.set(null);
			}
		});
		return fixJavaparserUnexpectedChanges(dirtyCode, refCleanCode.get());
	}

	protected String fixJavaparserUnexpectedChanges(String dirtyCode, String cleanCode) throws IOException {
		if (dirtyCode.equals(cleanCode)) {
			// Return the original reference whenever possible
			return dirtyCode;
		}

		Optional<LineEnding> optLineEnding = LineEnding.determineLineEnding(dirtyCode);

		if (optLineEnding.isEmpty()) {
			// Unable to guess the lineEnding: it may be a very small file
			return cleanCode;
		}

		LineEnding lineEnding = optLineEnding.get();
		if (optCodeStyleFixer.isPresent()) {
			// We are provided a way to format the code early
			cleanCode = optCodeStyleFixer.get().doFormat(cleanCode, lineEnding);
		}

		String lineEndingChars = lineEnding.getChars();

		List<String> dirtyRows = Arrays.asList(dirtyCode.split(lineEndingChars, -1));
		List<String> cleanRows = Arrays.asList(cleanCode.split(lineEndingChars, -1));
		Patch<String> diff = DiffUtils.diff(dirtyRows, cleanRows, new MyersDiff<String>());

		checkPatchIsValid(dirtyRows, cleanRows, diff);

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
	protected void checkPatchIsValid(List<String> dirtyRows, List<String> cleanRows, Patch<String> diff) {
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
		return compilationUnit.toString();
	}

	public static JavaParser makeDefaultJavaParser() {
		JavaSymbolSolver symbolResolver = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver()));
		ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(symbolResolver);
		JavaParser parser = new JavaParser(configuration);
		return parser;
	}
}
