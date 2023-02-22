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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.ATodoJavaParserMutator;

/**
 * Use the diamond operation {@code <>} whenever possible. Some cases are available only since JDK8
 *
 * @author Benoit Lacelle
 */
@Deprecated(since = "Not-ready")
public class UseDiamondOperatorJdk8 extends ATodoJavaParserMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2293");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseDiamondOperator");
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

}
