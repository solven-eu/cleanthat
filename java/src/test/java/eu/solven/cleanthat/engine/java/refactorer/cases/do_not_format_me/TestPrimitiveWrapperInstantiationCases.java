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
package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.awt.geom.Rectangle2D;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.PrimitiveWrapperInstantiation;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestPrimitiveWrapperInstantiationCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new PrimitiveWrapperInstantiation();
	}

	@CompareMethods
	public static class CaseBoolean_valueOf {
		public Object pre(boolean input) {
			return new Boolean(input);
		}

		public Object post(boolean input) {
			return Boolean.valueOf(input);
		}
	}

	@CompareMethods
	public static class CaseDouble_valueOf {
		public Object pre(double input) {
			return new Double(input);
		}

		public Object post(double input) {
			return Double.valueOf(input);
		}
	}

	@UnmodifiedMethod
	public static class RectangleDouble {
		public Object pre(double input) {
			return new Rectangle2D.Double(1, 2, 3, 4);
		}
	}

	// This would conflict with java,lang.Character
	public static class Character {
		char nameOfMyCharacter;

		public Character(char nameOfMyCharacter) {
			this.nameOfMyCharacter = nameOfMyCharacter;
		}
	}

	@UnmodifiedMethod
	public static class ConflictWithClassWithSameName {
		public Object pre(char c) {
			return new Character(c);
		}

		public Object post(boolean input) {
			return Boolean.valueOf(input);
		}
	}
}