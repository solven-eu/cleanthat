package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

//TODO Have a maven module per version of Java, to ensure the post is valid
@Disabled("IMutator not-ready")
public class TestUseDiamondOperatorJdk8Cases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new UseDiamondOperatorJdk8();
	}

	@CompareMethods
	public static class CaseCollection {
		private <T> List<T> genericMethod(List<T> list) {
			return list;
		}

		public Object pre() {
			return genericMethod(new ArrayList<Number>());
		}

		public Object post(Collection<?> input) {
			return genericMethod(new ArrayList<>());
		}
	}
}
