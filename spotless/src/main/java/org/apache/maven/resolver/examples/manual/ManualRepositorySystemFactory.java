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
package org.apache.maven.resolver.examples.manual;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for repository system instances that employs Maven Artifact Resolver's built-in service locator
 * infrastructure to wire up the system's components.
 * 
 * @author https://github.com/apache/maven-resolver/
 */
// CHECKSTYLE:OFF: checkstyle:LineLength
// https://github.com/apache/maven-resolver/blob/master/maven-resolver-demos/maven-resolver-demo-snippets/src/main/java/org/apache/maven/resolver/examples/manual/ManualRepositorySystemFactory.java
// CHECKSTYLE:ON: checkstyle:LineLength
@SuppressWarnings({ "PMD", "checkstyle:HideUtilityClassConstructor" })
public class ManualRepositorySystemFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManualRepositorySystemFactory.class);

	public static RepositorySystem newRepositorySystem() {
		/*
		 * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
		 * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
		 * factories.
		 */
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

		locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
			@Override
			public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
				LOGGER.error("Service creation failed for {} with implementation {}", type, impl, exception);
			}
		});

		return locator.getService(RepositorySystem.class);
	}

}
