package eu.solven.cleanthat.language.json.jackson;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.formatter.ICommonConventions;
import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * Configuration for Jackson Json formatter
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JacksonJsonFormatterProperties {

	@Deprecated(since = "Replaced by indentation")
	int indent = -1;
	String indentation = ICommonConventions.DEFAULT_INDENTATION;

	String lineending = SourceCodeProperties.DEFAULT_LINE_ENDING;

	boolean spaceBeforeSeparator = true;
	boolean alphabeticalOrder = false;
	boolean eolAtEof = false;

	@Deprecated
	public int getIndent() {
		return indent;
	}

	@Deprecated
	public void setIndent(int indent) {
		this.indent = indent;
	}

	public String getIndentation() {
		return indentation;
	}

	public void setIndentation(String indentation) {
		this.indentation = indentation;
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
