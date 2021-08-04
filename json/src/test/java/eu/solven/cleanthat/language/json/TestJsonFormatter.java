package eu.solven.cleanthat.language.json;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.language.CleanthatLanguageProperties;
import eu.solven.cleanthat.language.ILanguageProperties;

public class TestJsonFormatter {
	final JsonFormatter formatter = new JsonFormatter(new ObjectMapper());

	@Test
	public void testFormatJson() throws IOException {
		ILanguageProperties languageProperties = new CleanthatLanguageProperties();
		String formatted = formatter.format(languageProperties, "/somePath", "{   }");

		Assert.assertEquals("{}", formatted);
	}
}
