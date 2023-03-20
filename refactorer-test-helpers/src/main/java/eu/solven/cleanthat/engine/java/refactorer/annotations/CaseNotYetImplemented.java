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
package eu.solven.cleanthat.engine.java.refactorer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an annotation not used directly, but extends by various annotations expecting a change, but for which the
 * mutator is not yet implemented.
 * 
 * @author Benoit Lacelle
 *
 */
@Target(ElementType.TYPE)
// Runtime as this will be interpreted as Runtime to resolve `unmodifiedUntilImplemented`
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(since = "This pinpoints unexpected behavior in the mutator", forRemoval = false)
public @interface CaseNotYetImplemented {

	/**
	 * 
	 * @return true if this class will expect no modification, until a future development. It is more explicit than
	 *         having an UnmodifiedMethod with a TODO.
	 */
	boolean unmodifiedUntilImplemented() default true;

}
