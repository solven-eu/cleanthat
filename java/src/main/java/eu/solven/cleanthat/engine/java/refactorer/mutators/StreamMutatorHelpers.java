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

import java.util.Optional;
import java.util.stream.Stream;

import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Helps building mutators around {@link Stream}
 * 
 * @author Benoit Lacelle
 *
 */
public class StreamMutatorHelpers {
	protected StreamMutatorHelpers() {
		// hidden
	}

	public static Optional<Statement> findSingleStatement(Statement maybeBlockorSingleStatement) {
		Statement singleStatement;

		if (maybeBlockorSingleStatement.isBlockStmt()) {
			var asBlockStmt = maybeBlockorSingleStatement.asBlockStmt();
			if (asBlockStmt.getStatements().size() == 1) {
				singleStatement = asBlockStmt.getStatement(0);
			} else {
				return Optional.empty();
			}
		} else {
			singleStatement = maybeBlockorSingleStatement;
		}
		return Optional.of(singleStatement);
	}

	public static Optional<IfStmt> findSingleIfThenStmt(ForEachStmt forEachStmt) {
		Optional<Statement> optSingleStatement = findSingleStatement(forEachStmt.getBody());
		if (optSingleStatement.isEmpty()) {
			return Optional.empty();
		}

		var singleStatement = optSingleStatement.get();

		if (!singleStatement.isIfStmt() || singleStatement.asIfStmt().getElseStmt().isPresent()) {
			return Optional.empty();
		}

		var ifStmt = singleStatement.asIfStmt();
		return Optional.of(ifStmt);
	}

}
