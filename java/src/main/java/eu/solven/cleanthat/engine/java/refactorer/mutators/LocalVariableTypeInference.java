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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalReferences;

/**
 * Turns 'int i = 10;' into 'var i = 10'
 *
 * @author Benoit Lacelle
 */
// https://github.com/openrewrite/rewrite/issues/1656
public class LocalVariableTypeInference extends AJavaParserMutator implements IMutator, IRuleExternalReferences {
	@Override
	public boolean isDraft() {
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_10;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-6212");
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof VariableDeclarationExpr)) {
			return false;
		}
		VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) node;

		if (variableDeclarationExpr.getVariables().size() >= 2) {
			return false;
		}

		VariableDeclarator singleVariableDeclaration = variableDeclarationExpr.getVariable(0);

		Type type = singleVariableDeclaration.getType();
		if (type.isVarType()) {
			return false;
		}

		// https://github.com/javaparser/javaparser/issues/3898
		// We can not change the Type, as it would fail in the case of Type with Diamond
		Expression initializer = singleVariableDeclaration.getInitializer().orElse(null);
		VariableDeclarator newVariableDeclarator =
				new VariableDeclarator(new VarType(), singleVariableDeclaration.getName(), initializer);
		return singleVariableDeclaration.replace(newVariableDeclarator);
	}
}
