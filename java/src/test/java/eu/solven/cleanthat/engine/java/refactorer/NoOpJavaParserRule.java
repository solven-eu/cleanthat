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

import com.github.javaparser.ast.Node;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalUrls;

/**
 * This {@link AJavaParserMutator} does not modify the AST, but always report it as changed. It can be useful to
 * checkthe default behavior of JavaParser.
 *
 * @author Benoit Lacelle
 */
public class NoOpJavaParserRule extends AJavaParserMutator implements IMutator, IRuleExternalUrls {
	@Override
	public String getId() {
		return "NoOp";
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		// We return true to indicate we did modify the node, even through this is a no-op operator
		return true;
	}
}
