package io.cormoran.cleanthat.formatter.eclipse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import io.cormoran.cleanthat.formatter.LineEnding;
import net.revelc.code.impsort.Grouper;
import net.revelc.code.impsort.ImpSort;
import net.revelc.code.impsort.Result;

/**
 * Formatter for Java
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatter.class);

	@Override
	public String format(CleanThatRepositoryProperties properties, String asString) throws IOException {
		String output;
		// try {
		LineEnding eolToApply;
		if (properties.getLineEnding() == LineEnding.KEEP) {
			eolToApply = LineEnding.determineLineEnding(asString);
		} else {
			eolToApply = properties.getLineEnding();
		}

		output = new EclipseJavaFormatter(properties).doFormat(asString, eolToApply);

		// see net.revelc.code.impsort.maven.plugin.AbstractImpSortMojo
		Grouper grouper = new Grouper(properties.getGroups(), properties.getStaticGroups(), false, false, true);
		Charset charset = Charset.forName(properties.getEncoding());
		ImpSort impsort = new ImpSort(charset,
				grouper,
				properties.isRemoveUnusedImports(),
				true,
				net.revelc.code.impsort.LineEnding.valueOf(eolToApply.name()));

		Path tmpFile = Files.createTempFile("cleanthat", ".tmp");
		Files.writeString(tmpFile, output, charset, StandardOpenOption.TRUNCATE_EXISTING);

		Result result = impsort.parseFile(tmpFile);
		if (!result.isSorted()) {
			LOGGER.info("Saving imports-sorted file to {}", tmpFile);
			result.saveSorted(tmpFile);
			LOGGER.info("Loading imports-sorted file to {}", tmpFile);
			output = new String(Files.readAllBytes(tmpFile), charset);
		}

		return output;
	}

}
