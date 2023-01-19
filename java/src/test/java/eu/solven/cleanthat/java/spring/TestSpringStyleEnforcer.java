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
package eu.solven.cleanthat.java.spring;

import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.engine.java.refactorer.JavaparserDirtyMe;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;
import eu.solven.cleanthat.engine.java.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.engine.java.spring.SpringJavaStyleEnforcer;
import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;

import java.io.IOException;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

public class TestSpringStyleEnforcer {
	@Ignore("TODO")
	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		String dirtyCode = LocalClassTestHelper.loadClassAsString(JavaparserDirtyMe.class);

		// We need any styleEnforce to workaround Javaparser weaknesses
		IStyleEnforcer styleEnforcer =
				new SpringJavaStyleEnforcer(new SourceCodeProperties(), new SpringJavaFormatterProperties());

		Optional<LineEnding> lineEnding = LineEnding.determineLineEnding(dirtyCode);
		Assertions.assertThat(lineEnding).isPresent();
		styleEnforcer.doFormat(dirtyCode, lineEnding.get());
	}
}
