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
package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.Collection;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * Helper methods in the context of a mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanThatMavenHelper {
	protected CleanThatMavenHelper() {
		// hidden
	}

	public static MavenCodeCleaner makeCodeCleaner(ApplicationContext appContext) {
		Collection<ObjectMapper> objectMappers = appContext.getBeansOfType(ObjectMapper.class).values();
		Collection<IEngineLintFixerFactory> factories =
				appContext.getBeansOfType(IEngineLintFixerFactory.class).values();
		ICodeProviderFormatter codeProviderFormatter = appContext.getBean(ICodeProviderFormatter.class);
		return new MavenCodeCleaner(objectMappers, factories, codeProviderFormatter);
	}

	// Process the root of current module
	public static ICodeProviderWriter makeCodeProviderWriter(ACleanThatMojo cleanThatCleanThatMojo) {
		File baseDir = cleanThatCleanThatMojo.getBaseDir();
		return new FileSystemCodeProvider(baseDir.toPath());
	}
}
