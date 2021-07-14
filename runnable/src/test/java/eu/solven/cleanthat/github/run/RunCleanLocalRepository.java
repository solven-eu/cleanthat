package eu.solven.cleanthat.github.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.jgit.JGitCodeProvider;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

public class RunCleanLocalRepository extends ACleanThatXxxApplication {
	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(RunCleanLocalRepository.class);

		springApp.setWebApplicationType(WebApplicationType.NONE);

		springApp.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		Path repoFolder = Paths.get(System.getProperty("user.home"), "workspace2", "spring-boot");

		Git jgit = Git.open(repoFolder.toFile());

		JGitCodeProvider codeProvider =
				new JGitCodeProvider(repoFolder, jgit, JGitCodeProvider.getHeadName(jgit.getRepository()));

		ApplicationContext appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		File pathToConfig = CodeProviderHelpers.pathToConfig(repoFolder);

		CleanthatRepositoryProperties properties =
				appContext.getBean(ObjectMapper.class).readValue(pathToConfig, CleanthatRepositoryProperties.class);
		codeProviderFormatter.formatCode(properties, codeProvider);
	}

}
