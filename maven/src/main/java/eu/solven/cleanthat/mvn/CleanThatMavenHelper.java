package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * Helper methods in the context of a mvn plugin
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanThatMavenHelper {
	protected CleanThatMavenHelper() {
		// hidden
	}

	public static MavenCodeCleaner makeCodeCleaner(ApplicationContext appContext) {
		List<ObjectMapper> objectMappers =
				appContext.getBeansOfType(ObjectMapper.class).values().stream().collect(Collectors.toList());
		ICodeProviderFormatter codeProviderFormatter = appContext.getBean(ICodeProviderFormatter.class);
		return new MavenCodeCleaner(objectMappers, codeProviderFormatter);
	}

	// Process the root of current module
	public static ICodeProviderWriter makeCodeProviderWriter(ACleanThatMojo cleanThatCleanThatMojo) {
		File baseDir = cleanThatCleanThatMojo.getBaseDir();
		return new FileSystemCodeProvider(baseDir.toPath());
	}
}
