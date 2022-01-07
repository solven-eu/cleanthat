package eu.solven.cleanthat.language.xml.revelc;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * Configuration for Jackson Json formatter
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RevelcXmlFormatterProperties {

	private static final int DEFAULT_INDENT_WHITESPACES = 4;

	int indent = DEFAULT_INDENT_WHITESPACES;

	String lineending = SourceCodeProperties.DEFAULT_LINE_ENDING;

	boolean spaceBeforeSeparator = true;
	boolean alphabeticalOrder = false;
	boolean eolAtEof = false;

	public int getIndent() {
		return indent;
	}

	public void setIndent(int indent) {
		this.indent = indent;
	}

	public String getLineending() {
		return lineending;
	}

	public void setLineending(String lineending) {
		this.lineending = lineending;
	}

	public boolean isSpaceBeforeSeparator() {
		return spaceBeforeSeparator;
	}

	public void setSpaceBeforeSeparator(boolean spaceBeforeSeparator) {
		this.spaceBeforeSeparator = spaceBeforeSeparator;
	}

	public boolean isAlphabeticalOrder() {
		return alphabeticalOrder;
	}

	public void setAlphabeticalOrder(boolean alphabeticalOrder) {
		this.alphabeticalOrder = alphabeticalOrder;
	}

	public boolean isEolAtEof() {
		return eolAtEof;
	}

	public void setEolAtEof(boolean eolAtEof) {
		this.eolAtEof = eolAtEof;
	}
}
