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
package eu.solven.cleanthat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.pepper.collection.PepperMapHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.io.ClassPathResource;

/**
 * Helps providing information about Git through a REST api. It implements {@link BeanFactoryPostProcessor} to ensure
 * being loaded very early, in order to log early during startup.
 * 
 * @author Benoit Lacelle
 *
 */
public class GitService implements IGitService, InitializingBean {
	protected static final Logger LOGGER = LoggerFactory.getLogger(GitService.class);

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
		ClassPathResource resource = new ClassPathResource("/git.json");
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
		return PepperMapHelper.getRequiredString(properties, KEY_GIT_COMMIT_ID);
	}
}
