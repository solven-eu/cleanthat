/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.java.mutators;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.eclipse.ICodeProcessor;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.UseIsEmptyOnCollections;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Bridges to Eclipse formatting engine
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/java/JavaFormatter.java
public class RulesJavaMutator implements ICodeProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RulesJavaMutator.class);

	private final CleanthatJavaProcessorProperties properties;

	private final List<IClassTransformer> transformers;

	public RulesJavaMutator(CleanthatJavaProcessorProperties properties) {
		this.properties = properties;

		transformers =
				Arrays.asList(new EnumsWithoutEquals(), new PrimitiveBoxedForString(), new UseIsEmptyOnCollections());
	}

	@Override
	public String doFormat(String code, LineEnding eolToApply) throws IOException {
		LOGGER.debug("{}", this.properties);
		AtomicReference<String> codeRef = new AtomicReference<>(code);

		transformers.forEach(ct -> {
			LOGGER.debug("Applying {}", ct);
			CompilationUnit compilationUnit = StaticJavaParser.parse(codeRef.get());

			compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
					.stream()
					.flatMap(classDef -> classDef.getMethods().stream())
					.forEach(methodDef -> {
						if (ct.transform(methodDef)) {
							LOGGER.info("It is a hit");
						}
					});

			codeRef.set(compilationUnit.toString());
		});

		return codeRef.get();
	}

}