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

import java.util.List;

/**
 * Default and simple implementation of {@link ICodeWritingMetadata}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeWritingMetadata implements ICodeWritingMetadata {
	final List<String> comments;
	final List<String> labels;

	public CodeWritingMetadata(List<String> comments, List<String> labels) {
		this.comments = comments;
		this.labels = labels;
	}

	@Override
	public List<String> getComments() {
		return comments;
	}

	public List<String> getLabels() {
		return labels;
	}

	public static ICodeWritingMetadata empty() {
		return new CodeWritingMetadata(List.of(), List.of());
	}

}
