/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.language.spotless;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.ILintFixerWithPath;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.PathAndContent;
import eu.solven.cleanthat.spotless.EnrichedFormatter;
import eu.solven.cleanthat.spotless.ExecuteSpotless;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link ILintFixer} for Spotless engine
 * 
 * @author Benoit Lacelle
 *
 */
public class SpotlessLintFixer implements ILintFixerWithId, ILintFixerWithPath {
	final List<EnrichedFormatter> formatters;

	public SpotlessLintFixer(List<EnrichedFormatter> formatters) {
		this.formatters = formatters;
	}

	@Override
	public String doFormat(PathAndContent pathAndContent, LineEnding ending) throws IOException {
		AtomicReference<PathAndContent> output = new AtomicReference<>(pathAndContent);

		formatters.stream().forEach(f -> {
			ExecuteSpotless executeSpotless = new ExecuteSpotless(f);

			if (executeSpotless.acceptPath(pathAndContent.getPath())) {
				output.set(pathAndContent.withContent(executeSpotless.doStuff(output.get())));
			}
		});

		return output.get().getContent();
	}

	@Override
	public String getId() {
		return CleanthatSpotlessStepParametersProperties.ENGINE_ID;
	}

	@Override
	public String toString() {
		return "Formatters: " + formatters.toString();
	}

	@Override
	public String doFormat(String content, LineEnding ending) throws IOException {
		return doFormat(new PathAndContent(null, content), ending);
	}

}
