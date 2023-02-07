/*
 * Copyright 2009-2023 Benoit Lacelle - SOLVEN
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing all configurations for a eclipse-formatter-profile.
 * 
 * @author Lukas Frena
 */
public class FormatterConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(FormatterConfiguration.class);

	/** Map containing all eclipse editor-settings. */
	private final Map<String, String> mCleanupSettings = new HashMap<>();

	/** Map containing all eclipse formatter-settings. */
	private final Map<String, String> mFormatterSettings = new HashMap<>();

	/**
	 * Method for adding a new global setting.
	 * 
	 * @param setting
	 *            The global setting.
	 * @param val
	 *            The value of this setting.
	 */
	public void addCleanupSetting(final String setting, final String val) {
		mCleanupSettings.put(setting, val);
	}

	/**
	 * Method for adding a new local setting.
	 * 
	 * @param setting
	 *            The local setting.
	 * @param val
	 *            The value of this setting.
	 */
	public void addFormatterSetting(final String setting, final String val) {
		mFormatterSettings.put(setting, val);
	}

	/**
	 * Method for returning the stored global Settings.
	 * 
	 * @return Returns the global settings.
	 */
	public Map<String, String> getCleanupSettings() {
		return mCleanupSettings;
	}

	/**
	 * Method for returning the stored local Settings.
	 * 
	 * @return Returns the local settings.
	 */
	public Map<String, String> getFormatterSettings() {
		return mFormatterSettings;
	}

	/**
	 * Method for adding new configuration parameters.
	 * 
	 * @param settings
	 *            A eclipse-formatter-configuration.
	 */
	public void addConfiguration(final FormatterConfiguration settings) {
		// add local settings
		final Map<String, String> localSettings = settings.getFormatterSettings();
		final Collection<String> localKeys = localSettings.keySet();
		final Iterator<String> localIt = localKeys.iterator();
		String local;
		while (localIt.hasNext()) {
			local = localIt.next();
			if (mFormatterSettings.containsKey(local)
					&& !mFormatterSettings.get(local).equals(localSettings.get(local))) {
				LOGGER.debug("already containing local rule {} with other attributes, it gets overwritten!", local);
			}
			addFormatterSetting(local, localSettings.get(local));
		}

		// add global settings
		final Map<String, String> globalSettings = settings.getCleanupSettings();
		final Collection<String> globalKeys = globalSettings.keySet();
		final Iterator<String> globalIt = globalKeys.iterator();
		String global;
		while (globalIt.hasNext()) {
			global = globalIt.next();
			if (mCleanupSettings.containsKey(global)
					&& !getCleanupSettings().get(global).equals(globalSettings.get(global))) {
				LOGGER.debug("already containing global rule {} with other attributes, it gets overwritten!", global);
			}
			addCleanupSetting(global, settings.getCleanupSettings().get(global));
		}
	}
}