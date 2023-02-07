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
package eu.solven.cleanthat.formatter;

import com.google.common.base.Suppliers;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Couple a {@link Path} (which may not be based on FileSystems.default()) and its content. The content is fetched
 * lazily. Once it is fetched, it is cached.
 * 
 * @author Benoit Lacelle
 *
 */
public class PathAndContent {
	final Path path;
	final Supplier<String> contentSupplier;

	public PathAndContent(Path path, Supplier<String> contentSupplier) {
		this.path = path;
		this.contentSupplier = Suppliers.memoize(contentSupplier::get);
	}

	public PathAndContent(Path path, String content) {
		this(path, () -> content);
	}

	public Path getPath() {
		return path;
	}

	public String getContent() {
		return contentSupplier.get();
	}

	public PathAndContent withContent(String newContent) {
		return new PathAndContent(getPath(), () -> newContent);
	}
}
