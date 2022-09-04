package eu.solven.cleanthat.language.xml;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.SourceCodeFormatterHelper;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.xml.ekryd_sortpom.EcrydSortPomFormatter;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatter;

public class TestXmlFormattersFactory {
	private ObjectMapper objectMapper = new ObjectMapper();

	final ICodeProvider codeProvider = Mockito.mock(ICodeProvider.class);

	final XmlFormattersFactory factory = new XmlFormattersFactory(objectMapper);

	@Test
	public void makeDefaults() {
		LanguageProperties defaults = factory.makeDefaultProperties();

		defaults.getProcessors();

		List<Map.Entry<ILanguageProperties, ILintFixer>> lintFixers =
				new SourceCodeFormatterHelper(objectMapper).computeLintFixers(defaults, codeProvider, factory);

		Assertions.assertThat(lintFixers.get(0).getValue()).isInstanceOf(RevelcXmlFormatter.class);

		Assertions.assertThat(lintFixers.get(1).getKey().getSourceCode().getIncludes()).hasSize(1).contains("pom.xml");
		Assertions.assertThat(lintFixers.get(1).getValue()).isInstanceOf(EcrydSortPomFormatter.class);
	}
}
