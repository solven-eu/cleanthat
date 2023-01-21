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
package eu.solven.cleanthat.spotless;

import static java.util.Collections.emptySet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.google.common.collect.Sets;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import java.util.Set;

/**
 * COmmon behavior to any Spotless engine steps factory
 * 
 * @author Benoit Lacelle
 *
 */
// see com.diffplug.spotless.maven.FormatterFactory
public abstract class AFormatterStepFactory {

	private String[] includes;

	private String[] excludes;

	// private final List<FormatterStepFactory> stepFactories = new ArrayList<>();

	// private ToggleOffOn toggle;

	final ICodeProvider codeProvider;

	public AFormatterStepFactory(ICodeProvider codeProvider, String[] includes, String[] excludes) {
		this.codeProvider = codeProvider;
		this.includes = includes;
		this.excludes = excludes;
	}

	public abstract Set<String> defaultIncludes();

	public abstract String licenseHeaderDelimiter();

	public final Set<String> includes() {
		return includes == null ? emptySet() : Sets.newHashSet(includes);
	}

	public final Set<String> excludes() {
		return excludes == null ? emptySet() : Sets.newHashSet(excludes);
	}

	public abstract FormatterStep makeStep(SpotlessStepProperties s, Provisioner provisioner);

}
