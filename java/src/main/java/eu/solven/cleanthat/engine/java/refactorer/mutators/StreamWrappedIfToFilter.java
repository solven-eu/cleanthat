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

import java.util.Set;
import java.util.stream.BaseStream;

import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `strings.forEach(s -> { if (s.length() >= 1) { System.out.println(s); }
 * 
 * into `strings.filter(s -> s.length() >= 1).forEach(s -> { System.out.println(s); });`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class StreamWrappedIfToFilter extends OptionalWrappedIfToFilter {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Loop", "Stream");
	}

	@Override
	protected Set<String> getEligibleForUnwrappedFilter() {
		return Set.of("forEach");
	}

	@Override
	protected Class<?> getExpectedScope() {
		return BaseStream.class;
	}

}
