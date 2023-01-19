/*
 * Copyright 2023 Solven
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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.google.common.collect.ImmutableMap;
import eu.solven.cleanthat.config.pojo.EngineProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatterConfiguration;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseIsEmptyOnCollections;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;
import eu.solven.cleanthat.formatter.IStyleEnforcer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestRulesJavaMutator {
	final EngineProperties languageProperties = new EngineProperties();
	final JavaRefactorerProperties properties = new JavaRefactorerProperties();

	@Test
	public void testFilterOnVersion() {
		languageProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
		List<IClassTransformer> transformers5 = new JavaRefactorer(languageProperties, properties).getTransformers();

		languageProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
		List<IClassTransformer> transformers11 = new JavaRefactorer(languageProperties, properties).getTransformers();

		// We expect less rules compatible with Java5 than Java11
		Assertions.assertThat(transformers5.size()).isLessThan(transformers11.size());
	}

	@Test
	public void testFilterOnVersion_UseDiamondOperatorJdk8() {
		UseDiamondOperatorJdk8 rule = new UseDiamondOperatorJdk8();
		// UseDiamondOperatorJdk8 is not productionReady
		properties.setProductionReadyOnly(false);

		{
			languageProperties.setEngineVersion(IJdkVersionConstants.JDK_5);
			List<IClassTransformer> transformers5 =
					new JavaRefactorer(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers5)
					.flatMap(IClassTransformer::getIds)
					.doesNotContain(rule.getPmdId().get());
		}

		{
			languageProperties.setEngineVersion(IJdkVersionConstants.JDK_8);
			List<IClassTransformer> transformers5 =
					new JavaRefactorer(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers5).flatMap(IClassTransformer::getIds).contains(rule.getPmdId().get());
		}

		{
			languageProperties.setEngineVersion(IJdkVersionConstants.JDK_11);
			List<IClassTransformer> transformers11 =
					new JavaRefactorer(languageProperties, properties).getTransformers();

			Assertions.assertThat(transformers11).flatMap(IClassTransformer::getIds).contains(rule.getPmdId().get());
		}
	}

	@Test
	public void testFilterOnExcluded() {
		languageProperties.setEngineVersion(IJdkVersionConstants.JDK_11);

		UseIsEmptyOnCollections oneRule = new UseIsEmptyOnCollections();
		String oneRuleId = oneRule.getIds().stream().findFirst().get();

		{
			List<IClassTransformer> allTransformers =
					new JavaRefactorer(languageProperties, properties).getTransformers();
			Assertions.assertThat(allTransformers).flatMap(IClassTransformer::getIds).contains(oneRuleId);
		}

		{
			properties.setExcluded(Arrays.asList(oneRuleId));

			List<IClassTransformer> fileredTransformers =
					new JavaRefactorer(languageProperties, properties).getTransformers();
			Assertions.assertThat(fileredTransformers).flatMap(IClassTransformer::getIds).doesNotContain(oneRuleId);
		}
	}

	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		Class<JavaparserDirtyMe> classToLoad = JavaparserDirtyMe.class;
		String dirtyCode = LocalClassTestHelper.loadClassAsString(classToLoad);

		JavaRefactorer rulesJavaMutator = new JavaRefactorer(languageProperties, properties);

		// We need this piece of configuration, else cleaning the post-Javaparser code leads to very different result to
		// the diry/original code, hence leading the Diff procedure to give too-different results
		Map<String, String> options = ImmutableMap.<String, String>builder()
				.put("org.eclipse.jdt.core.formatter.lineSplit", "120")
				.put("org.eclipse.jdt.core.formatter.comment.line_length", "120")
				.build();

		// We need any styleEnforce to workaround Javaparser weaknesses
		IStyleEnforcer styleEnforcer = new EclipseJavaFormatter(new EclipseJavaFormatterConfiguration(options));
		rulesJavaMutator.registerCodeStyleFixer(styleEnforcer);

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

}
