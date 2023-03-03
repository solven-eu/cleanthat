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

import org.springframework.core.io.Resource;

import com.github.javaparser.ast.CompilationUnit;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * Used to compare 2 {@link CompilationUnit}, provided as {@link Resource}
 * 
 * @author Benoit Lacelle
 *
 */
@Target(ElementType.TYPE)
// Runtime as this will be interpreted as Runtime to resolve `pre` and `post` (as concatenated Strings)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnmodifiedCompilationUnitAsResource {
	/**
	 * 
	 * @return the path of the resource over which a {@link IMutator} has to be applied
	 */
	String pre();

	/**
	 * 
	 * @return the path of the resource expected code after the {@link IMutator}
	 */
	String post() default "";
}
