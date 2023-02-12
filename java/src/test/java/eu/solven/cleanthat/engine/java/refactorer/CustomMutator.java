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
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Set;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This is used to test the inclusion of a custom {@link IMutator} (e.g. for a third-party jar)
 * 
 * @author Benoit Lacelle
 *
 */
public class CustomMutator implements IMutator {

	@Override
	public Set<String> getIds() {
		return Set.of("MyCustomMutator");
	}

	@Override
	public boolean walkNode(Node pre) {
		return false;
	}

}
