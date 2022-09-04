package eu.solven.cleanthat.language.xml.ekryd_sortpom;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.ec4j.linters.XmlLinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.utils.FormatterHelpers;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import sortpom.SortPomImpl;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

/**
 * Formatter for XML with EC4J
 *
 * @author Benoit Lacelle
 */
// https://github.com/ec4j/editorconfig-linters/blob/master/editorconfig-linters/src/main/java/org/ec4j/linters/XmlLinter.java
public class EcrydSortPomFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(EcrydSortPomFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;

	final EcrydSortPomFormatterProperties properties;

	final XmlLinter xmlLinter;

	public EcrydSortPomFormatter(ISourceCodeProperties sourceCodeProperties,
			EcrydSortPomFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;

		this.xmlLinter = new XmlLinter();
	}

	@Override
	public String getId() {
		return "ecryd_sortpom";
	}

	// https://github.com/Ekryd/sortpom/blob/master/sorter/src/test/java/sortpom/util/SortPomImplUtil.java
	// https://github.com/Ekryd/sortpom/blob/master/maven-plugin/src/main/java/sortpom/SortMojo.java
	private PluginParameters getPluginParameters(File pomFile) {
		String encoding = sourceCodeProperties.getEncoding();
		return PluginParameters.builder()
				.setPomFile(pomFile)
				.setFileOutput(properties.createBackupFile,
						properties.testPomBackupExtension,
						properties.violationFile,
						properties.keepTimestamp)
				.setEncoding(encoding)
				.setFormatting(LineEnding.getOrGuess(sourceCodeProperties.getLineEndingAsEnum(), () -> {
					try {
						return Files.readString(pomFile.toPath(), Charset.forName(encoding));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}), properties.expandEmptyElements, properties.spaceBeforeCloseEmptyElement, properties.keepBlankLines)
				.setIndent(properties.getNrOfIndentSpace(),
						properties.indentBLankLines,
						properties.indentSchemaLocation)
				.setSortEntities(properties.sortDependencies,
						properties.sortDependencyExclusions,
						properties.sortDependencyManagement,
						properties.sortPlugins,
						properties.sortProperties,
						properties.sortModules,
						properties.sortExecutions)
				.setSortOrder(properties.sortOrderFile, properties.predefinedSortOrder)
				// Used in Check, not in Sort
				// .setVerifyFail(verifyFail, verifyFailOn)
				.setIgnoreLineSeparators(properties.ignoreLineSeparators)
				.build();
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		Charset charset = Charset.forName(sourceCodeProperties.getEncoding());

		String sorted = FormatterHelpers.formatAsFile(code, charset, path -> {
			final SortPomImpl sortPomImpl = new SortPomImpl();

			PluginParameters pluginParameters = getPluginParameters(path.toFile());
			SortPomLogger sortPomLogger = new SortPomLogger() {

				@Override
				public void warn(String content) {
					LOGGER.warn("SortPom: {}", content);
				}

				@Override
				public void info(String content) {
					LOGGER.info("SortPom: {}", content);
				}

				@Override
				public void error(String content) {
					LOGGER.error("SortPom: {}", content);
				}
			};
			sortPomImpl.setup(sortPomLogger, pluginParameters);

			sortPomImpl.sortPom();

			return Files.readString(path, charset);
		});

		if (!Objects.equal(code, sorted)) {
			LOGGER.debug("We sorted a pom.xml");
		}

		return sorted;
	}

}
