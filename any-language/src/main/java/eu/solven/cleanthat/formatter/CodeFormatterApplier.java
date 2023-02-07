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
package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.engine.EngineAndLinters;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public class CodeFormatterApplier implements ICodeFormatterApplier {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeFormatterApplier.class);

	public static final AtomicInteger NB_EXCEPTIONS = new AtomicInteger();

	@Override
	public String applyProcessors(EngineAndLinters engineAndSteps, PathAndContent pathAndContent) throws IOException {
		String code = pathAndContent.getContent();
		Path filepath = pathAndContent.getPath();
		AtomicReference<String> outputRef = new AtomicReference<>(code);

		IEngineProperties engineProperties = engineAndSteps.getEngineProperties();
		engineAndSteps.getLinters().forEach(linter -> {
			try {
				String output = applyProcessor(engineProperties, linter, pathAndContent);
				if (output == null) {
					throw new IllegalStateException("Null code.");
				}
				String input = outputRef.get();
				if (!input.equals(output)) {
					// Beware each processor may change a file, but the combined changes leads to a no change (e.g. the
					// final formatting step may clean all previous not relevant changes)
					LOGGER.debug("Mutated a file given: {}", linter);
					outputRef.set(output);
				}
			} catch (IOException | RuntimeException e) {
				NB_EXCEPTIONS.incrementAndGet();
				// Log and move to next processor
				LOGGER.warn("Issue over file='" + filepath
						+ "' with linter="
						+ linter
						+ " in engine={}. Please report it to: "
						+ "https://github.com/solven-eu/cleanthat/issues", engineProperties.getEngine(), e);
			}
		});
		return outputRef.get();
	}

	protected String applyProcessor(IEngineProperties languageProperties,
			ILintFixer formatter,
			PathAndContent pathAndContent) throws IOException {
		Objects.requireNonNull(pathAndContent, "pathAndContent should not be null");

		LineEnding lineEnding = languageProperties.getSourceCode().getLineEndingAsEnum();

		if (lineEnding == LineEnding.GIT) {
			// see GitAttributesLineEndings_InMemory
			LOGGER.warn("We switch lineEnding from {} to {}", lineEnding, LineEnding.NATIVE);
			lineEnding = LineEnding.NATIVE;
		} else if (lineEnding == LineEnding.KEEP) {
			String code = pathAndContent.getContent();
			lineEnding = LineEnding.determineLineEnding(code).orElse(LineEnding.NATIVE);
		}

		if (formatter instanceof ILintFixerWithPath) {
			return ((ILintFixerWithPath) formatter).doFormat(pathAndContent, lineEnding);
		} else {
			return formatter.doFormat(pathAndContent.getContent(), lineEnding);
		}
	}
}
