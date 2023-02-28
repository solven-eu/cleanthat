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
package eu.solven.cleanthat.engine.java.eclipse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;

public class TestEclipseJavaFormatterProcessorProperties {
	@Test
	public void testDefault() {
		EclipseJavaFormatterProcessorProperties properties = new EclipseJavaFormatterProcessorProperties();

		Assertions.assertThat(properties.getUrl()).isNotBlank();

		Resource defaultIsReadable = CleanthatUrlLoader.loadUrl(Mockito.mock(ICodeProvider.class), properties.getUrl());

		Assertions.assertThat(defaultIsReadable.isReadable()).isTrue();
	}

	@Test
	public void testDefaultsAreLoadable() {
		Stream.of(EclipseJavaFormatterProcessorProperties.URL_DEFAULT_GOOGLE,
				EclipseJavaFormatterProcessorProperties.URL_DEFAULT_SPRING,
				EclipseJavaFormatterProcessorProperties.URL_DEFAULT_PEPPER).forEach(url -> {
					Resource defaultIsReadable = CleanthatUrlLoader.loadUrl(Mockito.mock(ICodeProvider.class), url);
					Assertions.assertThat(defaultIsReadable.isReadable()).isTrue();

					try {
						var byteArray = ByteStreams.toByteArray(defaultIsReadable.getInputStream());
						var asString = new String(byteArray, StandardCharsets.UTF_8);
						Assertions.assertThat(asString).isNotBlank();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}
}
