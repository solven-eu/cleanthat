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

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * This is some code just to test which could like like a DSL to express an {@link IMutator}
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated
public class TestDsl {
	String renameVariable = "VariableDeclarationExpr.name=titi->VariableDeclarationExpr.name.set(var.name + '2')";
	String streamFilterFindAnyIsPresent = "node.instanceof=MethodCallExpr && node.MethodCallExpr.name=isPresent"

			+ "&& node.scope.instanceof=MethodCallExpr && node.scope.MethodCallExpr.name=findAny"
			+ "&& node.scope.scope.instanceof=MethodCallExpr && node.scope.scope.MethodCallExpr.name=isPresent"

			+ "&& node.scope.scope.resolveType=java.util.stream.Stream"

			+ "-> node.replace(MethodCallExpr(node.scope.scope, 'anyMatch', node.scope.arguments))";

	String indexOfToContains = " '<java.util.String:s1>.indexOf(<java.util.String:s2>) >= 0' into  's1.contains(s2)'";
	String indexOfToContains2 = "'<java.util.String:s1>.indexOf(<java.util.String:s2>) <  0' into '!s1.contains(s2)'";
}
