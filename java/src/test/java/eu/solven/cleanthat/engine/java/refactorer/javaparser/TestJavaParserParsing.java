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
package eu.solven.cleanthat.engine.java.refactorer.javaparser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserTestCases;

public class TestJavaParserParsing extends AJavaparserTestCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestJavaParserParsing.class);

	final IMutator mutator = new LocalVariableTypeInference();

	@Test
	public void testEachFileInFolder() throws IOException {
		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
		var resources = patternResolver.getResources("classpath*:" + "/source/do_not_format_me/JavaParser_Parsing/*");

		for (Resource resource : resources) {
			LOGGER.info("Processing: {}", resource);
			var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

			if ("SealedClassTests.java".equals(resource.getFilename())) {
				// sealed classes are not managed by JP3.25
				Assertions.assertThatThrownBy(() -> parseCompilationUnit(mutator, asString))
						.isInstanceOf(IllegalArgumentException.class);
			} else {
				parseCompilationUnit(mutator, asString);
			}
		}
	}
}
