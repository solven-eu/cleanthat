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

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.BoxedPrimitiveConstructor;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class BoxedPrimitiveConstructorCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new BoxedPrimitiveConstructor();
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

}