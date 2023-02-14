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

import org.assertj.core.api.Assertions;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIndexOfChar;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIsEmptyOnCollections;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllEvenNotProductionReadyMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.PMDMutators;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;

public class TestJavaRefactorer {
	final CleanthatEngineProperties engineProperties =
			CleanthatEngineProperties.builder().engine("java").engineVersion(IJdkVersionConstants.JDK_8).build();
	final JavaRefactorerProperties mutatorsProperties = JavaRefactorerProperties.allProductionReady();

	@Test
	public void testFilterOnVersion() {
		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
		List<IMutator> transformers5 = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();

		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
		List<IMutator> transformers11 = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();

		// We expect less rules compatible with Java5 than Java11
		Assertions.assertThat(transformers5.size()).isLessThan(transformers11.size());
	}

	@Test
	public void testFilterOnVersion_UseDiamondOperatorJdk8() {
		UseDiamondOperatorJdk8 rule = new UseDiamondOperatorJdk8();
		// UseDiamondOperatorJdk8 is not productionReady
		mutatorsProperties.setIncluded(Arrays.asList(AllEvenNotProductionReadyMutators.class.getName()));
		mutatorsProperties.setProductionReadyOnly(false);

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
			List<IMutator> transformers5 = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();

			Assertions.assertThat(transformers5).flatMap(IMutator::getIds).doesNotContain(rule.getPmdId().get());
		}

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_8);
			List<IMutator> transformers8 = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();

			Assertions.assertThat(transformers8).flatMap(IMutator::getIds).contains(rule.getPmdId().get());
		}

		{
			engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
			List<IMutator> transformers11 = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();

			Assertions.assertThat(transformers11).flatMap(IMutator::getIds).contains(rule.getPmdId().get());
		}
	}

	@Test
	public void testFilterOnExcluded() {
		engineProperties.setEngineVersion(IJdkVersionConstants.JDK_11);

		UseIsEmptyOnCollections oneRule = new UseIsEmptyOnCollections();
		String oneRuleId = oneRule.getIds().stream().findFirst().get();

		{
			List<IMutator> allTransformers = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();
			Assertions.assertThat(allTransformers).flatMap(IMutator::getIds).contains(oneRuleId);
		}

		{
			mutatorsProperties.setExcluded(Arrays.asList(oneRuleId));

			List<IMutator> fileredTransformers = new JavaRefactorer(engineProperties, mutatorsProperties).getMutators();
			Assertions.assertThat(fileredTransformers).flatMap(IMutator::getIds).doesNotContain(oneRuleId);
		}
	}

	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		Class<JavaparserDirtyMe> classToLoad = JavaparserDirtyMe.class;
		String dirtyCode = LocalClassTestHelper.loadClassAsString(classToLoad);

		JavaRefactorer rulesJavaMutator = new JavaRefactorer(engineProperties, mutatorsProperties);

		JavaParser javaParser = JavaRefactorer.makeDefaultJavaParser(true);
		CompilationUnit compilationUnit = javaParser.parse(dirtyCode).getResult().get();
		LexicalPreservingPrinter.setup(compilationUnit);
		String rawJavaparserCode = rulesJavaMutator.toString(compilationUnit);

		// Check this is a piece of code which is dirtied by Javaparser
		// 2022-01: We rely on LexicalPreservingPrinter for prettyPrinting
		Assertions.assertThat(rawJavaparserCode).isEqualTo(dirtyCode);

		String cleanJavaparserCode = rulesJavaMutator.fixJavaparserUnexpectedChanges(dirtyCode, rawJavaparserCode);
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
				false);

		Assertions.assertThat(rules).hasSize(1);
	}

	@Test
	public void testIncludeRuleByClassName_composite() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse(IJdkVersionConstants.LAST),
				Collections.singletonList(PMDMutators.class.getName()),
				Collections.emptyList(),
				false);

		Assertions.assertThat(rules).hasSizeGreaterThan(3);

		List<IMutator> rulesExcluding = JavaRefactorer.filterRules(JavaVersion.parse(IJdkVersionConstants.LAST),
				Collections.singletonList(PMDMutators.class.getName()),
				Collections.singletonList(UseIndexOfChar.class.getName()),
				false);

		// Check the exclusion succeeded
		Assertions.assertThat(rulesExcluding).hasSize(rules.size() - 1);
	}

	@Test
	public void testIncludeRuleByClassName_custom() {
		List<IMutator> rules = JavaRefactorer.filterRules(JavaVersion.parse("11"),
				Collections.singletonList(CustomMutator.class.getName()),
				Collections.emptyList(),
				false);

		Assertions.assertThat(rules).hasSize(1);
	}

}
