package eu.solven.cleanthat.language.xml;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.formatter.ICommonConventions;

/**
 * Default XML properties
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DefaultXmlFormatterProperties {
	private static final int DEFAULT_INDENT_WHITESPACES = ICommonConventions.DEFAULT_INDENTATION.length();

	// '-1' would mean '\t'
	int indent = DEFAULT_INDENT_WHITESPACES;

	boolean spaceBeforeSeparator = true;
	boolean alphabeticalOrder = false;
	boolean eolAtEof = false;

	/**
	 * '-1' would mean '\t'
	 */
	public int getIndent() {
		return indent;
	}

	/**
	 * '-1' would mean '\t'
	 * 
	 * @param indent
	 */
	public void setIndent(int indent) {
		this.indent = indent;
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
