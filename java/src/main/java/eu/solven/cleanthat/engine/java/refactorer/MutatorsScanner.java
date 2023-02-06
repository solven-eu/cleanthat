/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.engine.java.refactorer;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans dynamically for available rules
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection
public class MutatorsScanner {
	private static final String SUFFIX_DOT_CLASS = ".class";
	private static final Logger LOGGER = LoggerFactory.getLogger(MutatorsScanner.class);

	public List<IMutator> getMutators() {
		String packageName = "eu.solven.cleanthat.engine.java.refactorer.mutators";
		Class<?>[] classes;
		try {
			classes = getClasses(packageName);
		} catch (ClassNotFoundException | IOException e) {
			LOGGER.error("Issue loading mutators from {}", packageName, e);
			return Collections.emptyList();
		}

		return Stream.of(classes).filter(c -> IMutator.class.isAssignableFrom(c)).map(c -> {
			try {
				return c.asSubclass(IMutator.class).getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				LOGGER.error("Issue with {}", c, e);
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName
	 *            The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static Class<?>[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		List<Class<?>> classes = new ArrayList<>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes.toArray(new Class[0]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			} else if (file.getName().endsWith(SUFFIX_DOT_CLASS)) {
				classes.add(Class.forName(packageName + '.'
						+ file.getName().substring(0, file.getName().length() - SUFFIX_DOT_CLASS.length())));
			}
		}
		return classes;
	}
}
