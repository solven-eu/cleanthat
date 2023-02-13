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
package eu.solven.cleanthat.formatter;

import java.nio.file.Path;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import lombok.Data;

/**
 * The core components attached to a clean session. The cleaning session granularity is a full repository: it will
 * process all files, all all formatters, and a single {@link CleanthatRepositoryProperties}.
 * 
 * @author Benoit Lacelle
 *
 */
@Data
public class CleanthatSession {
	final Path repositoryRoot;
	final ICodeProvider codeProvider;
	final CleanthatRepositoryProperties repositoryProperties;

}
