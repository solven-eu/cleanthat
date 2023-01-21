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

import com.diffplug.spotless.Formatter;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.spotless.ExecuteSpotless;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SpotlessLintFixer implements ILintFixerWithId {
	final List<Formatter> formatters;

	public SpotlessLintFixer(List<Formatter> formatters) {
		this.formatters = formatters;
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		AtomicReference<String> output = new AtomicReference<>(code);

		formatters.stream().forEach(f -> output.set(new ExecuteSpotless(f).doStuff("", output.get())));

		return output.get();
	}

	@Override
	public String getId() {
		return "spotless";
	}

}
