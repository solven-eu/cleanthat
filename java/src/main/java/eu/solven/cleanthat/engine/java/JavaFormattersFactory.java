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
package eu.solven.cleanthat.engine.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
public class JavaFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormattersFactory.class);

	public JavaFormattersFactory(ConfigHelpers configHelpers) {
		super(configHelpers);
	}

	@Override
	public String getEngine() {
		return "java";
	}

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("java");
	}

	@Override
	public ILintFixer makeLintFixer(CleanthatStepProperties rawProcessor,
			IEngineProperties languageProperties,
			CleanthatSession cleanthatSession) {
		ILintFixerWithId processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		// override with explicit configuration
		Map<String, ?> parameters = getParameters(rawProcessor);

		LOGGER.debug("Processing: {}", engine);

		switch (engine) {
		case "refactorer": {
			JavaRefactorerProperties processorConfig = convertValue(parameters, JavaRefactorerProperties.class);
			processor = new JavaRefactorer(languageProperties, processorConfig);
			break;
		}

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		if (!processor.getId().equals(engine)) {
			throw new IllegalStateException("Inconsistency: " + processor.getId() + " vs " + engine);
		}

		return processor;
	}

	@Override
	public CleanthatEngineProperties makeDefaultProperties() {
		CleanthatEngineProperties languageProperties = new CleanthatEngineProperties();

		languageProperties.setEngine(getEngine());

		List<CleanthatStepProperties> processors = new ArrayList<>();

		// Apply rules
		{
			processors.add(CleanthatStepProperties.builder()
					.id("refactorer")
					.parameters(JavaRefactorerProperties.defaults())
					.build());
		}

		languageProperties.setSteps(processors);

		return languageProperties;
	}

}
