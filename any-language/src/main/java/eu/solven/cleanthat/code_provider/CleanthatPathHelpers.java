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
package eu.solven.cleanthat.code_provider;

import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Managing {@link Path} can be challenging from a security standpoint. As Cleanthat Robot infrastructure is shared, we
 * need to ensure one can not access any files from the FileSystem. ICodeProviders make a regular usage of {@link Path}
 * as they simulate each repository through a {@link FileSystem} which may be the default (real) FileSystem is some
 * cases.
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatPathHelpers {
	protected CleanthatPathHelpers() {
		// hidden
	}

	/**
	 * Ensure any path is a valid content path. At some point, we wanted a repository path to be absolute, considering
	 * '/' as the repositoy root. Doing so is OK with fake FileSystem. But in some edge-cases, we rely on the
	 * default/real {@link FileSystem}.
	 * 
	 * @param path
	 */
	// https://bugs.openjdk.org/browse/JDK-8262822
	public static void checkContentPath(Path path) {
		// if (!path.isAbsolute()) {
		// throw new IllegalStateException("We expect to receive only rooted path: " + path);
		// }

		if (path.isAbsolute()) {
			throw new IllegalArgumentException("Should be relative: " + path);
		} else if (path.getRoot() != null) {
			throw new IllegalArgumentException("Should not have a root: " + path);
		}
	}

	/**
	 * Converts the given path string to a {@code Path} and resolves it against this {@code Path}, throwing an exception
	 * if the resulting path is not a child of this path. The path is resolved in exactly the manner specified by the
	 * {@link #resolveChild(Path)} method.
	 *
	 * @param child
	 *            the path to resolve against this path
	 * @return the resulting path
	 * @throws InvalidPathException
	 *             if the path string cannot be converted to a Path
	 * @throws IllegalArgumentException
	 *             if the other path does not meet any of the requirements for being a child path
	 * @see {@link #resolveDirectChild(String)}
	 */
	// https://bugs.openjdk.org/browse/JDK-8262822
	public static Path resolveChild(Path parent, String child) throws InvalidPathException, IllegalArgumentException {
		return resolveChild(parent, parent.getFileSystem().getPath(child));
	}

	/**
	 * Resolves the given path against this {@code Path}, throwing an exception if the resulting path is not a child of
	 * this path. The path is resolved in exactly the manner specified by the {@link #resolve(Path) resolve} method.
	 * Afterwards it is verified that the result is a child of this path. The following requirements have to be met,
	 * otherwise an {@link IllegalArgumentException} is thrown:
	 * <ul>
	 * <li>the other path must not be {@link #isAbsolute() absolute}
	 * <li>the other path must not have a {@link #getRoot() root}
	 * <li>the other path must not contain any name elements which allow navigating the element hierarchy; <br>
	 * the precise definition of this is implementation dependent, but for example "{@code .}" and "{@code ..}",
	 * indicating the current and parent directory for some file systems, will not be allowed
	 * <li>the result path must be a true child (or grandchild, ...) of this path, it must not be equal to this path
	 * </ul>
	 *
	 * @apiNote This method is intended for cases where a path from an untrusted source has to be resolved. Note however
	 *          that it is in general <em>not recommended</em> to use untrusted file paths for file system access. This
	 *          method might not detect reserved file names or too long file names.
	 *
	 * @implSpec The default implementation of this method performs detection of name elements allowing element
	 *           hierarchy navigation through usage of {@link #normalize()}. Subtypes should override this method if
	 *           they can provide a better implementation.
	 *
	 * @param parent
	 *            the parent of the child
	 * @param child
	 *            the path to resolve against this path
	 * @return the resulting path
	 * @throws IllegalArgumentException
	 *             if the other path does not meet any of the requirements for being a child path
	 * @see #resolveDirectChild(Path)
	 */
	// https://bugs.openjdk.org/browse/JDK-8262822
	public static Path resolveChild(Path parent, Path child) throws IllegalArgumentException {
		/*
		 * Don't permit any root: - If different root, result would not be child of this -> have to throw exception - If
		 * same root, would allow an adversary to know that their provided root was guessed 'correctly' because no
		 * exception is thrown
		 */
		if (child.getRoot() != null) {
			throw new IllegalArgumentException("Child path has root");
		}
		// Don't permit absolute because when resolved against this, would
		// discard path of this
		else if (child.isAbsolute()) {
			throw new IllegalArgumentException("Child path is absolute");
		}

		/*
		 * Resolve path against dummy to detect `.` or `..`; cannot resolve against `this` because if this is empty
		 * path, `this.resolve(other).normalize()` would not get rid of leading `..`
		 *
		 * Additionally don't allow any `.` or `..` at all, even if they represent a child path after resolution, e.g.
		 * "a/b".resolve("../b/c") Because even the fact that the result is valid gives an adversary information they
		 * should not have; e.g. here they would know that the parent is `b` because for resolve("../x/c") an exception
		 * would have been thrown
		 */
		// TODO: Maybe this should be relaxed to allow `.` and `..` as long
		// as they only affect the to-be-resolved child but not the
		// parent; could be implemented by only checking that
		// otherDummyNormalized starts with dummy followed by first
		// name element of `child.normalize()` (if child has no name
		// elements it is not allowed either because it is not a true
		// child)
		Path dummy = parent.getFileSystem().getPath("dummy");
		Path otherDummyNormalized = dummy.resolve(child).normalize();
		// Check if `normalize()` removed any elements
		if (otherDummyNormalized.getNameCount() != 1 + child.getNameCount()) {
			throw new IllegalArgumentException("Invalid child path");
		}
		// Verify that normalization did not change any name elements
		if (!otherDummyNormalized.startsWith(dummy) || !otherDummyNormalized.endsWith(child)) {
			throw new IllegalArgumentException("Invalid child path");
		}

		Path result = parent.resolve(child);

		Path resultNormalized = result.normalize();
		Path thisNormalized = parent.normalize();
		int minDiff;
		if (isEmptyPath(thisNormalized)) {
			minDiff = 0;
			// Detect case "".resolve("")
			if (isEmptyPath(resultNormalized)) {
				throw new IllegalArgumentException("Invalid child path");
			}
		}
		// Only perform further checks when `this` is not empty path "" because for "".resolve(other)
		// startsWith(...) will be false
		else {
			minDiff = 1;
			// Sanity check; probably already covered by normalization checks above
			if (!resultNormalized.startsWith(thisNormalized)) {
				throw new IllegalArgumentException("Invalid child path");
			}
		}

		// Verify that result is actually a 'true' child
		if (resultNormalized.getNameCount() - thisNormalized.getNameCount() < minDiff) {
			throw new IllegalArgumentException("Invalid child path");
		}

		return result;
	}

	/**
	 * Returns if {@code p} is "".
	 */
	private static boolean isEmptyPath(Path p) {
		return p.getRoot() == null && p.getNameCount() == 1 && p.getName(0).toString().isEmpty();
	}

	public static Path makeContentPath(Path repositoryRoot, String pathString) {
		// Safe resolution of the content path
		Path contentPath = resolveChild(repositoryRoot, pathString);

		// Check the contentPath is really safe
		checkContentPath(contentPath);

		return contentPath;
	}

	public static String makeContentRawPath(Path repositoryRoot, Path contentPath) {
		Path childrenAbsolutePath = resolveChild(repositoryRoot, contentPath);

		return repositoryRoot.relativize(childrenAbsolutePath).toString();
	}
}
