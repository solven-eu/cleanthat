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
package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.diffplug.spotless.Provisioner;

import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.spotless.mvn.ArtifactResolver;
import eu.solven.cleanthat.spotless.mvn.MavenProvisioner;
import io.sentry.IHub;

/**
 * Mojo relying on a Spring {@link ApplicationContext}
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatSpringMojo extends ACleanThatMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(ACleanThatSpringMojo.class);

	protected static final AtomicReference<ACleanThatSpringMojo> CURRENT_MOJO = new AtomicReference<>();

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	/**
	 * The SpringBoot application started within maven Mojo
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@SpringBootApplication(scanBasePackages = "none")
	public static class MavenSpringConfig implements CommandLineRunner {

		@Autowired
		ApplicationContext appContext;

		@Bean
		public Provisioner mvnCliProvisionner() {
			ACleanThatSpringMojo mojo = CURRENT_MOJO.get();
			ArtifactResolver resolver =
					new ArtifactResolver(mojo.repositorySystem, mojo.repositorySystemSession, mojo.repositories);
			return MavenProvisioner.create(resolver);
		}

		@Override
		public void run(String... args) throws Exception {
			LOGGER.info("Processing arguments: {}", Arrays.asList(args));

			// Ensure events are sent to Sentry
			IHub sentryHub = appContext.getBean(IHub.class);
			sentryHub.captureMessage("Maven is OK");

			LOGGER.info("sha1: {}{}",
					"https://github.com/solven-eu/cleanthat/commit/",
					appContext.getBean(IGitService.class).getSha1());

			CURRENT_MOJO.get().doClean(appContext);
			sentryHub.flush(TimeUnit.SECONDS.toMillis(1));
		}
	}

	// Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html
	@Override
	public void execute() throws MojoExecutionException {
		getLog().debug("Hello, world.");
		checkParameters();

		if (isSkip()) {
			LOGGER.info("Mojo is skipped. May be turned on with '-Dcleanthat.skip=false'");
			return;
		}

		if (CURRENT_MOJO.compareAndSet(null, this)) {
			LOGGER.debug("Start applicationContext");
			try {
				List<Class<?>> classes = new ArrayList<>();

				classes.add(MavenSpringConfig.class);
				classes.addAll(springClasses());

				SpringApplication.run(classes.toArray(Class<?>[]::new), new String[0]);
			} finally {
				LOGGER.debug("Closed applicationContext");
				// Beware to clean so that it is OK in a multiModule reactor
				CURRENT_MOJO.set(null);
			}
		} else {
			throw new IllegalStateException("We have a leftover Mojo");
		}
	}

	protected abstract void doClean(ApplicationContext appContext) throws IOException, MojoFailureException;

	protected abstract List<? extends Class<?>> springClasses();
}