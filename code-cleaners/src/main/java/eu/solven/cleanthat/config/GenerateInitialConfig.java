package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.compress.utils.FileNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.LanguageProperties;

/**
 * Helps generating a default {@link CleanthatRepositoryProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public class GenerateInitialConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateInitialConfig.class);

	final Collection<ILanguageLintFixerFactory> factories;

	public GenerateInitialConfig(Collection<ILanguageLintFixerFactory> factories) {
		this.factories = factories;
	}

	// Guess Java version: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L13
	// Detect usage of Checkstyle:
	// https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L35
	// Code formatting: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L17
	// https://github.com/spring-io/spring-javaformat/blob/master/src/checkstyle/checkstyle.xml
	// com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck
	public CleanthatRepositoryProperties prepareDefaultConfiguration(ICodeProvider codeProvider) {
		CleanthatRepositoryProperties properties = new CleanthatRepositoryProperties();

		Set<String> extentionsFound = scanFileExtentions(codeProvider);

		factories.forEach(factory -> {
			if (!Sets.intersection(factory.getFileExtentions(), extentionsFound).isEmpty()) {
				LOGGER.info("There is a file-extension match for {}", factory);

				LanguageProperties languageProperties = factory.makeDefaultProperties();
				properties.getLanguages().add(languageProperties);
			}
		});

		return properties;
	}

	public Set<String> scanFileExtentions(ICodeProvider codeProvider) {
		Set<String> extentionsFound = new TreeSet<>();

		try {
			codeProvider.listFiles(file -> {
				String filePath = file.getPath();

				String extention = FileNameUtils.getExtension(filePath);

				extentionsFound.add(extention);
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue listing all files for extentions", e);
		}

		LOGGER.info("Extentions found in {}: {}", codeProvider, extentionsFound);

		return extentionsFound;
	}

}
