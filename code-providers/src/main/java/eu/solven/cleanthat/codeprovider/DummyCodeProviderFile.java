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
package eu.solven.cleanthat.codeprovider;

import java.nio.file.Path;

/**
 * Simply wraps an {@link Object}
 * 
 * @author Benoit Lacelle
 *
 */
public class DummyCodeProviderFile implements ICodeProviderFile {
	private final Path path;
	private final Object raw;

	/**
	 * 
	 * @param path
	 *            path of the file, consider '/' is the root of the repository
	 * @param raw
	 */
	public DummyCodeProviderFile(Path path, Object raw) {
		if (raw instanceof DummyCodeProviderFile) {
			throw new IllegalArgumentException("input can not be an instance of " + this.getClass());
		}

		String rawPath = path.toString();
		if (!rawPath.startsWith("/")) {
			throw new IllegalArgumentException("Invalid path: " + rawPath + " (missing '/' at the beginning)");
		} else if (rawPath.startsWith("//")) {
			throw new IllegalArgumentException("Invalid path: " + rawPath + " ('//' at the beginning)");
		}

		this.path = path;
		this.raw = raw;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public Object getRaw() {
		return raw;
	}
}
