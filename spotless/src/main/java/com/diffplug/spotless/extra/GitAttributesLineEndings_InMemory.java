/*
 * Copyright 2016-2023 Benoit Lacelle - SOLVEN
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
package com.diffplug.spotless.extra;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.internal.storage.dfs.DfsConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EOL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LineEnding.Policy;
import com.google.common.base.Strings;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.spotless.pojo.SpotlessGitProperties;

/**
 * Uses <a href="https://git-scm.com/docs/gitattributes">.gitattributes</a> to determine the appropriate line ending.
 * Falls back to the {@code core.eol} and {@code core.autocrlf} properties in the git config if there are no applicable
 * git attributes, then finally falls back to the platform native.
 * 
 * @see GitAttributesLineEndings
 * @author Benoit Lacelle
 */
// This differs from original GitAttributesLineEndings as it rely on a .gitattributes as a String, and no not try
// consider any system git configuration
@SuppressWarnings("checkstyle:TypeName")
public final class GitAttributesLineEndings_InMemory {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitAttributesLineEndings_InMemory.class);

	// prevent direct instantiation
	private GitAttributesLineEndings_InMemory() {
	}

	/**
	 * Creates a line-endings policy whose serialized state is relativized against projectDir, at the cost of eagerly
	 * evaluating the line-ending state of every target file when the policy is checked for equality with another
	 * policy.
	 */
	public static Policy create(ICodeProvider codeProvider,
			SpotlessGitProperties git,
			Path projectDir,
			Supplier<Iterable<Path>> toFormat) {
		return new RelocatablePolicy_InMemory(codeProvider, git, projectDir, toFormat);
	}

	static class RelocatablePolicy_InMemory extends LazyForwardingEquality<CachedEndings_InMemory>
			implements LineEnding.Policy {
		private static final long serialVersionUID = 5868522122123693015L;

		transient ICodeProvider codeProvider;
		transient SpotlessGitProperties git;
		transient Path projectDir;
		transient Supplier<Iterable<Path>> toFormat;

		RelocatablePolicy_InMemory(ICodeProvider codeProvider,
				SpotlessGitProperties git,
				Path projectDir,
				Supplier<Iterable<Path>> toFormat) {
			this.codeProvider = Objects.requireNonNull(codeProvider, "codeProvider");
			this.git = Objects.requireNonNull(git, "git");
			this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
			this.toFormat = Objects.requireNonNull(toFormat, "toFormat");
		}

		@SuppressWarnings("PMD.NullAssignment")
		@Override
		protected CachedEndings_InMemory calculateState() throws Exception {
			var runtime = new RuntimeInit_InMemory(codeProvider, git, projectDir).atRuntime();
			// LazyForwardingEquality guarantees that this will only be called once, and keeping toFormat
			// causes a memory leak, see https://github.com/diffplug/spotless/issues/1194
			var state = new CachedEndings_InMemory(projectDir, runtime, toFormat.get());
			projectDir = null;
			toFormat = null;
			return state;
		}

		@Override
		public String getEndingFor(File file) {
			return state().endingFor(file.toPath());
		}
	}

	// @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	@SuppressWarnings("checkstyle:AvoidInlineConditionals")
	static class CachedEndings_InMemory implements Serializable {
		private static final long serialVersionUID = -2534772773057900619L;

		/** this is transient, to simulate PathSensitive.RELATIVE */
		// CleanThat: we removed the transient keyword as the purpose is unclear, and Spotbugs was complaining
		final String rootDir;
		/** the line ending used for most files */
		final String defaultEnding;
		/** any exceptions to that default, in terms of relative path from rootDir */
		@SuppressWarnings("PMD.LinguisticNaming")
		final ConcurrentRadixTree<String> hasNonDefaultEnding =
				new ConcurrentRadixTree<>(new DefaultCharSequenceNodeFactory());

		CachedEndings_InMemory(Path projectDir, Runtime_InMemory runtime, Iterable<Path> toFormat) {
			var rootPath = FileSignature.pathNativeToUnix(projectDir.toAbsolutePath().toString());
			// is this a bug in original implementation? (.equals instead of .endsWith)
			rootDir = "/".equals(rootPath) ? rootPath : rootPath + "/";
			defaultEnding = runtime.defaultEnding;
			toFormat.forEach(file -> {
				var ending = runtime.getEndingFor(file);
				if (!ending.equals(defaultEnding)) {
					var absPath = FileSignature.pathNativeToUnix(file.toAbsolutePath().toString());
					var subPath = FileSignature.subpath(rootDir, absPath);
					hasNonDefaultEnding.put(subPath, ending);
				}
			});
		}

		/** Returns the line ending appropriate for the given file. */
		public String endingFor(Path file) {
			var absPath = FileSignature.pathNativeToUnix(file.toAbsolutePath().toString());
			var subpath = FileSignature.subpath(rootDir, absPath);
			String ending = hasNonDefaultEnding.getValueForExactKey(subpath);
			if (ending == null) {
				return defaultEnding;
			} else {
				return ending;
			}
		}
	}

	static class RuntimeInit_InMemory {
		final ICodeProvider codeProvider;
		final Config defaultConfig;

		final Path root;

		// @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
		RuntimeInit_InMemory(ICodeProvider codeProvider, SpotlessGitProperties git, Path root) {
			this.codeProvider = codeProvider;
			this.defaultConfig = new DfsConfig();
			if (git.getCoreAutocrlf() != null) {
				// see org.eclipse.jgit.lib.CoreConfig.AutoCRLF
				this.defaultConfig.setString(ConfigConstants.CONFIG_CORE_SECTION,
						null,
						ConfigConstants.CONFIG_KEY_AUTOCRLF,
						git.getCoreAutocrlf());
			}
			if (git.getCoreEol() != null) {
				// see org.eclipse.jgit.lib.CoreConfig.EOL
				this.defaultConfig.setString(ConfigConstants.CONFIG_CORE_SECTION,
						null,
						ConfigConstants.CONFIG_KEY_EOL,
						git.getCoreEol());
			}

			this.root = root;
		}

		private Runtime_InMemory atRuntime() {
			return new Runtime_InMemory(codeProvider,
					// parseRules(gitAttributes.get()),
					root,
					defaultConfig

			// ,parseRules(globalAttributesFile)
			);
		}
	}

	/** https://github.com/git/git/blob/1fe8f2cf461179c41f64efbd1dc0a9fb3b7a0fb1/Documentation/gitattributes.txt */
	static final class Runtime_InMemory {

		private static final String KEY_EOL = "eol";

		final @Nullable Path workTree;

		/** Cache of local .gitattributes files. */
		final AttributesCache cache;

		/**
		 * Default line ending, determined by a manually configured {@link Config}.
		 */
		final String defaultEnding;

		private Runtime_InMemory(ICodeProvider codeProvider, @Nullable Path workTree, Config config) {
			this.workTree = workTree;
			this.defaultEnding = findDefaultLineEnding(config).str();

			cache = new AttributesCache(codeProvider);
		}

		public String getEndingFor(Path file) {
			// handle the local .gitattributes (if any)
			var localResult = cache.valueFor(file, KEY_EOL);
			if (localResult != null) {
				return convertEolToLineEnding(localResult, file);
			}
			// if all else fails, use the default value
			return defaultEnding;
		}

		private static String convertEolToLineEnding(String eol, Path file) {
			switch (eol.toLowerCase(Locale.ROOT)) {
			case "lf":
				return LineEnding.UNIX.str();
			case "crlf":
				return LineEnding.WINDOWS.str();
			default:
				LOGGER.error(".gitattributes file has unspecified eol value: " + eol
						+ " for "
						+ file
						+ ", defaulting to platform native");
				return LineEnding.PLATFORM_NATIVE.str();
			}
		}

		private LineEnding findDefaultLineEnding(Config config) {
			// handle core.autocrlf, whose values "true" and "input" override core.eol
			AutoCRLF autoCRLF = config.getEnum(ConfigConstants.CONFIG_CORE_SECTION,
					null,
					ConfigConstants.CONFIG_KEY_AUTOCRLF,
					AutoCRLF.FALSE);
			if (autoCRLF == AutoCRLF.TRUE) {
				// autocrlf=true converts CRLF->LF during commit
				// and converts LF->CRLF during checkout
				// so CRLF is the default line ending
				return LineEnding.WINDOWS;
			} else if (autoCRLF == AutoCRLF.INPUT) {
				// autocrlf=input converts CRLF->LF during commit
				// and does no conversion during checkout
				// mostly used on Unix, so LF is the default encoding
				return LineEnding.UNIX;
			} else if (autoCRLF == AutoCRLF.FALSE) {
				// handle core.eol
				EOL eol = config
						.getEnum(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EOL, EOL.NATIVE);
				return fromEol(eol);
			} else {
				throw new IllegalStateException("Unexpected value for autoCRLF " + autoCRLF);
			}
		}

		/** Creates a LineEnding from an EOL. */
		private static LineEnding fromEol(EOL eol) {
			// @formatter:off
			switch (eol) {
			case CRLF:
				return LineEnding.WINDOWS;
			case LF:
				return LineEnding.UNIX;
			case NATIVE:
				return LineEnding.PLATFORM_NATIVE;
			default:
				throw new IllegalArgumentException("Unknown eol " + eol);
			}
			// @formatter:on
		}
	}

	/** Parses and caches .gitattributes files. */
	static class AttributesCache {

		final ICodeProvider codeProvider;

		final Map<Path, List<AttributesRule>> rulesAtPath = new HashMap<>();

		AttributesCache(ICodeProvider codeProvider) {
			this.codeProvider = codeProvider;
		}

		/** Returns a value if there is one, or unspecified if there isn't. */
		public @Nullable String valueFor(Path file, String key) {
			var pathBuilder = new StringBuilder(file.toAbsolutePath().toString().length());

			// We assume the input path if always a file, hence not a directory
			var isDirectory = false;
			var parent = file.getParent();

			pathBuilder.append(file.getFileName());
			while (parent != null) {
				var path = pathBuilder.toString();

				String value = findAttributeInRules(path, isDirectory, key, getRulesForFolder(parent));
				if (value != null) {
					return value;
				}

				pathBuilder.insert(0, parent.getFileName() + "/");

				// We assume the parent of any Path (file or directory) is a directory
				isDirectory = true;
				parent = parent.getParent();
			}
			return null;
		}

		/** Returns the gitattributes rules for the given folder. */
		private List<AttributesRule> getRulesForFolder(Path folder) {
			return rulesAtPath.computeIfAbsent(folder, f -> {
				Path gitAttributesPath = f.resolve(Constants.DOT_GIT_ATTRIBUTES);
				try {
					return parseRules(codeProvider, gitAttributesPath);
				} catch (IOException e) {
					LOGGER.warn("Issue processing {}", gitAttributesPath);
					return Collections.emptyList();
				}
			});
		}
	}

	private static List<AttributesRule> parseRules(ICodeProvider codeProvider, Path gitAttributesPath)
			throws IOException {
		Optional<String> gitAttributesContent = codeProvider.loadContentForPath(gitAttributesPath.toString());
		return gitAttributesContent.map(content -> parseRules(content)).orElse(Collections.emptyList());
	}

	/** Parses a list of rules from the given file, returning an empty list if the file doesn't exist. */
	private static List<AttributesRule> parseRules(String gitAttributes) {
		if (!Strings.isNullOrEmpty(gitAttributes)) {
			try (InputStream stream = new ByteArrayInputStream(gitAttributes.getBytes(StandardCharsets.UTF_8))) {
				AttributesNode parsed = new AttributesNode();
				parsed.parse(stream);
				return parsed.getRules();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return Collections.emptyList();
	}

	/** Parses an attribute value from a list of rules, returning null if there is no match for the given key. */
	private static @Nullable String findAttributeInRules(String subpath,
			boolean isFolder,
			String key,
			List<AttributesRule> rules) {
		String value = null;
		// later rules override earlier ones
		for (AttributesRule rule : rules) {
			if (rule.isMatch(subpath, isFolder)) {
				for (Attribute attribute : rule.getAttributes()) {
					if (attribute.getKey().equals(key)) {
						value = attribute.getValue();
					}
				}
			}
		}
		return value;
	}
}
