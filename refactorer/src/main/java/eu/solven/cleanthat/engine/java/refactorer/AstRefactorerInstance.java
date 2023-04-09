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

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

/**
 * This hold the logic of applying a single {@link IMutator}
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings("PMD.GenericsNaming")
class AstRefactorerInstance<AST, P, R> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AstRefactorerInstance.class);

	final AAstRefactorer<AST, P, R, ? extends IWalkingMutator<AST, R>> astRefactorer;
	final P parser;
	final IWalkingMutator<AST, R> mutator;

	final AtomicReference<AST> refCompilationUnit;
	final AtomicBoolean firstMutator;
	final AtomicBoolean inputIsBroken;

	AstRefactorerInstance(AAstRefactorer<AST, P, R, ? extends IWalkingMutator<AST, R>> astRefactorer,
			P parser,
			IWalkingMutator<AST, R> ct,
			AtomicReference<AST> refCompilationUnit,
			AtomicBoolean firstMutator,
			AtomicBoolean inputIsBroken) {
		this.astRefactorer = astRefactorer;
		this.mutator = ct;
		this.parser = parser;

		this.refCompilationUnit = refCompilationUnit;
		this.firstMutator = firstMutator;
		this.inputIsBroken = inputIsBroken;
	}

	public boolean applyOneMutator(AtomicReference<String> refCleanCode,
			AtomicReference<AST> refCompilationUnit,
			AtomicBoolean firstMutator,
			AtomicBoolean inputIsBroken,
			Path path) {
		if (inputIsBroken.get()) {
			LOGGER.trace("We skip {} as the input is broken", mutator);
			return false;
		}

		LOGGER.debug("Applying {}", mutator);
		parseCompilationUnit(refCompilationUnit, firstMutator, inputIsBroken, refCleanCode.get(), path);
		return applyMutator(refCleanCode, refCompilationUnit, path, mutator);
	}

	private boolean applyMutator(AtomicReference<String> refCleanCode,
			AtomicReference<AST> optCompilationUnit,
			Path path,
			IWalkingMutator<AST, R> mutator) {
		var compilationUnit = optCompilationUnit.get();
		if (compilationUnit == null) {
			// For any reason, we failed parsing the compilationUnit: do not apply the mutator
			return false;
		}

		Optional<R> walkNodeResult;
		try {
			walkNodeResult = mutator.walkAst(compilationUnit);
		} catch (RuntimeException | StackOverflowError e) {
			// StackOverflowError may come from Javaparser
			// e.g. https://github.com/javaparser/javaparser/issues/3940
			throw new IllegalArgumentException("Issue with mutator: " + mutator, e);
		}

		boolean appliedWithChange;

		if (walkNodeResult.isPresent()) {
			// Prevent Javaparser polluting the code, as it often impamutators comments when building back code from
			// AST,
			// or removing consecutive EOL
			LOGGER.debug("IMutator {} linted succesfully {}", mutator.getClass().getSimpleName(), path);

			// One relevant change: building source-code from the AST
			var resultAsString = astRefactorer.toString(walkNodeResult.get());
			if (astRefactorer.isValidResultString(parser, resultAsString)) {
				if (refCleanCode.get().equals(resultAsString)) {

					appliedWithChange = false;
				} else {
					refCleanCode.set(resultAsString);
					appliedWithChange = true;
				}
			} else {
				LOGGER.warn("{} generated invalid code over {}", mutator, path);
				appliedWithChange = false;
			}

			// Discard cache. It may be useful to prevent issues determining some types in mutated compilationUnits
			optCompilationUnit.set(null);
		} else {
			appliedWithChange = false;
		}

		return appliedWithChange;
	}

	private void parseCompilationUnit(AtomicReference<AST> optCompilationUnit,
			AtomicBoolean firstMutator,
			AtomicBoolean inputIsBroken,
			String refCleanCode,
			Path path) {
		// Fill cache
		if (optCompilationUnit.get() == null) {
			try {
				var sourceCode = refCleanCode;
				var tryCompilationUnit = astRefactorer.parseSourceCode(parser, sourceCode);
				if (tryCompilationUnit.isEmpty()) {
					// We are not able to parse the input
					LOGGER.warn("Not able to parse path='{}' with {}", path, parser);

					if (firstMutator.get()) {
						// BEWARE we may have mutators based on different parsers
						LOGGER.info("We mark path='{}' as not parseable by any mutator", path);
						inputIsBroken.set(true);
					}

					return;
				} else {
					optCompilationUnit.set(tryCompilationUnit.get());
				}
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Issue parsing the code", e);
			}
		}

		firstMutator.set(false);
	}
}
