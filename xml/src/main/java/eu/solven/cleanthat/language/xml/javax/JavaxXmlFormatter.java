package eu.solven.cleanthat.language.xml.javax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.base.CharMatcher;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.formatter.ICommonConventions;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.ISourceCodeProperties;

/**
 * Formatter for XML
 *
 * @author Benoit Lacelle
 */
// https://stackoverflow.com/questions/25864316/pretty-print-xml-in-java-8/33541820#33541820
public class JavaxXmlFormatter implements ILintFixerWithId {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaxXmlFormatter.class);

	final ISourceCodeProperties sourceCodeProperties;

	final JavaxXmlFormatterProperties properties;

	public JavaxXmlFormatter(ISourceCodeProperties sourceCodeProperties, JavaxXmlFormatterProperties properties) {
		this.sourceCodeProperties = sourceCodeProperties;
		this.properties = properties;
	}

	@Override
	public String getId() {
		return "javax";
	}

	@Override
	public String doFormat(String code, LineEnding ending) throws IOException {
		try {
			return unsafeDoFormat(code);
		} catch (ParserConfigurationException | TransformerException | SAXException | XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	protected String unsafeDoFormat(String code)
			throws SAXException, IOException, ParserConfigurationException, XPathExpressionException,
			TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		LOGGER.debug("About to process an XML of {} characters", PepperLogHelper.humanBytes(code.length()));

		// Turn xml string into a document
		Document document = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder()
				.parse(new InputSource(new ByteArrayInputStream(code.getBytes(sourceCodeProperties.getEncoding()))));

		// Remove whitespaces outside tags
		document.normalize();
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodeList =
				(NodeList) xPath.evaluate("//text()[normalize-space()='']", document, XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
		}

		int indentationCount = getIndentationCount();

		// Setup pretty print options
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		// https://stackoverflow.com/questions/1384802/java-how-to-indent-xml-generated-by-transformer
		transformerFactory.setAttribute("indent-number", indentationCount);
		Transformer transformer = transformerFactory.newTransformer();

		// Apply some default configuration
		transformer.setOutputProperty(OutputKeys.ENCODING, sourceCodeProperties.getEncoding());
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indentationCount));

		// Override default properties by the one in the configuration
		properties.getOutputOptions().forEach((k, v) -> transformer.setOutputProperty(k, v));

		// Return pretty print xml string
		StringWriter stringWriter = new StringWriter(code.length());
		transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

		return stringWriter.toString();
	}

	protected int getIndentationCount() {
		String indentation = properties.getIndentation();
		if ("\t".equals(indentation)) {
			// We replace '\t' by convention indentation as javax does not accept indenting by tabs
			indentation = ICommonConventions.DEFAULT_INDENTATION;
			LOGGER.warn("This can not indent with '\t'");
		}
		// Consider indentation based on whitespaces
		int whitespaces = CharMatcher.is(' ').countIn(indentation);

		if (whitespaces != indentation.length()) {
			LOGGER.warn("We can not indent exactly given indentation={}", indentation);
		}

		return whitespaces;
	}

}
