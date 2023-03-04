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
package eu.solven.cleanthat.maven_core;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.Os;

/**
 * Assists the project builder. <strong>Warning:</strong> This is an internal utility class that is only public for
 * technical reasons, it is not part of the public API. In particular, this class can be changed or deleted without
 * prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
// https://github.com/apache/maven/blob/master/maven-core/src/main/java/org/apache/maven/properties/internal/EnvironmentUtils.java
public class EnvironmentUtils {

	private static Properties envVars;

	protected EnvironmentUtils() {
		// hidden
	}

	/**
	 * Adds the environment variables in the form of properties whose keys are prefixed with {@code env.}, e.g. {@code
	 * env.PATH}. Unlike native environment variables, properties are always case-sensitive. For the sake of
	 * determinism, the environment variable names will be normalized to upper case on platforms with case-insensitive
	 * variable lookup.
	 *
	 * @param props
	 *            The properties to add the environment variables to, may be {@code null}.
	 */
	public static void addEnvVars(Properties props) {
		if (props != null) {
			if (envVars == null) {
				synchronized (EnvironmentUtils.class) {
					var tmp = new Properties();
					var caseSensitive = !Os.isFamily(Os.FAMILY_WINDOWS);
					for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
						String key;
						if (caseSensitive) {
							key = "env." + entry.getKey();
						} else {
							key = "env." + entry.getKey().toUpperCase(Locale.ENGLISH);
						}
						tmp.setProperty(key, entry.getValue());
					}
					envVars = tmp;
				}
			}

			props.putAll(envVars);
		}
	}
}