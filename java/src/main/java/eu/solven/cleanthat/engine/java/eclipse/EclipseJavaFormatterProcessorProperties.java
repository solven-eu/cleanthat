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
package eu.solven.cleanthat.engine.java.eclipse;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import lombok.Data;

/**
 * The configuration of what is not related to a language.
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
public class EclipseJavaFormatterProcessorProperties {
	public static final String PREFIX_URL_DEFAULT_GOOGLE = CleanthatUrlLoader.PREFIX_CLASSPATH_ECLIPSE;

	public static final String URL_DEFAULT_GOOGLE = PREFIX_URL_DEFAULT_GOOGLE + "eclipse-java-google-style.xml";
	public static final String URL_DEFAULT_SPRING = PREFIX_URL_DEFAULT_GOOGLE + "spring-eclipse-code-formatter.xml";
	public static final String URL_DEFAULT_PEPPER = PREFIX_URL_DEFAULT_GOOGLE + "pepper-eclipse-code-formatter.xml";

	// see eu.solven.cleanthat.language.CleanthatUrlLoader.loadUrl(ICodeProvider, String)
	private String url = URL_DEFAULT_GOOGLE;

}
