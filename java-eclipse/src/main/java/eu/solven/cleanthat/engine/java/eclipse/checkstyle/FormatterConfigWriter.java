/*
 * Copyright 2009-2025 Benoit Lacelle - SOLVEN
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import lombok.extern.slf4j.Slf4j;

/**
 * Class for writing a new eclipse-configuration-file. Gets used by class Transformer. Two eclipse-formatter-profile
 * files gets written to the project root.
 *
 * @author Alexandros Karypidis
 * @author Lukas Frena
 * @author Lars Ködderitzsch
 */
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/transformer/FormatterConfigWriter.java
@Slf4j
public class FormatterConfigWriter {

	/** Constant for show generated code. */
	private static final String CS_GENERATED = "CheckStyle-Generated ";
	/** A eclipse-configuration. */
	private final FormatterConfiguration mConfiguration;

	private final IProject mProject;

	/**
	 * Constructor to create a new instance of class FormatterConfigWriter.
	 *
	 * @param project
	 *            the project whose formatter settings should be written
	 * @param settings
	 *            A eclipse-configuration.
	 */
	public FormatterConfigWriter(IProject project, final FormatterConfiguration settings) {
		mConfiguration = settings;
		mProject = project;

		writeSettings();
	}

	/**
	 * Method for persisting all settings to files.
	 */
	private void writeSettings() {
		writeCleanupSettings(mConfiguration.getCleanupSettings());
		writeFormatterSettings(mConfiguration.getFormatterSettings());
	}

	/**
	 * Method for writing all cleanup settings to disc.
	 *
	 * @param settings
	 *            All the settings.
	 */
	private void writeCleanupSettings(final Map<String, String> settings) {
		final IFile settingsFile = mProject.getFile(mProject.getName() + "-cs-cleanup.xml");
		try (InputStream stream =
				XmlProfileWriter.writeCleanupProfileToStream(CS_GENERATED + mProject.getName(), settings)) {
			createOrUpdateFile(settingsFile, stream);
		} catch (CoreException | TransformerException | ParserConfigurationException | IOException e) {
			LOGGER.warn("Error saving cleanup profile", e);
		}
	}

	/**
	 * Method for writing all formatter settings to disc.
	 *
	 * @param settings
	 *            All the settings.
	 */
	private void writeFormatterSettings(final Map<String, String> settings) {
		final IFile settingsFile = mProject.getFile(mProject.getName() + "-cs-formatter.xml");
		try (InputStream stream =
				XmlProfileWriter.writeFormatterProfileToStream(CS_GENERATED + mProject.getName(), settings)) {
			createOrUpdateFile(settingsFile, stream);
		} catch (CoreException | TransformerException | ParserConfigurationException | IOException e) {
			LOGGER.warn("Error saving formatter profile", e);
		}
	}

	private static void createOrUpdateFile(IFile settingsFile, InputStream stream) throws CoreException {
		if (settingsFile.exists()) {
			settingsFile.setContents(stream, true, false, null);
		} else {
			settingsFile.create(stream, true, null);
		}
	}

}