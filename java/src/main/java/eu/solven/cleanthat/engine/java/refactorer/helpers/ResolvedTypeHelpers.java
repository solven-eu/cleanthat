/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import java.util.Optional;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.typesystem.LazyType;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import lombok.extern.slf4j.Slf4j;

/**
 * Helps working with {@link ResolvedType}
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class ResolvedTypeHelpers {

	protected ResolvedTypeHelpers() {
		// hidden
	}

	/**
	 * This specifically workaround difficulties around LazyType
	 * 
	 * @param left
	 * @param right
	 * @return true if the 2 resolvedTypes can be considered equivalent
	 */
	public static boolean areSameType(ResolvedType left, ResolvedType right) {
		if (left instanceof LazyType || right instanceof LazyType) {
			// https://github.com/javaparser/javaparser/issues/1983
			return left.describe().equals(right.describe());
		}

		return left.equals(right);
	}

	public static Optional<ResolvedType> optResolvedType(Type type) {
		try {
			return Optional.of(type.resolve());
		} catch (RuntimeException e) {
			try {
				var secondTryType = type.resolve();

				AJavaparserNodeMutator
						.logJavaParserIssue(type, e, "https://github.com/javaparser/javaparser/issues/3939");

				return Optional.of(secondTryType);
			} catch (RuntimeException ee) {
				// UnsolvedSymbolException | UnsupportedOperationException
				// Caused by: java.lang.UnsupportedOperationException: CorrespondingDeclaration not available for
				// unsolved symbol.
				// at com.github.javaparser.resolution.model.SymbolReference.getCorrespondingDeclaration
				// (SymbolReference.java:116)
				LOGGER.debug("Issue with JavaParser over {}", type, ee);
				return Optional.empty();
			}
		} catch (NoClassDefFoundError e) {
			AJavaparserNodeMutator.logJavaParserIssue(type, e, "https://github.com/javaparser/javaparser/issues/3504");

			return Optional.empty();
		}
	}

	public static boolean isAssignableBy(ReferenceTypeImpl referenceTypeImpl, ResolvedType resolvedType) {
		try {
			return referenceTypeImpl.isAssignableBy(resolvedType);
		} catch (UnsolvedSymbolException e) {
			LOGGER.debug("Unresolved: `{}` .isAssignableBy `{}`", referenceTypeImpl, resolvedType, e);

			return false;
		}
	}

	/**
	 * 
	 * @param qualifiedClassName
	 * @param resolvedType
	 * @return true if `qualifiedClassName` is java.util.Collection and `resolvedType` is java.util.List
	 */
	public static boolean isAssignableBy(String qualifiedClassName, ResolvedType resolvedType) {
		var typeSolver = new ReflectionTypeSolver(false);
		SymbolReference<ResolvedReferenceTypeDeclaration> optType = typeSolver.tryToSolveType(qualifiedClassName);

		if (!optType.isSolved()) {
			return false;
		}

		// https://github.com/javaparser/javaparser/issues/3929
		var referenceTypeImpl = new ReferenceTypeImpl(optType.getCorrespondingDeclaration());

		return isAssignableBy(referenceTypeImpl, resolvedType);
	}

	public static boolean typeIsAssignable(Optional<ResolvedType> optType, String requiredType) {
		if (optType.isEmpty()) {
			return false;
		}

		var type = optType.get();

		var isCorrectClass = false;
		if (type.isConstraint()) {
			// Happens on Lambda
			type = type.asConstraintType().getBound();
		}

		if (isAssignableBy(requiredType, type)) {
			isCorrectClass = true;
		} else if (type.isPrimitive() && type.asPrimitive().describe().equals(requiredType)) {
			// For a primitive double, requiredType is 'double'
			isCorrectClass = true;
		}
		if (!isCorrectClass) {
			return false;
		}

		return true;
	}

}
