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
package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;

import java.util.List;

import org.codehaus.plexus.languages.java.version.JavaVersion;

import com.google.common.collect.ImmutableList;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorerProperties;
import eu.solven.cleanthat.engine.java.refactorer.meta.IConstructorNeedsJdkVersion;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CastMathOperandsBeforeAssignement;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionToOptional;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachAddToStreamCollectToCollection;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfToIfStreamAnyMatch;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachToForIterableForEach;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LoopIntRangeToIntStreamForEach;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanExpression;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanInitialization;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringReplaceAllWithQuotableInput;

/**
 * This mutator will apply all {@link IMutator} considered not-trivial. It is relevant to demonstrate the most
 * complex/useful rules, without polluting the diff with trivial changes.
 * 
 * @author Benoit Lacelle
 *
 */
public class SafeButControversialMutators extends CompositeMutator<IMutator> implements IConstructorNeedsJdkVersion {
	public static final List<IMutator> BUT_CONTROVERSIAL = ImmutableList.<IMutator>builder()
			.add(new CollectionToOptional(),
					new ForEachIfToIfStreamAnyMatch(),
					new ForEachToForIterableForEach(),
					new ForEachAddToStreamCollectToCollection(),
					new SimplifyBooleanExpression(),
					new SimplifyBooleanInitialization(),
					new StringReplaceAllWithQuotableInput(),
					new LoopIntRangeToIntStreamForEach(),
					new OptionalWrappedIfToFilter(),
					new OptionalWrappedVariableToMap(),
					new StreamWrappedIfToFilter(),
					new StreamWrappedVariableToMap(),
					// new StreamWrappedMethodRefToMap(),
					new CastMathOperandsBeforeAssignement()

			// NullCheckToOptionalOfNullable is often counter-productive. We need to restrict its coverage to more
			// relevant situations than any nullCheck
			// new NullCheckToOptionalOfNullable()
			)
			.build();

	public SafeButControversialMutators(JavaVersion sourceJdkVersion) {
		super(filterWithJdk(sourceJdkVersion, BUT_CONTROVERSIAL));
	}

	@Override
	public String getCleanthatId() {
		return JavaRefactorerProperties.SAFE_BUT_CONTROVERSIAL;
	}
}
