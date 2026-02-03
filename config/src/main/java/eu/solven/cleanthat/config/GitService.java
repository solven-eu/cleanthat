/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.mappath.MapPath;
import eu.solven.pepper.mappath.MapPathGet;
import lombok.extern.slf4j.Slf4j;

/**
 * Helps providing information about Git through a REST api. It implements {@link BeanFactoryPostProcessor} to ensure
 * being loaded very early, in order to log early during startup.
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class GitService implements IGitService, InitializingBean {

	private static final String KEY_GIT_COMMIT_ID = "git.commit.id";

	@Override
	public void afterPropertiesSet() throws IOException {
		getProperties().entrySet().forEach(e -> {
			if (KEY_GIT_COMMIT_ID.equals(e.getKey())) {
				LOGGER.info("Git info: {}", e);
			} else {
				LOGGER.debug("Git info: {}", e);
			}
		});
	}

	@Override
	public Map<String, ?> getProperties() throws IOException {
		var resource = new ClassPathResource("/git.json");
		if (!resource.exists()) {
			// https://github.com/solven-eu/mitrust-datasharing/issues/8871
			LOGGER.warn("We failed finding the resource: {}", resource.getPath());
			resource = new ClassPathResource("/git.fallback.json");
		}

		return new ObjectMapper().readValue(resource.getInputStream(), Map.class);
	}

	@Override
	public String getSha1() {
		Map<String, ?> properties;
		try {
			properties = getProperties();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return MapPathGet.getRequiredString(properties, KEY_GIT_COMMIT_ID);
	}

	public static String safeGetSha1() {
		var gitService = new GitService();
		try {
			gitService.afterPropertiesSet();
		} catch (IOException e) {
			LOGGER.warn("Issue fetching git.sha1", e);
			return "error";
		}
		return gitService.getSha1();
	}
}
