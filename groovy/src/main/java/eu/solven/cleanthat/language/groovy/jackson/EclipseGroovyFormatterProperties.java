package eu.solven.cleanthat.language.groovy.jackson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.Strings;

import eu.solven.cleanthat.formatter.ICommonConventions;

/**
 * Configuration for Jackson Json formatter
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class EclipseGroovyFormatterProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseGroovyFormatterProperties.class);

	@Deprecated(since = "Replaced by indentation")
	int indent = -1;
	String indentation = ICommonConventions.DEFAULT_INDENTATION;

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

	@Deprecated(since = "Rely on ISourceCodeProperties")
	public void setLineending(String lineending) {
		if (!Strings.isNullOrEmpty(lineending)) {
			LOGGER.warn("This property is not used anymore");
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
