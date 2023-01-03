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
package eu.solven.cleanthat.java.mutators;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;

/**
 * Enables CleanThat as a SpotLess step
 * 
 * @author Benoit Lacelle
 */
// https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep
public class CleanthatSpotlessStep {
	// prevent direct instantiation
	private CleanthatSpotlessStep() {
	}

	private static final String NAME = "cleanthat";
	private static final String MAVEN_COORDINATE = "io.github.solven-eu.cleanthat:java:";

	static final String FORMATTER_CLASS = "eu.solven.cleanthat.java.Formatter";
	static final String FORMATTER_METHOD = "formatSource";

	private static final String OPTIONS_CLASS = "eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties";
	private static final String OPTIONS_BUILDER_METHOD = "builder";
	private static final String OPTIONS_BUILDER_CLASS = "com.google.googlejavaformat.java.JavaFormatterOptions$Builder";
	private static final String OPTIONS_BUILDER_BUILD_METHOD = "build";

	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String>support(NAME).add(8, "1.9");

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(MAVEN_COORDINATE, version, provisioner);
	}

	/** Creates a step which formats everything - groupArtifact, code, import order, and unused imports. */
	public static FormatterStep create(String groupArtifact, String version, Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'");
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep
				.createLazy(NAME, () -> new State(NAME, groupArtifact, version, provisioner), State::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the formatter. */
		final JarState jarState;
		final String stepName;
		final String version;

		State(String stepName, String version, Provisioner provisioner) throws Exception {
			this(stepName, MAVEN_COORDINATE, version, provisioner);
		}

		State(String stepName, String groupArtifact, String version, Provisioner provisioner) throws Exception {
			JVM_SUPPORT.assertFormatterSupported(version);
			// ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
			this.stepName = stepName;
			this.version = version;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> optionsClass = classLoader.loadClass(OPTIONS_CLASS);
			Class<?> optionsBuilderClass = classLoader.loadClass(OPTIONS_BUILDER_CLASS);
			Method optionsBuilderMethod = optionsClass.getMethod(OPTIONS_BUILDER_METHOD);
			Object optionsBuilder = optionsBuilderMethod.invoke(null);

			Method optionsBuilderBuildMethod = optionsBuilderClass.getMethod(OPTIONS_BUILDER_BUILD_METHOD);
			Object options = optionsBuilderBuildMethod.invoke(optionsBuilder);

			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor(optionsClass).newInstance(options);
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

			return JVM_SUPPORT.suggestLaterVersionOnError(version, (input -> {
				return (String) formatterMethod.invoke(formatter, input);
			}));
		}

	}
}
