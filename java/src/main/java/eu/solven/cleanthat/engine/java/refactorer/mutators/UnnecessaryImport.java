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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaToken;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.javadoc.description.JavadocSnippet;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Remove imports from a Java source file by analyzing {@link ImportDeclaration}.
 * <p>
 * More precisely, it will analyze each Tokens beinh used in the code-base, and remove imports not matching given
 * import.
 * <p>
 * One limitation is it will not strip away wildcard imports.
 *
 * @author Benoit Lacelle
 */
// https://github.com/javaparser/javaparser/issues/1590
// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java
public class UnnecessaryImport extends AJavaParserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UnnecessaryImport.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public boolean isDraft() {
		return false;
	}

	@Override
	public Set<String> getPmdIds() {
		return ImmutableSet.of("UnnecessaryImport", "UnusedImports");
	}

	@Override
	public String pmdUrl() {
		// https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#unusedimports
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessaryimport";
	}

	@Override
	public Optional<String> getCheckstyleId() {
		return Optional.of("UnusedImports");
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/config_imports.html#UnusedImports";
	}

	// @Override
	// public Optional<String> getSonarId() {
	// return Optional.of("RSPEC-2333");
	// }

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof CompilationUnit)) {
			return false;
		}

		CompilationUnit compilationUnit = (CompilationUnit) node;

		NodeList<ImportDeclaration> importDeclarations = compilationUnit.getImports();
		if (importDeclarations.isEmpty()) {
			return false;
		}

		Set<String> tokensInUse = tokensInUse(compilationUnit);

		List<ImportDeclaration> unusedImports = new ArrayList<>();

		unusedImports.addAll(removeUnusedImports(importDeclarations, tokensInUse));
		unusedImports.addAll(removeSamePackageImports(importDeclarations, compilationUnit.getPackageDeclaration()));

		if (unusedImports.isEmpty()) {
			return false;
		} else {
			// Remove all unused imports
			unusedImports.forEach(importDeclaration -> {
				try {
					importDeclaration.remove();
				} catch (RuntimeException e) {
					throw new RuntimeException("Issue removing an import statement: " + importDeclaration, e);
				}
			});

			return true;
		}
	}

	/*
	 * Extract all of the tokens from the main body of the file.
	 *
	 * This set of tokens represents all of the file's dependencies, and is used to figure out whether or not an import
	 * is unused.
	 */
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L278
	private static Set<String> tokensInUse(CompilationUnit unit) {
		Stream<Node> packageDecl;

		if (unit.getPackageDeclaration().isPresent()) {
			packageDecl = Stream.of(unit.getPackageDeclaration().get())
					.map(PackageDeclaration::getAnnotations)
					.flatMap(NodeList::stream);
		} else {
			packageDecl = Stream.empty();
		}
		Stream<String> typesInCode = Stream.concat(packageDecl, unit.getTypes().stream())
				.map(Node::getTokenRange)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(r -> r != TokenRange.INVALID)
				.flatMap(r -> {
					// get all JavaTokens as strings from each range
					return StreamSupport.stream(r.spliterator(), false);
				})
				.map(JavaToken::asString);

		// Extract referenced class names from parsed javadoc comments:
		Stream<String> typesInJavadocs = unit.getAllComments()
				.stream()
				.filter(c -> c instanceof JavadocComment)
				.map(JavadocComment.class::cast)
				.map(JavadocComment::parse)
				.flatMap(UnnecessaryImport::parseJavadoc);

		return Stream.concat(typesInCode, typesInJavadocs)
				.filter(t -> t != null && !t.isEmpty() && Character.isJavaIdentifierStart(t.charAt(0)))
				.collect(Collectors.toSet());
	}

	/*
	 * Remove unused imports.
	 *
	 * This algorithm only looks at the file itself, and evaluates whether or not a given import is unused, by checking
	 * if the last segment of the import path (typically a class name or a static function name) appears in the file.
	 *
	 * This means that it is not possible to remove import statements with wildcards.
	 */
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L350
	private static List<ImportDeclaration> removeUnusedImports(Collection<ImportDeclaration> imports,
			Set<String> tokensInUse) {
		return imports.stream().filter(i -> {
			if (i.isAsterisk()) {
				return false;
			}

			String[] segments = i.getNameAsString().split("[.]");
			if (segments.length == 0) {
				throw new AssertionError("Parse tree includes invalid import statements");
			}
			String lastSegment = segments[segments.length - 1];

			return !tokensInUse.contains(lastSegment);
		}).collect(Collectors.toList());
	}

	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L366
	static List<ImportDeclaration> removeSamePackageImports(Collection<ImportDeclaration> imports,
			Optional<PackageDeclaration> packageDeclaration) {
		String packageName = packageDeclaration.map(p -> p.getName().toString()).orElse("");

		return imports.stream().filter(i -> {
			String imp = i.getNameAsString();
			if (packageName.isEmpty()) {
				return !imp.contains(".");
			}
			return imp.startsWith(packageName) && imp.lastIndexOf(".") <= packageName.length();
		}).collect(Collectors.toList());
	}

	// parse both main doc description and any block tags
	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L304
	private static Stream<String> parseJavadoc(Javadoc javadoc) {
		// parse main doc description
		Stream<String> stringsFromJavadocDescription =
				Stream.of(javadoc.getDescription()).flatMap(UnnecessaryImport::parseJavadocDescription);
		// grab tag names and parsed descriptions for block tags
		Stream<String> stringsFromBlockTags = javadoc.getBlockTags().stream().flatMap(tag -> {
			// only @throws and @exception have names who are importable; @param and others don't
			EnumSet<JavadocBlockTag.Type> blockTagTypesWithImportableNames =
					EnumSet.of(JavadocBlockTag.Type.THROWS, JavadocBlockTag.Type.EXCEPTION);
			Stream<String> importableTagNames;
			if (blockTagTypesWithImportableNames.contains(tag.getType())) {
				importableTagNames = Stream.of(tag.getName()).filter(Optional::isPresent).map(Optional::get);
			} else {
				importableTagNames = Stream.empty();
			}
			Stream<String> tagDescriptions =
					Stream.of(tag.getContent()).flatMap(UnnecessaryImport::parseJavadocDescription);
			return Stream.concat(importableTagNames, tagDescriptions);
		});
		return Stream.concat(stringsFromJavadocDescription, stringsFromBlockTags);
	}

	// https://github.com/revelc/impsort-maven-plugin/blob/main/src/main/java/net/revelc/code/impsort/ImpSort.java#L323
	private static Stream<String> parseJavadocDescription(JavadocDescription description) {
		return description.getElements().stream().map(element -> {
			if (element instanceof JavadocInlineTag) {
				// inline tags like {@link Foo}
				return ((JavadocInlineTag) element).getContent();
			} else if (element instanceof JavadocSnippet) {
				// snippets like @see Foo
				return element.toText();
			} else {
				// try to handle unknown elements as best we can
				return element.toText();
			}
		}).flatMap(s -> {
			// split text descriptions into word tokens
			return Stream.of(s.split("\\W+"));
		});
	}

}
