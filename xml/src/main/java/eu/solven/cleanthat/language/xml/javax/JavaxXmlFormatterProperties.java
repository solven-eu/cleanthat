package eu.solven.cleanthat.language.xml.javax;

import java.util.Map;

import javax.xml.transform.OutputKeys;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.formatter.ICommonConventions;
import eu.solven.cleanthat.language.SourceCodeProperties;

/**
 * Configuration for Jackson Json formatter
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JavaxXmlFormatterProperties {
	String indentation = ICommonConventions.DEFAULT_INDENTATION;

	Map<String, String> outputOptions = ImmutableMap.<String, String>builder()
			.put(OutputKeys.OMIT_XML_DECLARATION, "no")
			.put(OutputKeys.INDENT, "yes")
			.build();

	String lineending = SourceCodeProperties.DEFAULT_LINE_ENDING;

	boolean eolAtEof = false;

	public String getIndentation() {
		return indentation;
	}

	public void setIndentation(String indentation) {
		this.indentation = indentation;
	}

	public Map<String, String> getOutputOptions() {
		return outputOptions;
	}

	public void setOutputOptions(Map<String, String> outputOptions) {
		this.outputOptions = outputOptions;
	}

	public String getLineending() {
		return lineending;
	}

	public void setLineending(String lineending) {
		this.lineending = lineending;
	}

	public boolean isEolAtEof() {
		return eolAtEof;
	}

	public void setEolAtEof(boolean eolAtEof) {
		this.eolAtEof = eolAtEof;
	}
}
