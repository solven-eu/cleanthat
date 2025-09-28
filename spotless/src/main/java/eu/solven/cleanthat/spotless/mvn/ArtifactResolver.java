/*
 * Copyright 2016-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.spotless.mvn;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Duplicates com.diffplug.spotless.maven.ArtifactResolver, as it is difficult to import from a maven-plugin jar.
 * 
 * This enables to download and load in classpath jars, following the mvn scheme (especially regarding transitive
 * dependencies).
 * 
 * @author Diffplug
 * @see com.diffplug.spotless.maven.ArtifactResolver
 *
 */
@Slf4j
public class ArtifactResolver {

	private static final Exclusion EXCLUDE_ALL_TRANSITIVES = new Exclusion("*", "*", "*", "*");

	private final RepositorySystem repositorySystem;
	private final RepositorySystemSession session;
	private final List<RemoteRepository> repositories;

	public ArtifactResolver(RepositorySystem repositorySystem,
			RepositorySystemSession session,
			List<RemoteRepository> repositories) {
		this.repositorySystem = Objects.requireNonNull(repositorySystem);
		this.session = Objects.requireNonNull(session);
		this.repositories = Objects.requireNonNull(repositories);
	}

	/**
	 * Given a set of maven coordinates, returns a set of jars which include all of the specified coordinates and
	 * optionally their transitive dependencies.
	 */
	public Set<File> resolve(boolean withTransitives, Collection<String> mavenCoordinates) {
		Collection<Exclusion> excludeTransitive = new ArrayList<Exclusion>(1);
		if (!withTransitives) {
			excludeTransitive.add(EXCLUDE_ALL_TRANSITIVES);
		}
		List<Dependency> dependencies = mavenCoordinates.stream()
				.map(coordinateString -> new DefaultArtifact(coordinateString))
				.map(artifact -> new Dependency(artifact, null, null, excludeTransitive))
				.collect(toList());
		CollectRequest collectRequest = new CollectRequest(dependencies, null, repositories);
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
		DependencyResult dependencyResult = resolveDependencies(dependencyRequest);

		return dependencyResult.getArtifactResults()
				.stream()
				.peek(this::logResolved)
				.map(ArtifactResult::getArtifact)
				.map(Artifact::getFile)
				.collect(toSet());
	}

	private DependencyResult resolveDependencies(DependencyRequest dependencyRequest) {
		try {
			return repositorySystem.resolveDependencies(session, dependencyRequest);
		} catch (DependencyResolutionException e) {
			// throw new ArtifactResolutionException("Unable to resolve dependencies", e);
			throw new IllegalStateException("Unable to resolve dependencies", e);
		}
	}

	private void logResolved(ArtifactResult artifactResult) {
		LOGGER.info("Resolved artifact: {}", artifactResult);
	}
}
