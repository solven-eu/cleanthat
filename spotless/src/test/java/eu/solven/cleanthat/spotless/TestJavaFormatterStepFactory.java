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
package eu.solven.cleanthat.spotless;

import java.nio.file.Files;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.diffplug.spotless.FormatterStep;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.language.JavaFormatterFactory;
import eu.solven.cleanthat.spotless.language.JavaFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;

public class TestJavaFormatterStepFactory {
	final JavaFormatterFactory javaFormatterFactory = new JavaFormatterFactory();
	final ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
	final SpotlessFormatterProperties formatterProperties =
			SpotlessFormatterProperties.builder().format("java").build();

	final JavaFormatterStepFactory stepFactory =
			new JavaFormatterStepFactory(javaFormatterFactory, codeProvider, formatterProperties);;

	@Test
	public void testLicenseHeaderYearMode_noLicense() throws Exception {
		SpotlessStepParametersProperties stepParameters = new SpotlessStepParametersProperties();
		stepParameters.putProperty(AFormatterStepFactory.KEY_CONTENT, "// someHeaderPrefix $YEAR someHeaderSuffix");

		FormatterStep licenseHeader = stepFactory.makeLicenseHeader(stepParameters);

		var tmpFile = Files.createTempFile("cleanthat", this.getClass().getSimpleName()).toFile();
		tmpFile.deleteOnExit();

		// licenseHeaders requires a real file, not Formatter.NO_FILE_SENTINEL
		String formatted = licenseHeader.format("package somePackage;", tmpFile);

		Assertions.assertThat(formatted)
				.containsSubsequence("// someHeaderPrefix 2023 someHeaderSuffix", "package somePackage;");
	}

	@Test
	public void testLicenseHeaderYearMode_oldFile() throws Exception {
		SpotlessStepParametersProperties stepParameters = new SpotlessStepParametersProperties();
		stepParameters.putProperty(AFormatterStepFactory.KEY_CONTENT, "// someHeaderPrefix $YEAR someHeaderSuffix");

		FormatterStep licenseHeader = stepFactory.makeLicenseHeader(stepParameters);

		var tmpFile = Files.createTempFile("cleanthat", this.getClass().getSimpleName()).toFile();
		tmpFile.deleteOnExit();

		// licenseHeaders requires a real file, not Formatter.NO_FILE_SENTINEL
		String formatted =
				licenseHeader.format("// someHeaderPrefix 1985 someHeaderSuffix\r\npackage somePackage;", tmpFile);

		Assertions.assertThat(formatted)
				.containsSubsequence("// someHeaderPrefix 1985-2023 someHeaderSuffix", "package somePackage;");
	}

	@Test
	public void testLicenseHeaderYearMode_oldFileWithRange() throws Exception {
		SpotlessStepParametersProperties stepParameters = new SpotlessStepParametersProperties();
		stepParameters.putProperty(AFormatterStepFactory.KEY_CONTENT, "// someHeaderPrefix $YEAR someHeaderSuffix");

		FormatterStep licenseHeader = stepFactory.makeLicenseHeader(stepParameters);

		var tmpFile = Files.createTempFile("cleanthat", this.getClass().getSimpleName()).toFile();
		tmpFile.deleteOnExit();

		// licenseHeaders requires a real file, not Formatter.NO_FILE_SENTINEL
		String formatted =
				licenseHeader.format("// someHeaderPrefix 1985-2000 someHeaderSuffix\r\npackage somePackage;", tmpFile);

		Assertions.assertThat(formatted)
				.containsSubsequence("// someHeaderPrefix 1985-2023 someHeaderSuffix", "package somePackage;");
	}
}
