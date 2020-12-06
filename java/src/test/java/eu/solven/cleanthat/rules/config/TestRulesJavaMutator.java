package eu.solven.cleanthat.rules.config;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.rules.IJdkVersionConstants;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class TestRulesJavaMutator {
	@Test
	public void testFilterOnVersion() {
		CleanthatLanguageProperties languageProperties = new CleanthatLanguageProperties();
		CleanthatJavaProcessorProperties properties = new CleanthatJavaProcessorProperties();

		languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_5);
		List<IClassTransformer> transformers5 = new RulesJavaMutator(languageProperties, properties).getTransformers();

		languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_11);
		List<IClassTransformer> transformers11 = new RulesJavaMutator(languageProperties, properties).getTransformers();

		// We expect less rules compatible with Java5 than Java11
		Assertions.assertThat(transformers5.size()).isLessThan(transformers11.size());
	}
}
