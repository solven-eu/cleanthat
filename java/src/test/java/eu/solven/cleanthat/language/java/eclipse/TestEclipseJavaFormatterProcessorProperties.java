package eu.solven.cleanthat.language.java.eclipse;

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
import eu.solven.cleanthat.language.CleanthatUrlLoader;

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
						byte[] byteArray = ByteStreams.toByteArray(defaultIsReadable.getInputStream());
						String asString = new String(byteArray, StandardCharsets.UTF_8);
						Assertions.assertThat(asString).isNotBlank();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}
}
