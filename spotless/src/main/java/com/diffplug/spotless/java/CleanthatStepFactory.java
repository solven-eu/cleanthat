/*
 * Copyright 2016-2022 DiffPlug
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
package com.diffplug.spotless.java;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;

/**
 * Enables CleanThat as a SpotLess step. This may be moved to Spotless own repo (https://github.com/diffplug/spotless/tree/main/lib)
 * 
 * @author Benoit Lacelle
 */
// https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep
public class CleanthatStepFactory {
	// prevent direct instantiation
	private CleanthatStepFactory() {
	}

	private static final String NAME = "cleanthat";
	private static final String MAVEN_COORDINATE = "io.github.solven-eu.cleanthat:java:";

	static final String FORMATTER_CLASS = "eu.solven.cleanthat.java.mutators.RulesJavaMutator";
	static final String FORMATTER_METHOD = "doFormat";

	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String>support(NAME).add(8, "1.9");

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(MAVEN_COORDINATE, version, defaultExcluded(), defaultIncluded(), provisioner);
	}

	private static List<String> defaultExcluded() {
		return List.of();
	}

	private static List<String> defaultIncluded() {
		return List.of("*");
	}

	/** Creates a step which formats everything - groupArtifact, code, import order, and unused imports. */
	public static FormatterStep create(String groupArtifact,
			String version,
			List<String> excluded,
			List<String> included,
			Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'");
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new JavaRulesState(NAME, groupArtifact, version, excluded, included, provisioner),
				JavaRulesState::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	static final class JavaRulesState implements Serializable {
		private static final long serialVersionUID = 1L;

		final JarState jarState;
		final String stepName;
		final String version;

		final List<String> excluded;
		final List<String> included;

		JavaRulesState(String stepName, String version, Provisioner provisioner) throws Exception {
			this(stepName, MAVEN_COORDINATE, version, defaultExcluded(), defaultIncluded(), provisioner);
		}

		JavaRulesState(String stepName,
				String groupArtifact,
				String version,
				List<String> excluded,
				List<String> included,
				Provisioner provisioner) throws Exception {
			JVM_SUPPORT.assertFormatterSupported(version);
			// ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
			this.stepName = stepName;
			this.version = version;

			this.excluded = excluded;
			this.included = included;
		}

		@SuppressWarnings({ "unchecked" })
		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			Class<?> lineEndingClass = classLoader.loadClass("eu.solven.cleanthat.formatter.LineEnding");
			// Spotless will invoke String to be formatted, always filled with '\n'
			// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
			Object linuxLineEnding = Enum.valueOf(lineEndingClass.asSubclass(Enum.class), "LF");

			Class<?> iLanguagePropertiesClass =
					classLoader.loadClass("eu.solven.cleanthat.language.ILanguageProperties");

			Class<?> languagePropertiesClass = classLoader.loadClass("eu.solven.cleanthat.language.LanguageProperties");
			Object languageProperties = languagePropertiesClass.getConstructor().newInstance();

			{
				languageProperties.getClass().getMethod("setLanguage", String.class).invoke(languageProperties, "java");
				// IJdkVersionConstants
				languageProperties.getClass()
						.getMethod("setLanguageVersion", String.class)
						.invoke(languageProperties, "1.8");
			}

			Class<?> rulesPropertiesClass =
					classLoader.loadClass("eu.solven.cleanthat.java.mutators.JavaRefactorerProperties");
			Object rulesProperties = rulesPropertiesClass.getConstructor().newInstance();
			{
				// eu.solven.cleanthat.language.java.JavaFormattersFactory.getLanguage()
				rulesProperties.getClass().getMethod("setLanguage", String.class).invoke(languageProperties, "java");
			}

			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Constructor<?> formatterConstructor =
					formatterClazz.getConstructor(iLanguagePropertiesClass, rulesPropertiesClass);

			Object formatter = formatterConstructor.newInstance(languageProperties, rulesProperties);
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class, lineEndingClass);

			return JVM_SUPPORT.suggestLaterVersionOnError(version, (input -> {
				return (String) formatterMethod.invoke(formatter, input, linuxLineEnding);
			}));
		}

	}
}
