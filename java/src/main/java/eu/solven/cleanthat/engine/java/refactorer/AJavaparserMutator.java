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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.annotations.VisibleForTesting;

import eu.solven.cleanthat.engine.java.refactorer.function.OnMethodName;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Enables common behavior to JavaParser-based rules
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings("PMD.GodClass")
public abstract class AJavaparserMutator implements IJavaparserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(AJavaparserMutator.class);

	private static final AtomicInteger WARNS_IDEMPOTENCY_COUNT = new AtomicInteger();

	@Deprecated
	@VisibleForTesting
	public static int getWarnCount() {
		return WARNS_IDEMPOTENCY_COUNT.get();
	}

	protected boolean replaceBy(Node replacee, Node replacementNode) {
		LOGGER.info("Replacing `{}` by `{}`", replacee, replacementNode);

		return replacee.replace(replacementNode);
	}

	@Override
	public Optional<Node> walkAst(Node tree) {
		var transformed = new AtomicBoolean();
		tree.walk(node -> {
			boolean hasTransformed;
			try {
				LOGGER.trace("{} is going over {}",
						this.getClass().getSimpleName(),
						PepperLogHelper.getObjectAndClass(node));
				hasTransformed = processNotRecursively(node);
			} catch (RuntimeException e) {
				String rangeInSourceCode = "Around lines: " + node.getTokenRange().map(Object::toString).orElse("-");
				var messageForIssueReporting = messageForIssueReporting(this, node);
				throw new IllegalArgumentException(
						"Issue with a cleanthat mutator. " + rangeInSourceCode + " " + messageForIssueReporting,
						e);
			}

			if (hasTransformed) {
				LOGGER.debug("{} transformed something into `{}`", this, node);
				idempotencySanityCheck(node);
				transformed.set(true);
			}
		});
		if (transformed.get()) {
			return Optional.of(tree);
		} else {
			return Optional.empty();
		}
	}

	private void idempotencySanityCheck(Node node) {
		var transformAgain = processNotRecursively(node);
		// 'NoOp' is a special parserRule which always returns true even while it did not transform the code
		if (!this.getIds().contains(IMutator.ID_NOOP) && transformAgain) {
			// This may restore the initial code (e.g. if the rule is switching 'a.equals(b)' to 'b.equals(a)'
			// to again 'a.equals(b)')
			WARNS_IDEMPOTENCY_COUNT.incrementAndGet();
			var messageForIssueReporting = messageForIssueReporting(this, node);
			LOGGER.warn("A mutator is not idem-potent. {}", messageForIssueReporting);
		}
	}

	public static String messageForIssueReporting(IMutator mutator, Node node) {
		var faultyCode = node.toString();

		String messageForIssueReporting =
				"\r\n\r\nPlease report it to '" + "https://github.com/solven-eu/cleanthat/issues"
						+ "' referring the faulty mutator: '"
						+ mutator.getClass().getName()
						+ " with as testCase: \r\n\r\n"
						+ faultyCode;
		return messageForIssueReporting;
	}

	protected boolean processNotRecursively(Node node) {
		Optional<Node> optReplacement = replaceNode(node);

		if (optReplacement.isPresent()) {
			var replacement = optReplacement.get();
			return tryReplace(node, replacement);
		} else {
			return false;
		}
	}

	protected boolean tryReplace(Node node, Node replacement) {
		if (cancelDueToComment(node)) {
			LOGGER.info("We skip removing {} due to the presence of a comment", node);
			return false;
		}

		LOGGER.info("Turning `{}` into `{}`", node, replacement);

		var result = node.replace(replacement);

		if (!result) {
			LOGGER.warn("We failed turning `{}` into `{}`", node, replacement);
		}

		return result;
	}

	protected boolean tryRemove(Node node) {
		if (cancelDueToComment(node)) {
			LOGGER.info("We skip removing {} due to the presence of a comment", node);
			return false;
		}

		var nodeParentAsString = node.getParentNode().map(n -> n.getClass().getSimpleName()).orElse("-");
		LOGGER.info("Removing `{}` from a {}", node, nodeParentAsString);

		var result = node.remove();

		if (!result) {
			LOGGER.warn("Failed removing `{}` from a {}", node, nodeParentAsString);
		}
		return result;
	}

	protected boolean cancelDueToComment(Node node) {
		if (node.findFirst(Node.class, n -> n.getComment().isPresent()).isPresent()) {
			// For now, Cleanthat is pretty weak on comment management (due to Javaparser limitations)
			// So we prefer aborting any modification in case of comment presence, to prevent losing comments
			// https://github.com/javaparser/javaparser/issues/3677
			LOGGER.debug("You should cancel the operation due to the presence of a comment");
			return true;
		}

		return false;
	}

	protected Optional<Node> replaceNode(Node node) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	// https://github.com/javaparser/javaparser/issues/1491
	protected Optional<ResolvedType> optResolvedType(Expression expr) {
		if (expr.findCompilationUnit().isEmpty()) {
			// This node is not hooked anymore on a CompilationUnit
			return Optional.empty();
		}

		try {
			// ResolvedType type = expr.getSymbolResolver().calculateType(expr);
			var type = expr.calculateResolvedType();
			return Optional.of(type);
		} catch (RuntimeException e) {
			try {
				var secondTryType = expr.calculateResolvedType();

				logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3939");

				return Optional.of(secondTryType);
			} catch (RuntimeException ee) {
				LOGGER.debug("Issue resolving the type of {}", expr, ee);
				return Optional.empty();
			} catch (NoClassDefFoundError ee) {
				logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3504");

				return Optional.empty();
			}
		} catch (NoClassDefFoundError e) {
			logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3504");

			return Optional.empty();
		}
	}

	private void logJavaParserIssue(Object o, Throwable e, String issue) {
		var msg = "We encounter a case of {} for `{}`. Full-stack is available in 'debug'";
		if (LOGGER.isDebugEnabled()) {
			LOGGER.warn(msg, issue, o, e);
		} else {
			LOGGER.warn(msg, issue, o);
		}
	}

	protected Optional<ResolvedType> optResolvedType(Type type) {
		try {
			return Optional.of(type.resolve());
		} catch (RuntimeException e) {
			try {
				var secondTryType = type.resolve();

				logJavaParserIssue(type, e, "https://github.com/javaparser/javaparser/issues/3939");

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
			logJavaParserIssue(type, e, "https://github.com/javaparser/javaparser/issues/3504");

			return Optional.empty();
		}
	}

	protected Optional<ResolvedDeclaration> optResolved(Expression expr) {
		if (expr.findCompilationUnit().isEmpty()) {
			// This node is not hooked anymore on a CompilationUnit
			return Optional.empty();
		}

		if (!(expr instanceof Resolvable<?>)) {
			return Optional.empty();
		}

		try {
			Object resolved = ((Resolvable<?>) expr).resolve();
			return Optional.of((ResolvedDeclaration) resolved);
			// resolved = ((Resolvable<ResolvedValueDeclaration>) singleArgument).resolve();
		} catch (UnsolvedSymbolException e) {
			LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
			return Optional.empty();
		} catch (IllegalStateException | UnsupportedOperationException e) {
			if (e.getMessage().contains(SymbolResolver.class.getSimpleName())) {
				// com.github.javaparser.ast.Node.getSymbolResolver()
				LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
				return Optional.empty();
			} else if (e.getMessage().contains("unsolved symbol")) {
				// Caused by: java.lang.UnsupportedOperationException: CorrespondingDeclaration not available for
				// unsolved symbol.
				// at
				// com.github.javaparser.resolution.model.SymbolReference.getCorrespondingDeclaration(SymbolReference.java:116)
				LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
				return Optional.empty();
			} else {
				throw new IllegalStateException(e);
			}
		}
	}

	protected void onMethodName(Node node, String methodName, OnMethodName consumer) {
		if (node instanceof MethodCallExpr && methodName.equals(((MethodCallExpr) node).getName().getIdentifier())) {
			var methodCall = (MethodCallExpr) node;
			Optional<Expression> optScope = methodCall.getScope();
			if (optScope.isEmpty()) {
				// TODO Document when this would happen
				return;
			}
			var scope = optScope.get();
			Optional<ResolvedType> type = optResolvedType(scope);
			if (type.isPresent()) {
				consumer.onMethodName(methodCall, scope, type.get());
			}
		}
	}

	protected boolean scopeHasRequiredType(Optional<Expression> optScope, Class<?> requiredType) {
		// .canonicalName is typically required to handle primitive arrays
		// For int[]: qualifiedName is [I while canonicalName is int[]
		// BEWARE: How would it handle local or anonymous class?
		// https://stackoverflow.com/questions/5032898/how-to-instantiate-class-class-for-a-primitive-type
		return scopeHasRequiredType(optScope, requiredType.getName());
	}

	protected boolean scopeHasRequiredType(Optional<Expression> optScope, String requiredType) {
		Optional<ResolvedType> optType = optScope.flatMap(this::optResolvedType);

		return typeHasRequiredType(optType, requiredType);
	}

	protected boolean isAssignableBy(ReferenceTypeImpl referenceTypeImpl, ResolvedType resolvedType) {
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
	protected boolean isAssignableBy(String qualifiedClassName, ResolvedType resolvedType) {
		var typeSolver = new ReflectionTypeSolver(false);
		SymbolReference<ResolvedReferenceTypeDeclaration> optType = typeSolver.tryToSolveType(qualifiedClassName);

		if (!optType.isSolved()) {
			return false;
		}

		// https://github.com/javaparser/javaparser/issues/3929
		var referenceTypeImpl = new ReferenceTypeImpl(optType.getCorrespondingDeclaration());

		return isAssignableBy(referenceTypeImpl, resolvedType);
	}

	protected boolean typeHasRequiredType(Optional<ResolvedType> optType, String requiredType) {
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
		} else if (type.isPrimitive() && type.asPrimitive().describe().equals(requiredType)) {
			// For a primitive double, requiredType is 'double'
			isCorrectClass = true;
		}
		if (!isCorrectClass) {
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param compilationUnit
	 * @param methodRefPackage
	 * @param qualifiedName
	 * @return true if the given qualifiedName (which may be a nested Class) in given package is already imported in
	 *         given CompilationUnit
	 */
	protected boolean isImported(CompilationUnit compilationUnit, String methodRefPackage, String qualifiedName) {
		Optional<PackageDeclaration> optPackageDeclaration = compilationUnit.getPackageDeclaration();
		if (optPackageDeclaration.isPresent()) {
			var packageDecl = optPackageDeclaration.get().getNameAsString();

			// see UnnecessaryImport.removeSamePackageImports(Collection<ImportDeclaration>,
			// Optional<PackageDeclaration>)
			if (methodRefPackage.equals(packageDecl)) {
				return true;
			}
		}

		NodeList<ImportDeclaration> imports = compilationUnit.getImports();

		if (imports.isEmpty() && methodRefPackage.indexOf('.') >= 0) {
			return false;
		}

		if ("java.lang".equals(methodRefPackage)) {
			return true;
		}
		// TODO manage wildcards/asterisks
		return imports.stream().anyMatch(id -> id.getNameAsString().equals(qualifiedName));
	}

	protected String nameOrQualifiedName(CompilationUnit compilationUnit, Class<?> clazz) {
		if (isImported(compilationUnit, clazz.getPackageName(), clazz.getName())) {
			return clazz.getSimpleName();
		} else {
			return clazz.getName();
		}
	}

	/**
	 * 
	 * @param compilationUnit
	 * @param qualifiedName
	 * @return true if the given qualifiedName (which may be a nested Class) in given package can be imported in given
	 *         CompilationUnit without conflicting existing imports
	 */
	protected boolean isImportable(CompilationUnit compilationUnit, String qualifiedName) {
		NodeList<ImportDeclaration> imports = compilationUnit.getImports();

		var tokenName = getSimpleName(qualifiedName);

		// There is already a wildcard import: it may hold the token name
		return imports.stream()
				.noneMatch(id -> id.isAsterisk() || getSimpleName(id.getNameAsString()).equals(tokenName));
	}

	protected String getSimpleName(String qualifiedName) {
		var indexOfDot = qualifiedName.lastIndexOf('.');

		if (indexOfDot < 0) {
			return qualifiedName;
		} else {
			return qualifiedName.substring(indexOfDot + 1);
		}
	}
}
