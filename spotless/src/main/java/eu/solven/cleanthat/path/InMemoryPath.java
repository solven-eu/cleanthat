/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.path;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class InMemoryPath implements Path {

	@Override
	public FileSystem getFileSystem() {
		// TODO Auto-generated method stub
		return new FileSystem() {

			@Override
			public Set<String> supportedFileAttributeViews() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public FileSystemProvider provider() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public WatchService newWatchService() throws IOException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isReadOnly() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isOpen() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public UserPrincipalLookupService getUserPrincipalLookupService() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getSeparator() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Iterable<Path> getRootDirectories() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public PathMatcher getPathMatcher(String syntaxAndPattern) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Path getPath(String first, String... more) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Iterable<FileStore> getFileStores() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public boolean isAbsolute() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path getRoot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNameCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Path getName(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean startsWith(Path other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean endsWith(Path other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path normalize() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path resolve(Path other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path relativize(Path other) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public URI toUri() {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Path toAbsolutePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		throw new UnsupportedOperationException("Irrelevant InMemory");
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		throw new UnsupportedOperationException("Irrelevant InMemory");
	}

	@Override
	public int compareTo(Path other) {
		throw new UnsupportedOperationException("TODO");
	}

}
