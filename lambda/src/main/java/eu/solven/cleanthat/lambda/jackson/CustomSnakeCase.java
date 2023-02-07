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
package eu.solven.cleanthat.lambda.jackson;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.Locale;

/**
 * Helps interpreting fields in capital case
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/30205006/why-does-jackson-2-not-recognize-the-first-capital-letter-if-the-leading-camel-c
public class CustomSnakeCase extends PropertyNamingStrategies.NamingBase {
	private static final long serialVersionUID = -537037017048878391L;

	@Override
	public String translate(String input) {
		if (input == null) {
			return input; // garbage in, garbage out
		}

		return input.toUpperCase(Locale.US);
	}
}