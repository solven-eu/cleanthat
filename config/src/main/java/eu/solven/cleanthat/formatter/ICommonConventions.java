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
package eu.solven.cleanthat.formatter;

/**
 * Some convention may need to be shared in multiple places
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICommonConventions {
	// https://stackoverflow.com/questions/18698738/what-is-the-json-indentation-level-convention
	// '____' seems to be slightly more standard than '\t' or '__'
	String DEFAULT_INDENTATION = "    ";

	/**
	 * The number of ' ' in the default indentation (0 if the default indentation is based on '\t')
	 */
	int DEFAULT_INDENT_WHITESPACES = ICommonConventions.DEFAULT_INDENTATION.length();

	/**
	 * The number of ' ' indentations to mean we request a '\t' indentation
	 */
	int DEFAULT_INDENT_FOR_TAB = -1;
}
