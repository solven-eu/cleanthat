package eu.solven.cleanthat.language.xml;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.CharMatcher;

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
	String indentationAsString = ICommonConventions.DEFAULT_INDENTATION;
	int indentationAsWhitespaces = DEFAULT_INDENT_WHITESPACES;

	boolean spaceBeforeSeparator = true;
	boolean alphabeticalOrder = false;
	boolean eolAtEof = false;

	/**
	 * '-1' would mean '\t'
	 */
	public int getIndentationAsWhitespaces() {
		return indentationAsWhitespaces;
	}

	/**
	 * '-1' would mean '\t'
	 * 
	 * @param indentationAsWhitespaces
	 */
	public void setIndentationAsWhitespaces(int indentationAsWhitespaces) {
		this.indentationAsWhitespaces = indentationAsWhitespaces;

		if (indentationAsWhitespaces >= 0) {
			this.indentationAsString =
					IntStream.range(0, indentationAsWhitespaces).mapToObj(i -> " ").collect(Collectors.joining());
		} else {
			this.indentationAsString = "\t";
		}
	}

	public String getIndentationAsString() {
		return indentationAsString;
	}

	public void setIndentationAsString(String indentation) {
		this.indentationAsString = indentation;

		if ("\t".equals(indentation) || "\\t".equals(indentation)) {
			indentationAsWhitespaces = ICommonConventions.DEFAULT_INDENT_FOR_TAB;
		} else {
			if (!CharMatcher.anyOf(" ").matchesAllOf(indentation)) {
				throw new IllegalArgumentException("Expected '\\t' or a number of whitespaces");
			}

			indentationAsWhitespaces = indentation.length();
		}
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
