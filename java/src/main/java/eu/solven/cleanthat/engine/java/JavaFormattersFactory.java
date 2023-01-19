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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.EngineProperties;
import eu.solven.cleanthat.engine.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatterConfiguration;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatterProcessorProperties;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.engine.java.spring.SpringJavaStyleEnforcer;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.IEngineProperties;
import eu.solven.pepper.collection.PepperMapHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
public class JavaFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormattersFactory.class);

	private static final int DEFAULT_CACHE_SIZE = 16;

	// Prevents parsing/loading remote configuration on each parse
	// We expect a low number of different configurations
	// Beware this can lead to race-conditions/thread-safety issues into EclipseJavaFormatter
	final Cache<EclipseFormatterCacheKey, EclipseJavaFormatterConfiguration> configToEngine =
			CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build();

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

	@VisibleForTesting
	protected long getCacheSize() {
		return configToEngine.size();
	}

	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			IEngineProperties languageProperties,
			ICodeProvider codeProvider) {
		ILintFixerWithId processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		// override with explicit configuration
		Map<String, ?> parameters = getParameters(rawProcessor);

		LOGGER.debug("Processing: {}", engine);

		switch (engine) {
		case "spring_formatter": {
			SpringJavaFormatterProperties processorConfig =
					convertValue(parameters, SpringJavaFormatterProperties.class);
			processor = new SpringJavaStyleEnforcer(languageProperties.getSourceCode(), processorConfig);
			break;
		}
		case "rules": {
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
	public EngineProperties makeDefaultProperties() {
		EngineProperties languageProperties = new EngineProperties();

		languageProperties.setEngine(getEngine());

		List<Map<String, ?>> processors = new ArrayList<>();

		// Apply rules
		{
			JavaRefactorerProperties engineParameters = new JavaRefactorerProperties();

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, "rules")
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		// Eclipse formatting is done last, to clean after rules
		{
			Map<String, ?> processorProperties = makeEclipseFormatterDefaultProperties();
			processors.add(processorProperties);
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}

	public static Map<String, ?> makeEclipseFormatterDefaultProperties() {
		EclipseJavaFormatterProcessorProperties engineParameters = new EclipseJavaFormatterProcessorProperties();

		Map<String, ?> processorProperties = ImmutableMap.<String, Object>builder()
				.put(KEY_ENGINE, EclipseJavaFormatter.ID)
				.put(KEY_PARAMETERS, engineParameters)
				.build();
		return processorProperties;
	}

}
