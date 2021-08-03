package eu.solven.cleanthat.rules.config;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.CleanthatLanguageProperties;
import eu.solven.cleanthat.rules.IJdkVersionConstants;
import eu.solven.cleanthat.rules.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.rules.UseIsEmptyOnCollections;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class TestRulesJavaMutator {
	final CleanthatLanguageProperties languageProperties = new CleanthatLanguageProperties();
	final JavaRulesMutatorProperties properties = new JavaRulesMutatorProperties();

	@Test
	public void testFilterOnVersion() {
		languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_5);
		List<IClassTransformer> transformers5 = new RulesJavaMutator(languageProperties, properties).getTransformers();

		languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_11);
		List<IClassTransformer> transformers11 = new RulesJavaMutator(languageProperties, properties).getTransformers();

		// We expect less rules compatible with Java5 than Java11
		Assertions.assertThat(transformers5.size()).isLessThan(transformers11.size());
	}

	@Test
	public void testFilterOnVersion_UseDiamondOperatorJdk8() {
		UseDiamondOperatorJdk8 rule = new UseDiamondOperatorJdk8();
		// UseDiamondOperatorJdk8 is not productionReady
		properties.setProductionReadyOnly(false);

		{
			languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_5);
			List<IClassTransformer> transformers5 =
					new RulesJavaMutator(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers5)
					.flatMap(IClassTransformer::getIds)
					.doesNotContain(rule.getPmdId().get());
		}

		{
			languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_8);
			List<IClassTransformer> transformers5 =
					new RulesJavaMutator(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers5).flatMap(IClassTransformer::getIds).contains(rule.getPmdId().get());
		}

		{
			languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_11);
			List<IClassTransformer> transformers11 =
					new RulesJavaMutator(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers11).flatMap(IClassTransformer::getIds).contains(rule.getPmdId().get());
		}
	}

	@Test
	public void testFilterOnExcluded() {
		languageProperties.setLanguageVersion(IJdkVersionConstants.JDK_1);

		UseIsEmptyOnCollections oneRule = new UseIsEmptyOnCollections();
		String oneRuleId = oneRule.getIds().stream().findFirst().get();

		{
			List<IClassTransformer> allTransformers =
					new RulesJavaMutator(languageProperties, properties).getTransformers();
			Assertions.assertThat(allTransformers).map(IClassTransformer::getId).contains(oneRuleId);
		}

		{
			properties.setExcluded(Arrays.asList(oneRuleId));

			List<IClassTransformer> fileredTransformers =
					new RulesJavaMutator(languageProperties, properties).getTransformers();
			Assertions.assertThat(fileredTransformers).map(IClassTransformer::getId).doesNotContain(oneRuleId);
		}
	}
}
