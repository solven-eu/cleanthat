package eu.solven.cleanthat.language.xml.revelc;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import net.revelc.code.formatter.xml.lib.FormattingPreferences;
import net.revelc.code.formatter.xml.lib.XmlDocumentFormatter;

/**
 * Formatter for XML with Revelc
 *
 * @author Benoit Lacelle
 */
// https://github.com/revelc/formatter-maven-plugin/blob/main/src/main/java/net/revelc/code/formatter/xml/XMLFormatter.java
public class RevelcXmlFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(RevelcXmlFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;

	final RevelcXmlFormatterProperties properties;

	public RevelcXmlFormatter(ISourceCodeProperties sourceCodeProperties, RevelcXmlFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;
	}

	private XmlDocumentFormatter makeFormatter(String code) {
		ObjectMapper objectMapper = ConfigHelpers.makeJsonObjectMapper();
		// parseRevelcPreferences expect lowerCamelCase, as in original Revelc implementation
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
		Map<String, Object> options = objectMapper.convertValue(properties, Map.class);

		FormattingPreferences prefs = parseRevelcPreferences(options);

		return new XmlDocumentFormatter(getEol(options, sourceCodeProperties, code), prefs);
	}

	private String getEol(Map<String, Object> options, ISourceCodeProperties sourceCodeProperties, String dirtyCode) {
		// https://github.com/revelc/formatter-maven-plugin/blob/main/src/main/java/net/revelc/code/formatter/xml/XMLFormatter.java#L55
		Object revelcLineEnding = options.get("lineending");

		if (revelcLineEnding != null) {
			return revelcLineEnding.toString();
		}

		return LineEnding.getOrGuess(sourceCodeProperties.getLineEndingAsEnum(), () -> dirtyCode);
	}

	@Override
	public String getId() {
		return "revelc";
	}

	@SuppressWarnings("checkstyle:AvoidInlineConditionals")
	protected final FormattingPreferences parseRevelcPreferences(Map<String, Object> options) {
		FormattingPreferences prefs = new FormattingPreferences();

		// https://github.com/revelc/formatter-maven-plugin/blob/main/src/main/java/net/revelc/code/formatter/xml/XMLFormatter.java
		String maxLineLength = (String) options.get("maxLineLength");
		Boolean wrapLongLines = (Boolean) options.get("wrapLongLines");
		Boolean tabInsteadOfSpaces = (Boolean) options.get("tabInsteadOfSpaces");
		Integer tabWidth = (Integer) options.get("tabWidth");
		Boolean splitMultiAttrs = (Boolean) options.get("splitMultiAttrs");
		String wellFormedValidation = (String) options.get("wellFormedValidation");

		prefs.setMaxLineLength(maxLineLength != null ? Integer.valueOf(maxLineLength) : null);
		prefs.setWrapLongLines(wrapLongLines != null ? Boolean.valueOf(wrapLongLines) : null);
		prefs.setTabInsteadOfSpaces(tabInsteadOfSpaces != null ? Boolean.valueOf(tabInsteadOfSpaces) : null);
		prefs.setTabWidth(tabWidth != null ? Integer.valueOf(tabWidth) : null);
		prefs.setSplitMultiAttrs(splitMultiAttrs != null ? Boolean.valueOf(splitMultiAttrs) : null);

		if (wellFormedValidation != null) {
			prefs.setWellFormedValidation(wellFormedValidation);
		}
		return prefs;
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		String formattedCode = makeFormatter(code).format(code);

		// Append an EOL
		if (properties.isEolAtEof() && !formattedCode.endsWith(ending.optChars().orElseThrow())) {
			formattedCode = formattedCode + ending.optChars();
		}

		if (code.equals(formattedCode)) {
			// Return the original reference
			LOGGER.debug("No impact");
			return code;
		}

		return formattedCode;
	}

}
