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
package eu.solven.cleanthat.engine.java.refactorer.meta;

import java.util.Objects;

/**
 * A {@link Comparable} for versions
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
public class VersionWrapper implements Comparable<VersionWrapper> {

	final String version;

	public final String get() {
		return this.version;
	}

	public VersionWrapper(String version) {
		if (version == null) {
			throw new IllegalArgumentException("Version can not be null");
		}
		if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
			throw new IllegalArgumentException("Invalid version format");
		}
		this.version = version;
	}

	@Override
	public int compareTo(VersionWrapper that) {
		if (that == null) {
			return 1;
		}
		String[] thisParts = this.get().split("\\.");
		String[] thatParts = that.get().split("\\.");
		int length = Math.max(thisParts.length, thatParts.length);
		for (int i = 0; i < length; i++) {
			int thisPart = getPart(thisParts, i);
			int thatPart = getPart(thatParts, i);
			if (thisPart < thatPart) {
				return -1;
			}
			if (thisPart > thatPart) {
				return 1;
			}
		}
		return 0;
	}

	private int getPart(String[] thisParts, int i) {
		int thisPart;
		if (i < thisParts.length) {
			thisPart = Integer.parseInt(thisParts[i]);
		} else {
			thisPart = 0;
		}
		return thisPart;
	}

	@Override
	public int hashCode() {
		return Objects.hash(version);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (this.getClass() != that.getClass()) {
			return false;
		}
		return this.compareTo((VersionWrapper) that) == 0;
	}

}