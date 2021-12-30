package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.Collection;

import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;

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
		Collection<ObjectMapper> objectMappers = appContext.getBeansOfType(ObjectMapper.class).values();
		Collection<ILanguageLintFixerFactory> factories =
				appContext.getBeansOfType(ILanguageLintFixerFactory.class).values();
		ICodeProviderFormatter codeProviderFormatter = appContext.getBean(ICodeProviderFormatter.class);
		return new MavenCodeCleaner(objectMappers, factories, codeProviderFormatter);
	}

	// Process the root of current module
	public static ICodeProviderWriter makeCodeProviderWriter(ACleanThatMojo cleanThatCleanThatMojo) {
		File baseDir = cleanThatCleanThatMojo.getBaseDir();
		return new FileSystemCodeProvider(baseDir.toPath());
	}
}
