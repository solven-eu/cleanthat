/*
 * Copyright 2023 Solven
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

import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.base.Strings;

/**
 * Check a {@link EngineProperties} is properly configured
 *
 * @author Benoit Lacelle
 */
public final class CleanthatEnginePropertiesSanitizer
		extends StdConverter<CleanthatEngineProperties, CleanthatEngineProperties> {

	@Override
	public CleanthatEngineProperties convert(CleanthatEngineProperties pojo) {
		if (Strings.isNullOrEmpty(pojo.getEngine())) {
			throw new IllegalArgumentException("the 'engine' is mandatory");
		}

		return pojo;
	}
}
