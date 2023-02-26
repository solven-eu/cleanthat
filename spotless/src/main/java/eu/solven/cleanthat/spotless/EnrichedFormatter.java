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
package eu.solven.cleanthat.spotless;

import com.diffplug.spotless.Formatter;
import com.google.common.base.MoreObjects;

/**
 * Transports a {@link AFormatterStepFactory} next to a {@link Formatter} instance
 * 
 * @author Benoit Lacelle
 *
 */
public class EnrichedFormatter {
	final AFormatterStepFactory formatterStepFactory;
	final Formatter formatter;

	public EnrichedFormatter(AFormatterStepFactory formatterStepFactory, Formatter formatter) {
		this.formatterStepFactory = formatterStepFactory;
		this.formatter = formatter;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("id", getId()).add("name", getFormatter().getName()).toString();
	}

	public String getId() {
		return formatterStepFactory.getClass().getName();
	}

	public Formatter getFormatter() {
		return formatter;
	}
}
