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
package eu.solven.cleanthat.engine.java;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.engine.java.eclipse.EclipseJavaFormatterProcessorProperties;
import eu.solven.cleanthat.language.IEngineProperties;
import lombok.EqualsAndHashCode;

/**
 * Used as cache Key for Eclipse configuration
 * 
 * @author Benoit Lacelle
 *
 */
@EqualsAndHashCode
public class EclipseFormatterCacheKey {
	// The same URL may map to different configuration (e.g. if the URL is relative to the repository)
	final ICodeProvider codeProvider;
	final IEngineProperties languageProperties;
	final EclipseJavaFormatterProcessorProperties eclipseJavaFormatterProcessorProperties;

	public EclipseFormatterCacheKey(ICodeProvider codeProvider,
			IEngineProperties languageProperties,
			EclipseJavaFormatterProcessorProperties eclipseJavaFormatterProcessorProperties) {
		this.codeProvider = codeProvider;
		this.languageProperties = languageProperties;
		this.eclipseJavaFormatterProcessorProperties = eclipseJavaFormatterProcessorProperties;
	}

}
