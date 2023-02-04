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
package org.apache.maven.resolver.examples.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A helper to boot the repository system and a repository system session.
 * 
 * @author https://github.com/apache/maven-resolver/
 */
// CHECKSTYLE:OFF: checkstyle:LineLength
// https://github.com/apache/maven-resolver/blob/master/maven-resolver-demos/maven-resolver-demo-snippets/src/main/java/org/apache/maven/resolver/examples/util/Booter.java
// CHECKSTYLE:ON: checkstyle:LineLength
@SuppressWarnings({ "PMD", "checkstyle:HideUtilityClassConstructor" })
public class Booter {
	public static final String SERVICE_LOCATOR = "serviceLocator";

	public static final String GUICE = "guice";

	public static final String SISU = "sisu";

	public static String selectFactory(String[] args) {
		if (args == null || args.length == 0) {
			return SERVICE_LOCATOR;
		} else {
			return args[0];
		}
	}

	public static RepositorySystem newRepositorySystem(final String factory) {
		switch (factory) {
		case SERVICE_LOCATOR:
			return org.apache.maven.resolver.examples.manual.ManualRepositorySystemFactory.newRepositorySystem();
		// case GUICE:
		// return org.apache.maven.resolver.examples.guice.GuiceRepositorySystemFactory.newRepositorySystem();
		// case SISU:
		// return org.apache.maven.resolver.examples.sisu.SisuRepositorySystemFactory.newRepositorySystem();
		default:
			throw new IllegalArgumentException("Unknown factory: " + factory);
		}
	}

	public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system,
			Path localRepoPath) {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

		LocalRepository localRepo = new LocalRepository(localRepoPath.toFile());
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

		session.setTransferListener(new LoggingTransferListener());
		session.setRepositoryListener(new LoggingRepositoryListener());

		// uncomment to generate dirty trees
		// session.setDependencyGraphTransformer( null );

		return session;
	}

	public static List<RemoteRepository> newRepositories(RepositorySystem system, RepositorySystemSession session) {
		return new ArrayList<>(Collections.singletonList(newCentralRepository()));
	}

	private static RemoteRepository newCentralRepository() {
		return new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
	}

}
