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
package eu.solven.cleanthat.codeprovider.resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * Knows how to load resource given a URL, with a flexible format handling 'classpath:' to load from the classpath, and
 * 'repository:' to load from the repository.
 * 
 * For security reasons, this is limited to 'repository:' for customers/external usage.
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatUrlLoader {
	public static final String PREFIX_CLASSPATH_ECLIPSE = "classpath:/eclipse/";

	// Used to load from the repository
	@Deprecated
	public static final String PREFIX_CODE_LEGACY = "code:";
	public static final String PREFIX_CODE = "repository:";

	protected CleanthatUrlLoader() {
		// hidden
	}

	public static Resource loadUrl(ICodeProvider codeProvider, String configFile) {
		Resource resource;
		if (configFile.startsWith(PREFIX_CODE)) {
			// This is inspired by Spring 'classpath:'
			// Here, it is used to load files from current repository
			String path = configFile.substring(PREFIX_CODE.length());
			resource = loadFromRepository(codeProvider, path);
		} else if (configFile.startsWith(PREFIX_CODE_LEGACY)) {
			// This is inspired by Spring 'classpath:'
			// Here, it is used to load files from current repository
			String path = configFile.substring(PREFIX_CODE_LEGACY.length());
			resource = loadFromRepository(codeProvider, path);
		} else if (configFile.startsWith(PREFIX_CLASSPATH_ECLIPSE)) {
			resource = new DefaultResourceLoader().getResource(configFile);
		} else {
			// For security reasons, we do not take the risk to load 'file://' or internal 'url://' not even
			// 'classpath:' which may enable returning internal '.yml' or '.properties'
			// LATER: We may allow loading from white-listed domains (e.g. public Github repositories, etc)
			throw new IllegalArgumentException("You must prefix with 'repository:'. Was '" + configFile + "'");
			// resource = new DefaultResourceLoader().getResource(javaConfigFile);
		}

		return resource;
	}

	public static Resource loadFromRepository(ICodeProvider codeProvider, String path) {
		if (path.startsWith("/")) {
			// It is legit to consider a path absolute, with '/' representing the root of the repository
			path = path.substring("/".length());
		}

		Optional<String> optContent;
		try {
			optContent = codeProvider.loadContentForPath(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (optContent.isEmpty()) {
			throw new IllegalStateException("There is no content at: " + path + " (provider: " + codeProvider + ")");
		}
		return new ByteArrayResource(optContent.get().getBytes(StandardCharsets.UTF_8), path);
	}

}
