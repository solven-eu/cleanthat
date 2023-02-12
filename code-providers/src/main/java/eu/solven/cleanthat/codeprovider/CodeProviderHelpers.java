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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import eu.solven.cleanthat.config.ConfigHelpers;

/**
 * Helpers working for any {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderHelpers {
	// We consider only paths with Unix-like path separator
	public static final String PATH_SEPARATOR = "/";

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderHelpers.class);

	public static final String FILENAME_CLEANTHAT_FOLDER = ".cleanthat";

	public static final String FILENAME_CLEANTHAT_YAML = "cleanthat.yaml";
	public static final String FILENAME_CLEANTHAT_YML = "cleanthat.yml";
	@Deprecated
	public static final String FILENAME_CLEANTHAT_JSON = "cleanthat.json";

	public static final List<String> PATHES_CLEANTHAT =
			ImmutableList.of(PATH_SEPARATOR + FILENAME_CLEANTHAT_FOLDER + PATH_SEPARATOR + FILENAME_CLEANTHAT_YAML,
					PATH_SEPARATOR + FILENAME_CLEANTHAT_FOLDER + PATH_SEPARATOR + FILENAME_CLEANTHAT_YML,
					PATH_SEPARATOR + FILENAME_CLEANTHAT_YAML,
					PATH_SEPARATOR + FILENAME_CLEANTHAT_YML,
					PATH_SEPARATOR + FILENAME_CLEANTHAT_JSON);

	// public static final String PATH_CLEANTHAT_JSON = "/" + FILENAME_CLEANTHAT_JSON;

	protected Collection<ObjectMapper> objectMappers;

	public CodeProviderHelpers(Collection<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
	}

	// TODO Get the merged configuration head -> base
	// Could be done by relying on the merge commit given a PR
	// It will enable cleaning a PR given the configuration of the base branch
	public Optional<Map<String, ?>> unsafeConfig(ICodeProvider codeProvider) {
		Optional<Map.Entry<String, String>> optPathAndContent;
		optPathAndContent = PATHES_CLEANTHAT.stream().map(p -> {
			try {
				return codeProvider.loadContentForPath(p).map(content -> Maps.immutableEntry(p, content));
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}).flatMap(Optional::stream).findFirst();

		if (optPathAndContent.isEmpty()) {
			return Optional.empty();
		}

		ObjectMapper objectMapper;
		Map.Entry<String, String> pathAndContent = optPathAndContent.get();

		String sha1ForLog;
		if (codeProvider instanceof IGitSha1CodeProvider) {
			sha1ForLog = ((IGitSha1CodeProvider) codeProvider).getSha1();
		} else {
			sha1ForLog = codeProvider.getClass().getName();
		}
		LOGGER.info("Loaded config from {} from sha1={}", pathAndContent.getKey(), sha1ForLog);
		if (pathAndContent.getKey().endsWith(".json")) {
			objectMapper = ConfigHelpers.getJson(objectMappers);
		} else if (pathAndContent.getKey().endsWith(".yml") || pathAndContent.getKey().endsWith(".yaml")) {
			objectMapper = ConfigHelpers.getYaml(objectMappers);
		} else {
			throw new IllegalArgumentException("Not managed extention: " + pathAndContent.getKey());
		}

		return optPathAndContent.map(content -> {
			try {
				return objectMapper.readValue(content.getValue(), Map.class);
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Invalid config (json vs yaml?)", e);
			}
		});
	}

	public static File pathToConfig(Path localFolder) {
		return CodeProviderHelpers.PATHES_CLEANTHAT.stream().map(s -> {
			String prefix = PATH_SEPARATOR;
			if (!s.startsWith(prefix)) {
				throw new IllegalArgumentException("We expect cleanpath config pathes to start with '" + prefix + "'");
			}
			File file = localFolder.resolve(s.substring(prefix.length())).toFile();
			return file;
		})
				.filter(File::exists)
				.findAny()
				.orElseThrow(() -> new IllegalStateException(
						"No configuration at pathes: " + CodeProviderHelpers.PATHES_CLEANTHAT));
	}

	public static Path getRoot(Path path) {
		return getRoot(path.getFileSystem());
	}

	public static Path getRoot(FileSystem fs) {
		return fs.getPath(fs.getSeparator());
	}

}
