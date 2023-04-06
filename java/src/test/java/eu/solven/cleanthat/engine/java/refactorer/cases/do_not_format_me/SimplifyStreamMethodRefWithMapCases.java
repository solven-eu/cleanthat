package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyStreamMethodRefWithMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class SimplifyStreamMethodRefWithMapCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new SimplifyStreamMethodRefWithMap();
	}

	@CompareMethods
	public static class CaseMethodRef {
		void pre(List<String> list) {
			list.stream().anyMatch(element -> element.toLowerCase().equals("searchMe"));
		}

		void post(List<String> list) {
			list.stream().map(String::toLowerCase).anyMatch(element -> element.equals("searchMe"));
		}
	}

	@CompareMethods
	public static class CaseMethodRefMethodRef {
		void pre(List<String> list) {
			list.stream().anyMatch(element -> element.toLowerCase().toUpperCase().equals("searchMe"));
		}

		void post(List<String> list) {
			list.stream().map(element -> element.toLowerCase().toUpperCase()).anyMatch(s -> s.equals("searchMe"));
		}
	}
}
