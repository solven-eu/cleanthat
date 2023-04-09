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
package eu.solven.cleanthat.engine.java.refactorer.cases;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Enables listing testCases dynamically from a testClass static method
 * 
 * @author Benoit Lacelle
 *
 */
// https://www.baeldung.com/parameterized-tests-junit-5#8-custom-annotation
class CasesArgumentsProvider implements ArgumentsProvider {
	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		return context.getTestClass()
				.map(this::getCases)
				.orElseThrow(() -> new IllegalArgumentException("Failed to load test arguments"));
	}

	private Stream<Arguments> getCases(Class<?> clazz) {
		AParameterizesRefactorerCases<?, ?> casesInstance;
		try {
			casesInstance = (AParameterizesRefactorerCases<?, ?>) clazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
			throw new IllegalStateException("Issue with " + clazz, e);
		}

		try {
			return AParameterizesRefactorerCases.listCases(casesInstance);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue with " + clazz, e);
		}
	}
}