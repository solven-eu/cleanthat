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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * Helps building an {@link IMutator} which will process consecutive {@link Statement}s in a {@link BlockStmt}
 *
 * @author Benoit Lacelle
 */
public abstract class ARefactorConsecutiveStatements extends AJavaparserStmtMutator {

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isBlockStmt()) {
			return false;
		}

		var blockStmt = stmt.asBlockStmt();

		var result = false;
		for (var i = 0; i < blockStmt.getStatements().size() - 1; i++) {
			var currentStmt = blockStmt.getStatement(i);
			var nextStmt = blockStmt.getStatement(i + 1);

			result |= trySimplifyingStatements(currentStmt, nextStmt);
		}

		return result;
	}

	protected abstract boolean trySimplifyingStatements(Statement currentStmt, Statement nextStmt);

}
