package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfBreakToStreamFindFirst;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestForEachIfBreakToStreamFindFirstCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ForEachIfBreakToStreamFindFirst();
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-find-first.html#loop-with-break-statement
	@CompareMethods
	public static class WithBreak {
		public String pre(List<String> values) {
			String key = "";
			for (String value : values) {
				if (value.length() > 4) {
					key = value;
					break;
				}
			}
			return key;
		}

		public String post(List<String> values) {
			String key = values.stream().filter(value -> value.length() > 4).findFirst().orElse("");
			return key;
		}
	}

	// This should be done in a separate IMutator
	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-find-first.html#loop-with-return-statement
	@CompareMethods
	@CaseNotYetImplemented
	public static class WithReturn {
		public String pre(List<String> values) {
			for (String value : values) {
				if (value.length() > 4) {
					return value;
				}
			}
			return "";
		}

		public String post(List<String> values) {
			return values.stream().filter(value -> value.length() > 4).findFirst().orElse("");
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-find-first.html#implicit-casting
	@CompareMethods
	public static class ImplicitCasting_double {
		public double pre(List<Integer> values) {
			double defaultValue = -1.0;
			double defaultIndex = defaultValue;
			for (int value : values) {
				if (value > 4) {
					defaultIndex = value;
					break;
				}
			}

			return defaultIndex;
		}

		public double post(List<Integer> values) {
			double defaultValue = -1.0;
			double defaultIndex =
					values.stream().filter(value -> value > 4).findFirst().map(Double::valueOf).orElse(defaultValue);

			return defaultIndex;
		}
	}

	@CompareMethods
	public static class ImplicitCasting_long {
		public double pre(List<Character> values) {
			long defaultValue = -1;
			long defaultIndex = defaultValue;
			for (char value : values) {
				if (value > 4) {
					defaultIndex = value;
					break;
				}
			}

			return defaultIndex;
		}

		public double post(List<Character> values) {
			long defaultValue = -1;
			long defaultIndex =
					values.stream().filter(value -> value > 4).findFirst().map(Long::valueOf).orElse(defaultValue);

			return defaultIndex;
		}
	}

	@CompareMethods
	public static class ImplicitCasting_charSequence {
		public CharSequence pre(List<String> values) {
			StringBuilder defaultValue = new StringBuilder();
			CharSequence defaultIndex = defaultValue;
			for (String value : values) {
				if (!value.isEmpty()) {
					defaultIndex = value;
					break;
				}
			}

			return defaultIndex;
		}

		public CharSequence post(List<String> values) {
			StringBuilder defaultValue = new StringBuilder();
			CharSequence defaultIndex = values.stream()
					.filter(value -> !value.isEmpty())
					.findFirst()
					.map(CharSequence.class::cast)
					.orElse(defaultValue);

			return defaultIndex;
		}
	}

	@CompareMethods
	// @CaseNotYetImplemented
	public static class ImplicitCasting_charSequence_reversed {
		public CharSequence pre(List<CharSequence> values) {
			StringBuilder defaultValue = new StringBuilder();
			String defaultIndex = defaultValue.toString();
			for (CharSequence value : values) {
				if (!value.toString().isEmpty()) {
					defaultIndex = (String) value;
					break;
				}
			}

			return defaultIndex;
		}

		public CharSequence post(List<CharSequence> values) {
			StringBuilder defaultValue = new StringBuilder();
			String defaultIndex = values.stream()
					.filter(value -> !value.toString().isEmpty())
					.findFirst()
					.map(value -> (String) value)
					.orElse(defaultValue.toString());

			return defaultIndex;
		}
	}

	@CompareMethods
	public static class WithBreak_editInAssignement {
		public String pre(List<String> values) {
			String key = "";
			for (String value : values) {
				if (value.length() > 4) {
					key = value.substring(4);
					break;
				}
			}
			return key;
		}

		public String post(List<String> values) {
			String key = values.stream()
					.filter(value -> value.length() > 4)
					.findFirst()
					.map(value -> value.substring(4))
					.orElse("");
			return key;
		}
	}

	@CompareMethods
	public static class ImplicitCasting_editInAssignement {
		public CharSequence pre(List<String> values) {
			StringBuilder defaultValue = new StringBuilder();
			CharSequence key = defaultValue;
			for (String value : values) {
				if (value.length() > 4) {
					key = value.substring(4);
					break;
				}
			}
			return key;
		}

		public CharSequence post(List<String> values) {
			StringBuilder defaultValue = new StringBuilder();
			CharSequence key = values.stream()
					.filter(value -> value.length() > 4)
					.findFirst()
					.map(value -> value.substring(4))
					.map(CharSequence.class::cast)
					.orElse(defaultValue);
			return key;
		}
	}
}
