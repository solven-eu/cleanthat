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
package eu.solven.cleanthat.engine.java.refactorer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.Test;

import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIsEmptyOnCollections;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.PMDMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.SafeAndConsensualMutators;
import eu.solven.cleanthat.engine.java.refactorer.test.ATestCases;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;

public class TestJavaRefactorer {
	final CleanthatEngineProperties engineProperties =
			CleanthatEngineProperties.builder().engine("java").engineVersion(IJdkVersionConstants.JDK_8).build();
	final JavaRefactorerProperties prdMutatorsProperties = JavaRefactorerProperties.allProductionReady();
	final JavaRefactorerProperties draftMutatorsProperties = JavaRefactorerProperties.allEvenNotProductionReady();

	@Test
	public void testFilterOnVersion() {
		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
		List<IMutator> transformers5 = JavaRefactorer.filterRules(engineProperties, prdMutatorsProperties);

		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
		List<IMutator> transformers11 = JavaRefactorer.filterRules(engineProperties, prdMutatorsProperties);

		// We expect less rules compatible with Java5 than Java11
		Assertions.assertThat(transformers5.size()).isLessThan(transformers11.size());
	}

	@Test
	public void testFilterOnVersion_UseDiamondOperatorJdk8() {
		var rule = new UseDiamondOperatorJdk8();
		// UseDiamondOperatorJdk8 is not productionReady
		var mutatorsProperties = draftMutatorsProperties;

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
			List<IMutator> transformers5 = JavaRefactorer.filterRules(engineProperties, mutatorsProperties);

			Assertions.assertThat(transformers5).flatMap(IMutator::getIds).doesNotContain(rule.getPmdId().get());
		}

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_8);
			List<IMutator> transformers8 = JavaRefactorer.filterRules(engineProperties, mutatorsProperties);

			Assertions.assertThat(transformers8).flatMap(IMutator::getIds).contains(rule.getPmdId().get());
		}

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
			List<IMutator> transformers11 = JavaRefactorer.filterRules(engineProperties, mutatorsProperties);

			Assertions.assertThat(transformers11).flatMap(IMutator::getIds).contains(rule.getPmdId().get());
		}
	}

	@Test
	public void testFilterOnExcluded() {
		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);

		// UseIsEmptyOnCollections is not productionReady
		var mutatorsProperties = draftMutatorsProperties;

		var oneRule = new UseIsEmptyOnCollections();
		Set<String> oneRuleIds = oneRule.getIds();

		Assertions.assertThat(oneRuleIds.size()).isGreaterThan(1);

		{
			List<IMutator> allTransformers = JavaRefactorer.filterRules(engineProperties, mutatorsProperties);
			Assertions.assertThat(allTransformers).flatMap(IMutator::getIds).containsAll(oneRuleIds);
		}

		for (String oneRuleId : oneRuleIds) {
			mutatorsProperties.setExcluded(Arrays.asList(oneRuleId));

			List<IMutator> fileredTransformers = JavaRefactorer.filterRules(engineProperties, mutatorsProperties);
			Assertions.assertThat(fileredTransformers).flatMap(IMutator::getIds).doesNotContain(oneRuleId);
		}
	}

	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		Class<JavaparserDirtyMe> classToLoad = JavaparserDirtyMe.class;
		String dirtyCode = LocalClassTestHelper.loadClassAsString(classToLoad);

		var rulesJavaMutator = new JavaRefactorer(engineProperties, prdMutatorsProperties);

		var javaParser = JavaRefactorer.makeDefaultJavaParser(JavaRefactorer.JAVAPARSER_JRE_ONLY);
		var compilationUnit = ATestCases.throwIfProblems(javaParser.parse(dirtyCode));
		LexicalPreservingPrinter.setup(compilationUnit);
		var rawJavaparserCode = rulesJavaMutator.toString(compilationUnit);

		// Check this is a piece of code which is dirtied by JavaParser
		// 2022-01: We rely on LexicalPreservingPrinter for prettyPrinting
		Assertions.assertThat(rawJavaparserCode).isEqualTo(dirtyCode);

		var cleanJavaparserCode = rulesJavaMutator.fixJavaparserUnexpectedChanges(dirtyCode, rawJavaparserCode);
		Assertions.assertThat(cleanJavaparserCode).isEqualTo(dirtyCode);
	}

	@Test
	public void testGetIds() {
		Assertions.assertThat(JavaRefactorer.getAllIncluded()).hasSizeGreaterThan(5);
	}

	@Test
	public void testIncludeRuleByClassName() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList(LocalVariableTypeInference.class.getName()),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules).hasSize(1);
	}

	@Test
	public void testIncludeRuleByClassName_composite_excludeDraftExplicitly() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse(IJdkVersionConstants.LAST),
				Collections.singletonList(PMDMutators.class.getName()),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules)
				.hasSizeGreaterThan(3)
				.map(c -> c.getClass().getName())
				.contains(UseIndexOfChar.class.getName());

		List<IMutator> rulesExcluding = JavaRefactorer.filterRules(JavaVersion.parse(IJdkVersionConstants.LAST),
				Collections.singletonList(PMDMutators.class.getName()),
				Collections.singletonList(UseIndexOfChar.class.getName()),
				true);

		// Check the exclusion succeeded
		Assertions.assertThat(rulesExcluding)
				.hasSize(rules.size() - 1)
				.map(c -> c.getClass().getName())
				.doesNotContain(UseIndexOfChar.class.getName());
	}

	@Test
	public void testIncludeRuleByClassName_custom() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList(CustomMutator.class.getName()),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules).hasSize(1);
	}

	@Test
	public void testIncludeRuleByClassName_draftRule_draftNotIncluded() {
		final var customProperties = new JavaRefactorerProperties();
		customProperties.setIncludeDraft(false);
		customProperties.setIncluded(Arrays.asList(CustomCompositeMutator.class.getName()));

		List<IMutator> rules = JavaRefactorer.filterRules(engineProperties, customProperties);

		Assertions.assertThat(rules)
				.hasSize(1)
				.map(c -> c.getClass().getName())
				.contains(CustomMutator.class.getName());
	}

	@Test
	public void testIncludeRuleByClassName_draftRule_draftIncluded() {
		final var customProperties = new JavaRefactorerProperties();
		customProperties.setIncludeDraft(true);
		customProperties.setIncluded(Arrays.asList(CustomCompositeMutator.class.getName()));

		List<IMutator> rules = JavaRefactorer.filterRules(engineProperties, customProperties);

		Assertions.assertThat(rules)
				.hasSize(2)
				.map(c -> c.getClass().getName())
				.contains(CustomMutator.class.getName())
				.contains(CustomDraftMutator.class.getName());
	}

	@Test
	public void testIncludeRuleByClassName_draftRule_draftNotIncluded_butExplicitlyListed() {
		final var customProperties = new JavaRefactorerProperties();
		customProperties.setIncludeDraft(false);
		customProperties.setIncluded(Arrays.asList(CustomDraftMutator.class.getName()));

		List<IMutator> rules = JavaRefactorer.filterRules(engineProperties, customProperties);

		Assertions.assertThat(rules)
				.hasSize(1)
				.map(c -> c.getClass().getName())
				.contains(CustomDraftMutator.class.getName());
	}

	@Test
	public void testIncludeRuleByClassName_draftRule_draftNotIncluded_butExplicitlyListedNextToComposite() {
		final var customProperties = new JavaRefactorerProperties();
		customProperties.setIncludeDraft(false);
		customProperties
				.setIncluded(Arrays.asList(CustomCompositeMutator.class.getName(), CustomDraftMutator.class.getName()));

		List<IMutator> rules = JavaRefactorer.filterRules(engineProperties, customProperties);

		Assertions.assertThat(rules)
				.hasSize(2)
				.map(c -> c.getClass().getName())
				.contains(CustomMutator.class.getName())
				.contains(CustomDraftMutator.class.getName());
	}

	@Test
	public void testIncludeRule_compositeMutator() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList(JavaRefactorerProperties.SAFE_AND_CONSENSUAL),
				Collections.emptyList(),
				true);

		// The list will grow through time
		Assertions.assertThat(rules).hasSizeGreaterThan(6);
	}

	@Test
	public void testIncludeRule_ruleInSafeAndConsensual() {
		var idInSafeAndConsensual = SafeAndConsensualMutators.SAFE_AND_CONSENSUAL.get(0).getIds().iterator().next();
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList(idInSafeAndConsensual),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules).hasSize(1);
	}

	@Test
	public void testIncludeRule_unknownRule() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList("UnknownId"),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules).isEmpty();
	}

	@Test
	public void testIncludeRule_idInMultipleSingle() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList("RSPEC-2293"),
				Collections.emptyList(),
				true);

		Assertions.assertThat(rules)
				.hasSize(2)
				.map(c -> c.getClass().getName())
				.contains(UseDiamondOperator.class.getName())
				.contains(UseDiamondOperatorJdk8.class.getName());
	}

}
