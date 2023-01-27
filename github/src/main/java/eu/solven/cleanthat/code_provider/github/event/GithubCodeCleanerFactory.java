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
package eu.solven.cleanthat.code_provider.github.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import java.util.List;
import java.util.Optional;

/**
 * {@link ICodeCleanerFactory} specialized for GitHub
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubCodeCleanerFactory implements ICodeCleanerFactory {
	final List<ObjectMapper> objectMappers;
	final List<IEngineLintFixerFactory> factories;
	final ICodeProviderFormatter formatterProvider;

	public GithubCodeCleanerFactory(List<ObjectMapper> objectMappers,
			List<IEngineLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.factories = factories;
		this.formatterProvider = formatterProvider;
	}

	@Override
	public Optional<IGitRefCleaner> makeCleaner(Object somethingInteresting) {
		if (somethingInteresting instanceof GithubAndToken) {
			GithubRefCleaner refCleaner = new GithubRefCleaner(objectMappers,
					factories,
					formatterProvider,
					(GithubAndToken) somethingInteresting);
			return Optional.of(refCleaner);
		}
		return Optional.empty();
	}
}
