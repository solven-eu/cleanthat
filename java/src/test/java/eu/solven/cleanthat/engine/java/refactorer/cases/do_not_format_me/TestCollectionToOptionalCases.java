package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CollectionToOptional;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestCollectionToOptionalCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new CollectionToOptional();
	}

	@CompareMethods
	public static class CaseFindAnyIsPresent {
		public Object pre(List<?> list) {
			return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
		}

		public Object post(List<?> list) {
			return list.stream().findFirst();
		}
	}

}
