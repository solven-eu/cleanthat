package eu.solven.cleanthat.language.xml.ec4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.ec4j.lint.api.Resource;
import org.ec4j.linters.XmlLinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Formatter for XML with EC4J
 *
 * @author Benoit Lacelle
 */
// https://github.com/ec4j/editorconfig-linters/blob/master/editorconfig-linters/src/main/java/org/ec4j/linters/XmlLinter.java
public class Ec4jXmlFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(Ec4jXmlFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;

	final Ec4jXmlFormatterProperties properties;

	final XmlLinter xmlLinter;

	public Ec4jXmlFormatter(ISourceCodeProperties sourceCodeProperties, Ec4jXmlFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;

		this.xmlLinter = new XmlLinter();
	}

	@Override
	public String getId() {
		return "ec4j";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		LOGGER.warn("TODO");
		xmlLinter.process(new Resource(null, null, StandardCharsets.UTF_8, code), null, null);

		return code;
	}

}
