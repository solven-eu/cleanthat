package eu.solven.cleanthat.language.groovy.jackson;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/main/src/main/java/net/revelc/code/formatter/json/JsonFormatter.java
public class EclipseGroovyFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(EclipseGroovyFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;
	final EclipseGroovyFormatterProperties properties;

	final ObjectMapper formatter;

	public EclipseGroovyFormatter(ISourceCodeProperties sourceCodeProperties,
			EclipseGroovyFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;

		boolean useAlphabeticalOrder = properties.isAlphabeticalOrder();
		formatter = new ObjectMapper();

		// DefaultPrettyPrinter printer = makePrinter(indent, spaceBeforeSeparator);
		// formatter.setDefaultPrettyPrinter(printer);
		formatter.enable(SerializationFeature.INDENT_OUTPUT);
		formatter.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, useAlphabeticalOrder);
	}

	@Override
	public String getId() {
		return "eclipse";
	}

	protected DefaultPrettyPrinter makePrinter(String eol) {
		boolean spaceBeforeSeparator = properties.isSpaceBeforeSeparator();

		// Setup a pretty printer with an indenter (indenter has 4 spaces in this case)
		DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(getIndentation(), eol);
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter() {
			private static final long serialVersionUID = 1L;

			@Override
			public DefaultPrettyPrinter createInstance() {
				return new DefaultPrettyPrinter(this);
			}

			@Override
			public DefaultPrettyPrinter withSeparators(Separators separators) {
				this._separators = separators;
				if (spaceBeforeSeparator) {
					this._objectFieldValueSeparatorWithSpaces = " " + separators.getObjectFieldValueSeparator() + " ";
				} else {
					this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
				}
				return this;
			}
		};

		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		return printer;
	}

	private String getIndentation() {
		int deprecatedIndent = properties.getIndent();

		if (deprecatedIndent >= 0) {
			// Deprecated behavior: indentation is defined by a number of whitespaces
			return Strings.repeat(" ", deprecatedIndent);
		} else {
			return properties.getIndentation();
		}
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		String eol = LineEnding.getOrGuess(ending, () -> code);

		// Clone objectMapper as we will change its prettyPrinter
		ObjectMapper localFormatter = formatter.copy();

		localFormatter.setDefaultPrettyPrinter(makePrinter(eol));

		Object json = formatter.readValue(code, Object.class);
		String formattedCode = formatter.writer().writeValueAsString(json);

		// Append an EOL
		if (properties.isEolAtEof() && !formattedCode.endsWith(eol)) {
			formattedCode = formattedCode + eol;
		}

		if (code.equals(formattedCode)) {
			// Return the original reference
			LOGGER.debug("No impact");
			return code;
		}

		return formattedCode;
	}

}
