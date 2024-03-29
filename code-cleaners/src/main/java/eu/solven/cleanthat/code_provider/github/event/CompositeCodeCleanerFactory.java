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
package eu.solven.cleanthat.code_provider.github.event;

import java.util.List;
import java.util.Optional;

import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;

/**
 * Factory for {@link IGitRefCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class CompositeCodeCleanerFactory implements ICodeCleanerFactory {

	final List<ICodeCleanerFactory> specializedFactories;

	public CompositeCodeCleanerFactory(List<ICodeCleanerFactory> specializedFactories) {
		this.specializedFactories = specializedFactories;
	}

	@Override
	public Optional<IGitRefCleaner> makeCleaner(Object somethingInteresting) {
		return specializedFactories.stream()
				.map(f -> f.makeCleaner(somethingInteresting))
				.flatMap(Optional::stream)
				.findFirst();
	}

}
