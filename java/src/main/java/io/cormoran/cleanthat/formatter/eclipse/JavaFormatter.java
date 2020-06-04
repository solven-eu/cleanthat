package io.cormoran.cleanthat.formatter.eclipse;

import java.io.IOException;

import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import io.cormoran.cleanthat.formatter.LineEnding;

/**
 * Formatter for Java
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatter implements IStringFormatter {

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
		return output;
	}

}
