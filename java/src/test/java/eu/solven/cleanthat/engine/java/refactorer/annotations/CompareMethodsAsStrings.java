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
import java.lang.annotation.Target;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * Used to compare 2 {@link MethodDeclaration}, provided as Strings. Beware, the lack of `import XXX` will break most
 * type resolutions.
 * 
 * @author Benoit Lacelle
 *
 */
@Target(ElementType.TYPE)
public @interface CompareMethodsAsStrings {
	String pre();

	String post();
}
