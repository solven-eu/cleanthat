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
package eu.solven.cleanthat.engine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * Computed processors, to be applicable to any file of a given repository
 * 
 * @author Benoit Lacelle
 *
 */
public class EngineAndLinters implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(EngineAndLinters.class);

	final IEngineProperties engineProperties;
	final List<ILintFixer> linters;

	public EngineAndLinters(IEngineProperties engineProperties, List<ILintFixer> linters) {
		this.engineProperties = engineProperties;
		this.linters = linters;
	}

	public IEngineProperties getEngineProperties() {
		return engineProperties;
	}

	public List<ILintFixer> getLinters() {
		return linters;
	}

	@Override
	public String toString() {
		ToStringHelper builder = MoreObjects.toStringHelper(this).add("engine", engineProperties.getEngine());

		for (int i = 0; i < linters.size(); i++) {
			builder.add("step_" + i, linters.get(i).getClass().getSimpleName());
		}

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
