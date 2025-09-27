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
package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import eu.solven.cleanthat.config.IDocumentationConstants;
import eu.solven.cleanthat.engine.EngineAndLinters;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.language.IEngineProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class CodeFormatterApplier implements ICodeFormatterApplier {
	public static final AtomicInteger NB_EXCEPTIONS = new AtomicInteger();

	@Override
	public String applyProcessors(EngineAndLinters engineAndSteps, PathAndContent pathAndContent) throws IOException {
		var code = pathAndContent.getContent();
		var filepath = pathAndContent.getPath();
		var outputRef = new AtomicReference<>(code);

		var engineProperties = engineAndSteps.getEngineProperties();
		engineAndSteps.getLinters().forEach(linter -> {
			try {
				var output = applyProcessor(engineProperties, linter, pathAndContent);
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
						+ IDocumentationConstants.URL_REPO
						+ "/issues", engineProperties.getEngine(), e);
			}
		});
		return outputRef.get();
	}

	protected String applyProcessor(IEngineProperties engineProperties,
			ILintFixer lintFixer,
			PathAndContent pathAndContent) throws IOException {
		Objects.requireNonNull(pathAndContent, "pathAndContent should not be null");

		if (lintFixer instanceof ILintFixerWithPath) {
			return ((ILintFixerWithPath) lintFixer).doFormat(pathAndContent);
		} else {
			return lintFixer.doFormat(pathAndContent.getContent());
		}
	}
}
