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
package eu.solven.cleanthat.engine;

import java.util.List;
import java.util.stream.IntStream;

import com.google.common.base.MoreObjects;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.IEngineProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Computed processors, to be applicable to any file of a given repository
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class EngineAndLinters implements AutoCloseable {

	final IEngineProperties engineProperties;
	final List<ILintFixer> linters;

	@Override
	public String toString() {
		var builder = MoreObjects.toStringHelper(this).add("engine", engineProperties.getEngine());

		IntStream.range(0, linters.size()).forEach(i -> builder.add("step_" + i, linters.get(i)));

		return builder.toString();
	}

	@Override
	public void close() {
		linters.forEach(lf -> {
			if (lf instanceof AutoCloseable) {
				try {
					((AutoCloseable) lf).close();
				} catch (Exception e) {
					LOGGER.warn("Issue closing {}", lf, e);
				}
			}
		});
	}
}
