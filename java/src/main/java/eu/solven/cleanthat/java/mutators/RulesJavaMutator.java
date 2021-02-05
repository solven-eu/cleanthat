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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.rules.CreateTempFilesUsingNio;
import eu.solven.cleanthat.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.IJdkVersionConstants;
import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.UseIsEmptyOnCollections;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.meta.VersionWrapper;

/**
 * Bridges to Eclipse formatting engine
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class RulesJavaMutator implements ISourceCodeFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesJavaMutator.class);

	private final ILanguageProperties languageProperties;
	private final CleanthatJavaProcessorProperties properties;

	private static final List<IClassTransformer> ALL_TRANSFORMERS = Arrays.asList(new EnumsWithoutEquals(),
			new PrimitiveBoxedForString(),
			new UseIsEmptyOnCollections(),
			new CreateTempFilesUsingNio());

	private final List<IClassTransformer> transformers;

	public RulesJavaMutator(ILanguageProperties languageProperties, CleanthatJavaProcessorProperties properties) {
		this.languageProperties = languageProperties;
		this.properties = properties;

		VersionWrapper languageVersion = new VersionWrapper(languageProperties.getLanguageVersion());
		this.transformers = ALL_TRANSFORMERS.stream().filter(ct -> {
			VersionWrapper transformerVersion = new VersionWrapper(ct.minimalJavaVersion());
			return languageVersion.compareTo(transformerVersion) >= 0;
		}).collect(Collectors.toList());
	}

	public List<IClassTransformer> getTransformers() {
		return transformers;
	}

	@Override
	public String doFormat(String code, LineEnding eolToApply) throws IOException {
		LOGGER.debug("{}", this.properties);
		AtomicReference<String> codeRef = new AtomicReference<>(code);

		// Ensure we compute the compilation-unit only once per String
		AtomicReference<CompilationUnit> optCompilationUnit = new AtomicReference<>();

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
				// Synchronized until: https://github.com/javaparser/javaparser/issues/3050
				synchronized (StaticJavaParser.class) {
					try {
						optCompilationUnit.set(StaticJavaParser.parse(codeRef.get()));
					} catch (RuntimeException e) {
						throw new RuntimeException("Issue parsing the code", e);
					}
				}
			}

			// Prevent Javaparser polluting the code, as it often impacts comments when building back code from AST
			// We rely on javaParser source-code only if the rule has actually impacted the AST
			AtomicBoolean hasImpacted = new AtomicBoolean();

			CompilationUnit compilationUnit = optCompilationUnit.get();
			if (ct.walkNode(compilationUnit)) {
				hasImpacted.set(true);
				LOGGER.info("It is a hit");
			}

			if (hasImpacted.get()) {
				// One relevant change: building back from the AST
				codeRef.set(compilationUnit.toString());

				// Discard cache
				optCompilationUnit.set(null);
			}
		});
		return codeRef.get();
	}
}
