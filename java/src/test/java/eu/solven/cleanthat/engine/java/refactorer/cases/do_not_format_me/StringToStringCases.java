package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StringToString;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class StringToStringCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
		return new StringToString();
	}

	@UnchangedMethod
	public static class CaseNotString {
		public Object pre(Number o) {
			return o.toString();
		}
	}

	@CompareMethods
	public static class CaseString {
		public Object pre(String o) {
			return o.toString();
		}

		public Object post(String o) {
			return o;
		}
	}

	@UnchangedMethod
	public static class CaseCharSequence {
		public Object pre(CharSequence o) {
			return o.toString();
		}

		public Object post(CharSequence o) {
			return o.toString();
		}
	}

	@CompareMethods
	public static class CaseOnLambda {
		public Object pre(List<String> input) {
			return input.stream().filter(s -> !s.toString().isEmpty()).collect(Collectors.toList());
		}

		public Object post(List<String> input) {
			return input.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
		}
	}
}
