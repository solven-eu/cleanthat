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
package eu.solven.cleanthat.config.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Marking interface indicating the implementation can be used to configure a {@link CleanthatStepProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICleanthatStepParametersProperties {
	/**
	 * This is the identifier of the default mutators to be safely applied to most project
	 */
	// BEWARE This is specific to JavaRefactorerProperties
	String SAFE_AND_CONSENSUAL = "SafeAndConsensual";

	/**
	 * These mutators are not consensual, but their changes are relatively small.
	 */
	// BEWARE This is specific to JavaRefactorerProperties
	String SAFE_BUT_NOT_CONSENSUAL = "SafeButNotConsensual";

	/**
	 * These mutators are safe but not consensual at all. The larger changes may be controversial.
	 */
	// BEWARE This is specific to JavaRefactorerProperties
	String SAFE_BUT_CONTROVERSIAL = "SafeButControversial";

	/**
	 * The mutators applies Guava-friendly mutators. Most of them would rely on Guava methods. Some of them would get
	 * out of Guava (e.g. for methods having equivalent in JDK)
	 */
	// BEWARE This is specific to JavaRefactorerProperties
	String GUAVA = "Guava";

	/**
	 * The mutators applies any Stream related IMutator
	 */
	// BEWARE This is specific to JavaRefactorerProperties
	String STREAM = "Stream";

	@JsonIgnore
	Object getCustomProperty(String key);
}
