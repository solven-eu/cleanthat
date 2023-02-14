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
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigConstants;

/**
 * Helpers working for any {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeProviderHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderHelpers.class);

	protected Collection<ObjectMapper> objectMappers;

	public CodeProviderHelpers(Collection<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
	}

	// TODO Get the merged configuration head -> base
	// Could be done by relying on the merge commit given a PR
	// It will enable cleaning a PR given the configuration of the base branch
	public Optional<Map<String, ?>> unsafeConfig(ICodeProvider codeProvider) {
		Optional<Map.Entry<String, String>> optPathAndContent;
		optPathAndContent = ICleanthatConfigConstants.PATHES_CLEANTHAT.stream().map(p -> {
			try {
				Path resolvedPath = CleanthatPathHelpers.makeContentPath(codeProvider.getRepositoryRoot(), p);
				return codeProvider.loadContentForPath(resolvedPath).map(content -> Maps.immutableEntry(p, content));
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

	public static Optional<File> pathToConfig(Path localFolder) {
		return ICleanthatConfigConstants.PATHES_CLEANTHAT.stream().map(s -> {
			File file = CleanthatPathHelpers.resolveChild(localFolder, s).toFile();
			return file;
		}).filter(File::exists).findAny();
	}

	public static Path getRoot(Path path) {
		return getRoot(path.getFileSystem());
	}

	public static Path getRoot(FileSystem fs) {
		return fs.getPath(fs.getSeparator());
	}

}
