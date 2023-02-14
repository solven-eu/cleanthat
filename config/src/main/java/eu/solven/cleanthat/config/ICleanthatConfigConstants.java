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
package eu.solven.cleanthat.config;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Constants related to config files
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICleanthatConfigConstants {
	// We consider only paths with Unix-like path separator
	String PATH_SEPARATOR = "/";

	String FILENAME_CLEANTHAT_FOLDER = ".cleanthat";

	String FILENAME_CLEANTHAT_YAML = "cleanthat.yaml";
	String FILENAME_CLEANTHAT_YML = "cleanthat.yml";
	@Deprecated
	String FILENAME_CLEANTHAT_JSON = "cleanthat.json";

	// contentPathes are relative: do not lead with a '/'
	String DEFAULT_PATH_CLEANTHAT = FILENAME_CLEANTHAT_FOLDER + PATH_SEPARATOR + FILENAME_CLEANTHAT_YAML;

	List<String> PATHES_CLEANTHAT = ImmutableList.of(DEFAULT_PATH_CLEANTHAT,
			FILENAME_CLEANTHAT_FOLDER + PATH_SEPARATOR + FILENAME_CLEANTHAT_YML,
			FILENAME_CLEANTHAT_YAML,
			FILENAME_CLEANTHAT_YML,
			FILENAME_CLEANTHAT_JSON);

	// String PATH_CLEANTHAT_JSON = "/" + FILENAME_CLEANTHAT_JSON;
}
