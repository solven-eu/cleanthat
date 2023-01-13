package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.UseDiamondOperatorJdk8;
import eu.solven.cleanthat.language.java.refactorer.test.ARefactorerCases;

//TODO Have a maven module per version of Java, to ensure the post is valid
public class UseDiamondOperatorJdk8Cases extends ARefactorerCases {

	@Override
	public IClassTransformer getTransformer() {
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
