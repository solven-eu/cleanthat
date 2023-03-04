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

import java.util.Properties;

import com.google.common.base.Strings;

/**
 * @since 3.2.3
 */
// https://github.com/apache/maven/blob/master/maven-core/src/main/java/org/apache/maven/properties/internal/SystemProperties.java
public class SystemProperties {
	/**
	 * Thread-safe System.properties copy implementation.
	 */
	public static void addSystemProperties(Properties props) {
		props.putAll(getSystemProperties());
	}

	/**
	 * {@link System#getProperties()} is unsafe as it gives potentially access to secrets
	 * 
	 * @return
	 */
	public static final Properties getSafeSystemProperties() {
		var safe = new Properties();

		// JdkVersionProfileActivator.isActive(Profile, ProfileActivationContext, ModelProblemCollector)
		addIfPresent(safe, "java.version");

		return safe;
	}

	private static void addIfPresent(Properties safe, String key) {
		var optJavaVersion = System.getProperty(key);
		if (!Strings.isNullOrEmpty(optJavaVersion)) {
			safe.put(key, optJavaVersion);
		}
	}

	/**
	 * Returns a copy of {@link System#getProperties()} in a thread-safe manner.
	 *
	 * @return {@link System#getProperties()} obtained in a thread-safe manner.
	 */
	public static Properties getSystemProperties() {
		return copyProperties(getSafeSystemProperties());
	}

	/**
	 * Copies the given {@link Properties} object into a new {@link Properties} object, in a thread-safe manner.
	 * 
	 * @param properties
	 *            Properties to copy.
	 * @return Copy of the given properties.
	 */
	public static Properties copyProperties(Properties properties) {
		final var copyProperties = new Properties();
		// guard against modification/removal of keys in the given properties (MNG-5670, MNG-6053, MNG-6105)
		synchronized (properties) {
			copyProperties.putAll(properties);
		}
		return copyProperties;
	}
}