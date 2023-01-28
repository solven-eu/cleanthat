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
package eu.solven.cleanthat.engine.java.refactorer;

import com.github.javaparser.ast.Node;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps preparing rules
 *
 * @author Benoit Lacelle
 */
public abstract class ATodoJavaParserRule extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATodoJavaParserRule.class);

	@Override
	public boolean isProductionReady() {
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_LATEST;
	}

	@Override
	public boolean walkNode(Node tree) {
		LOGGER.debug("TODO");
		return false;
	}
}
