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
package eu.solven.cleanthat.engine.java.refactorer.test;

import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerEnums;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsResource;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;

/**
 * Base class for Cleanthat testing framework
 * 
 * @author Benoit Lacelle
 *
 * @param <N>
 * @param <R>
 */
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.GodClass" })
public abstract class ATestCases<N, R> implements IAstTestHelper<N, R> {

	protected static List<ClassOrInterfaceDeclaration> getAllCases(CompilationUnit compilationUnit) {
		return compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareTypes.class).isPresent()
						|| c.getAnnotationByClass(CompareMethods.class).isPresent()
						|| c.getAnnotationByClass(CompareClasses.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedMethod.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerClasses.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedInnerClass.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerAnnotations.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerEnums.class).isPresent()
						|| c.getAnnotationByClass(CompareMethodsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(CompareCompilationUnitsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent()
						|| c.getAnnotationByClass(CompareCompilationUnitsAsResources.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedCompilationUnitAsResource.class).isPresent());
	}

	public static MethodDeclaration getMethodWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<MethodDeclaration> preMethods = oneCase.getMethodsByName(name);
		if (preMethods.size() != 1) {
			throw new IllegalStateException("Expected one and only one '" + name + "' method in " + oneCase);
		}
		var pre = preMethods.get(0);
		return pre;
	}

	public static ClassOrInterfaceDeclaration getClassWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<ClassOrInterfaceDeclaration> matching =
				oneCase.findAll(ClassOrInterfaceDeclaration.class, n -> name.equals(n.getNameAsString()));

		if (matching.size() != 1) {
			throw new IllegalStateException(
					"We expected a single interface/class named '" + name + "' but they were: " + matching.size());
		}

		return matching.get(0);
	}

	public static AnnotationDeclaration getAnnotationWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<AnnotationDeclaration> matching =
				oneCase.findAll(AnnotationDeclaration.class, n -> name.equals(n.getNameAsString()));

		if (matching.size() != 1) {
			throw new IllegalStateException(
					"We expected a single annotation named '" + name + "' but they were: " + matching.size());
		}

		return matching.get(0);
	}

	public static EnumDeclaration getEnumWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<EnumDeclaration> matching = oneCase.findAll(EnumDeclaration.class, n -> name.equals(n.getNameAsString()));

		if (matching.size() != 1) {
			throw new IllegalStateException(
					"We expected a single enum named '" + name + "' but they were: " + matching.size());
		}

		return matching.get(0);
	}

}
