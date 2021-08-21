//============================================================================
//
// Copyright (C) 2009 Lukas Frena
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//============================================================================

package eu.solven.cleanthat.language.java.eclipse.checkstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for writing a new eclipse-configuration-file. Gets used by class Transformer. Two eclipse-formatter-profile
 * files gets written to the project root.
 *
 * @author Alexandros Karypidis
 * @author Lukas Frena
 * @author Lars Ködderitzsch
 */
// https://github.com/checkstyle/eclipse-cs/blob/master/net.sf.eclipsecs.core/src/net/sf/eclipsecs/core/transformer/FormatterConfigWriter.java
public class FormatterConfigWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatterConfigWriter.class);

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