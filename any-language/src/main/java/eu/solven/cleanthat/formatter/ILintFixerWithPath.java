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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Knows how to format a piece of code
 *
 * @author Benoit Lacelle
 */
public interface ILintFixerWithPath extends ILintFixer {
	/**
	 * This can be used as {@link Path} when one is necessary by the API while none is available.
	 */
	Path NO_PATH = Paths.get("cleanthat/path_is_not_available");

	String doFormat(PathAndContent pathAndContent) throws IOException;

	@Override
	default String doFormat(String content) throws IOException {
		var contentWithNoPath = new PathAndContent(NO_PATH, content);
		return doFormat(contentWithNoPath);
	}
}
