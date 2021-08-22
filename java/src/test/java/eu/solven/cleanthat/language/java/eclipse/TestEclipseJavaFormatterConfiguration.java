package eu.solven.cleanthat.language.java.eclipse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.java.IJdkVersionConstants;

public class TestEclipseJavaFormatterConfiguration {
	final ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
	final LanguageProperties languageProperties = new LanguageProperties();
	final EclipseJavaFormatterProcessorProperties processorConfig = new EclipseJavaFormatterProcessorProperties();

	@Test
	public void testLoadConfig_empty() {
		EclipseJavaFormatterConfiguration config =
				EclipseJavaFormatterConfiguration.load(codeProvider, languageProperties, processorConfig);

		Assertions.assertThat(config.getSettings())
				.hasSize(3)
				.containsEntry(JavaCore.COMPILER_SOURCE, "0")
				.containsEntry(JavaCore.COMPILER_COMPLIANCE, "0")
				.containsEntry(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "0");
	}

	@Test
	public void testLoadConfig_fromFile() throws IOException {
		String path = "/config/eclipse_java_code_formatter.xml";

		processorConfig.setUrl("code:" + path);

		String content =
				new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		Mockito.when(codeProvider.loadContentForPath(path)).thenReturn(Optional.of(content));

		EclipseJavaFormatterConfiguration config =
				EclipseJavaFormatterConfiguration.load(codeProvider, languageProperties, processorConfig);

		Assertions.assertThat(config.getSettings())
				.hasSize(308)
				// .containsEntry(JavaCore.COMPILER_SOURCE, IJdkVersionConstants.JDK_8)
				// .containsEntry(JavaCore.COMPILER_COMPLIANCE, IJdkVersionConstants.JDK_8)
				.containsEntry(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, IJdkVersionConstants.JDK_8);
	}
}
