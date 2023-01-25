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
package eu.solven.cleanthat.engine.java.eclipse.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.PathAndContent;

/**
 * Helps computing a difference score between code source
 * 
 * @author blacelle
 *
 */
public class CodeDiffHelper {

	// Compute the diff can be expensive. However, we expect to encounter many times
	// files formatted exactly the same
	// way
	protected final Cache<List<String>, Long> cache = CacheBuilder.newBuilder().build();

	protected long computeDiffScore(ILintFixer formatter, Collection<String> contents) {
		return contents.parallelStream().mapToLong(content -> {
			try {
				return computeDiffScore(formatter, content);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).sum();
	}

	/**
	 * 
	 * @param lintFixer
	 * @param pathAsString
	 * @return a score indicating how much this formatter impacts given content. If 0, the formatter has no impacts. A
	 *         higher score means a bigger difference
	 * @throws IOException
	 */
	protected long computeDiffScore(ILintFixer lintFixer, String content) throws IOException {
		String formatted = lintFixer.doFormat(new PathAndContent(Paths.get("fake"), content), LineEnding.KEEP);

		if (formatted == null) {
			// It means something failed while formatting
			return Long.MAX_VALUE;
		}

		long deltaDiff;
		try {
			deltaDiff = cache.get(Arrays.asList(content, formatted), () -> deltaDiff(content, formatted));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return deltaDiff;
	}

	public long deltaDiff(String pathAsString, String formatted) {
		List<String> originalRows = Arrays.asList(pathAsString.split("[\r\n]+"));
		List<String> formattedRows = Arrays.asList(formatted.split("[\r\n]+"));
		Patch<String> diff = DiffUtils.diff(originalRows, formattedRows);
		List<String> patchApplied;
		try {
			patchApplied = diff.applyTo(originalRows);
		} catch (PatchFailedException e) {
			throw new RuntimeException(e);
		}
		if (!formattedRows.equals(patchApplied)) {
			throw new IllegalArgumentException("Issue computing the diff?");
		}
		long deltaDiff = diff.getDeltas().stream().mapToLong(d -> {
			if (d.getType() == DeltaType.EQUAL) {
				return 0L;
			}
			// We count the number of impacted characters
			List<String> sourceLines = d.getSource().getLines();
			List<String> targetLines = d.getTarget().getLines();

			if (sourceLines.size() == 1 && targetLines.size() == 1) {
				String sourceLine = sourceLines.get(0);
				String targetLine = targetLines.get(0);
				// int common = new LongestCommonSubsequence().apply(sourceLine, targetLine);
				// The diff is the longest difference between the 2 lines
				// return Math.max(sourceLine.length(), targetLine.length()) - common;

				return LevenshteinDistance.getDefaultInstance().apply(sourceLine, targetLine);
			} else {
				long sourceSize = sourceLines.stream().mapToLong(String::length).sum();
				long targetSize = targetLines.stream().mapToLong(String::length).sum();
				// Given a diff, we consider the biggest square between the source and the
				// target
				return Math.max(sourceSize, targetSize);
			}
		}).sum();
		return deltaDiff;
	}
}
