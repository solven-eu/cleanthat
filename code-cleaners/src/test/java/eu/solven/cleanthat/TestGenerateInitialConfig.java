package eu.solven.cleanthat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.GenerateInitialConfig;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;

public class TestGenerateInitialConfig {
	@Test
	public void testGenerateDefaultConfig_empty() throws IOException {
		ILanguageLintFixerFactory factory = Mockito.mock(ILanguageLintFixerFactory.class);
		GenerateInitialConfig generator = new GenerateInitialConfig(Arrays.asList(factory));

		ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
		CleanthatRepositoryProperties config = generator.prepareDefaultConfiguration(codeProvider);

		Assertions.assertThat(config.getSourceCode().getIncludes()).isEmpty();
		Assertions.assertThat(config.getSourceCode().getExcludes()).isEmpty();
	}

	@Test
	public void testGenerateDefaultConfig_mvnWrapper() throws IOException {
		ILanguageLintFixerFactory factory = Mockito.mock(ILanguageLintFixerFactory.class);
		GenerateInitialConfig generator = new GenerateInitialConfig(Arrays.asList(factory));

		ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);
		Mockito.when(codeProvider.loadContentForPath("/.mvn/wrapper/maven-wrapper.properties"))
				.thenReturn(Optional.of("somePropertiesFileContent"));

		CleanthatRepositoryProperties config = generator.prepareDefaultConfiguration(codeProvider);

		Assertions.assertThat(config.getSourceCode().getIncludes()).isEmpty();
		Assertions.assertThat(config.getSourceCode().getExcludes()).hasSize(1).contains("glob:/.mvn/wrapper/**");
	}
}
