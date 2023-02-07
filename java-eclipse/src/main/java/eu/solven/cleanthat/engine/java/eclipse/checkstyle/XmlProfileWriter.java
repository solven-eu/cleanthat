/*
 * Copyright 2002-2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.engine.java.eclipse.checkstyle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class to write eclipse formatter/cleanup profile XML files.
 *
 * @author Alexandros Karypidis
 *
 */
public final class XmlProfileWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlProfileWriter.class);

	private static final String XML_NODE_ROOT = "profiles";
	private static final String XML_NODE_PROFILE = "profile";
	private static final String XML_NODE_SETTING = "setting";

	private static final String XML_ATTRIBUTE_VERSION = "version";
	private static final String XML_ATTRIBUTE_ID = "id";
	private static final String XML_ATTRIBUTE_NAME = "name";
	private static final String XML_ATTRIBUTE_PROFILE_KIND = "kind";
	private static final String XML_ATTRIBUTE_VALUE = "value";

	private static final String CLEANUP_PROFILE_VERSION = "2";
	private static final String CLEANUP_PROFILE_KIND = "CleanUpProfile";

	private static final String FORMATTER_PROFILE_VERSION = "10";
	private static final String FORMATTER_PROFILE_KIND = "CodeFormatterProfile";

	private XmlProfileWriter() {
		// no code
	}

	public static InputStream writeCleanupProfileToStream(String name, Map<String, String> settings)
			throws TransformerException, ParserConfigurationException {
		return writeProfileToStream(name, CLEANUP_PROFILE_VERSION, CLEANUP_PROFILE_KIND, settings);
	}

	public static InputStream writeFormatterProfileToStream(String name, Map<String, String> settings)
			throws TransformerException, ParserConfigurationException {
		return writeProfileToStream(name, FORMATTER_PROFILE_VERSION, FORMATTER_PROFILE_KIND, settings);
	}

	private static InputStream writeProfileToStream(String name,
			String profileVersion,
			String profileKind,
			Map<String, String> settings) throws TransformerException, ParserConfigurationException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.newDocument();

		final Element rootElement = document.createElement(XML_NODE_ROOT);
		rootElement.setAttribute(XML_ATTRIBUTE_VERSION, profileVersion);

		document.appendChild(rootElement);

		final Element profileElement = document.createElement(XML_NODE_PROFILE);
		profileElement.setAttribute(XML_ATTRIBUTE_NAME, name);
		profileElement.setAttribute(XML_ATTRIBUTE_VERSION, profileVersion);
		profileElement.setAttribute(XML_ATTRIBUTE_PROFILE_KIND, profileKind);

		final Iterator<String> keyIter = settings.keySet().iterator();

		while (keyIter.hasNext()) {
			final String key = keyIter.next();
			final String value = settings.get(key);
			if (value != null) {
				final Element setting = document.createElement(XML_NODE_SETTING);
				setting.setAttribute(XML_ATTRIBUTE_ID, key);
				setting.setAttribute(XML_ATTRIBUTE_VALUE, value);
				profileElement.appendChild(setting);
			} else {
				LOGGER.info("Profile is missing value for [key={}]", key);
			}
		}
		rootElement.appendChild(profileElement);

		final Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		final StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
	}

}