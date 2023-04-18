package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachToIterableForEach;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestForEachToIterableForEachCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ForEachToIterableForEach();
	}

	@CompareMethods
	public static class forEach_singleStatement {
		public void pre(List<String> strings) {
			for (String string : strings) {
				System.out.println(string);
			}
		}

		public void post(List<String> strings) {
			strings.forEach(string -> {
				System.out.println(string);
			});
		}
	}

	@CompareMethods
	public static class forEach_singleStatement_noBlock {
		public void pre(List<String> strings) {
			for (String string : strings)
				System.out.println(string);

		}

		public void post(List<String> strings) {
			strings.forEach(string -> System.out.println(string));
		}
	}

	@CompareMethods
	public static class forEach_multipleStatement {
		public void pre(Collection<String> strings) {
			for (String value : strings) {
				int length = value.length();
				if (length > 2) {
					length /= 2;
				}
				System.out.println(value.substring(0, length));
			}
		}

		public void post(Collection<String> strings) {
			strings.forEach(value -> {
				int length = value.length();
				if (length > 2) {
					length /= 2;
				}
				System.out.println(value.substring(0, length));
			});
		}
	}

	@UnmodifiedMethod
	public static class forEach_assignExpr {
		public String pre(List<String> strings) {
			String s = null;

			for (String string : strings) {
				System.out.println(string);
				s = string;
			}

			return s;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class forEach_assignExpr_outer {
		String s = null;

		public String pre(List<String> strings) {
			strings.forEach(string -> {
				System.out.println(string);
				s = string;
			});

			return s;
		}

		public String post(List<String> strings) {
			for (String string : strings) {
				System.out.println(string);
				s = string;
			}

			return s;
		}
	}

	@UnmodifiedMethod
	public static class ThrowException {
		public void pre(List<String> strings) throws Exception {
			for (String string : strings) {
				throw new Exception(string);
			}
		}
	}

	@CompareMethods
	public static class ThrowImplicitException {
		public void pre(List<String> strings) throws IllegalArgumentException {
			for (String string : strings) {
				throw new IllegalArgumentException(string);
			}
		}

		public void post(List<String> strings) throws IllegalArgumentException {
			strings.forEach(string -> {
				throw new IllegalArgumentException(string);
			});
		}
	}

	@UnmodifiedMethod
	public static class ThrowExplicitException {
		public void pre(List<String> strings) throws TimeoutException {
			for (String string : strings) {
				throw new IllegalArgumentException(string);
			}
		}
	}

	@CompareMethods
	public static class ThrowError {
		public void pre(List<String> strings) throws OutOfMemoryError {
			for (String string : strings) {
				throw new OutOfMemoryError(string);
			}
		}

		public void post(List<String> strings) throws OutOfMemoryError {
			strings.forEach(string -> {
				throw new OutOfMemoryError(string);
			});
		}
	}

	@UnmodifiedMethod
	public static class ThrowThrowable {
		public void pre(List<String> strings) throws Throwable {
			for (String string : strings) {
				throw new Throwable(string);
			}
		}
	}

	@UnmodifiedMethod
	public static class continueKeyword {
		public void pre(List<String> strings) {
			for (String string : strings) {
				if (string.length() >= 3) {
					continue;
				}
			}
		}
	}

	@UnmodifiedMethod
	public static class forEachArray {
		public void pre(String... strings) {
			for (String string : strings) {
				System.out.println(string);
			}
		}
	}

	@UnmodifiedCompilationUnitAsString(pre = "class SomeClass {\n" + "\n"
			+ "	 String findAttributeInRules(String subpath,\n"
			+ "			boolean isFolder,\n"
			+ "			String key,\n"
			+ "			List<AttributesRule> rules) {\n"
			+ "		String value = null;\n"
			+ "		for (AttributesRule rule : rules) {\n"
			+ "			if (rule.isMatch(subpath, isFolder)) {\n"
			+ "				for (Attribute attribute : rule.getAttributes()) {\n"
			+ "					if (attribute.getKey().equals(key)) {\n"
			+ "						value = attribute.getValue();\n"
			+ "					}\n"
			+ "				}\n"
			+ "			}\n"
			+ "		}\n"
			+ "		return value;\n"
			+ "	}\n"
			+ "}")
	public static class UnresolvedType {

	}

	@CompareMethods
	public static class Imbricated_simple {
		public void pre(List<String> strings, List<String> strings2) {
			for (String string : strings) {
				for (String string2 : strings2) {
					System.out.println(string + string2);
				}
			}
		}

		public void post(List<String> strings, List<String> strings2) {
			strings.forEach(string -> {
				strings2.forEach(string2 -> {
					System.out.println(string + string2);
				});
			});
		}
	}

	@CompareMethods
	public static class Imbricated_complex {
		void pre(int key, List<String> rules) {
			for (String rule : rules) {
				if (rule.isEmpty()) {
					for (int attribute : rule.chars().mapToObj(i -> i).collect(Collectors.toList())) {
						if (attribute == key) {
							System.out.println(attribute);
						}
					}
				}
			}
		}

		void post(int key, List<String> rules) {
			rules.forEach(rule -> {
				if (rule.isEmpty()) {
					rule.chars().mapToObj(i -> i).collect(Collectors.toList()).forEach(attribute -> {
						if (attribute == key) {
							System.out.println(attribute);
						}
					});
				}
			});
		}
	}

}
