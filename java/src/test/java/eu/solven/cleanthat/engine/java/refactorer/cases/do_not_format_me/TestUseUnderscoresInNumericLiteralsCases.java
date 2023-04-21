package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseUnderscoresInNumericLiterals;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUseUnderscoresInNumericLiteralsCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new UseUnderscoresInNumericLiterals();
	}

	@CompareMethods
	public static class IntDecimal {
		public Object pre() {
			int int4 = 2023;
			int int6 = 202303;
			int int8 = 20230323;
			return int4 + int6 + int8;
		}

		public Object post() {
			int int4 = 2023;
			int int6 = 202_303;
			int int8 = 20_230_323;
			return int4 + int6 + int8;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class IntBinary {
		public Object pre() {
			return 0b01101001010011011110010101011110;
		}

		public Object post() {
			return 0b01101001_01001101_11100101_01011110;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class LongHexa {
		public Object pre() {
			return 0x7fffffffffffffffL;
		}

		public Object post() {
			return 0x7fff_ffff_ffff_ffffL;
		}
	}

	@CompareMethods
	public static class CaseReturnDouble {
		public Object pre() {
			return 123456789.0123456789;
		}

		public Object post() {
			return 123_456_789.012_345_678_9;
		}
	}

	@CompareMethods
	public static class CaseWeirdUnderscore {
		public Object pre() {
			int i = 12345678_9;
			return i;
		}

		public Object post() {
			int i = 123_456_789;
			return i;
		}
	}

	@CompareInnerClasses
	public static class SomeInterface {

		public interface Pre {
			long i = 123456789;
			float f_F = 123456F;
			double d_d = 7654321.12345678d;
		}

		public interface Post {
			long i = 123_456_789;
			float f_F = 123_456F;
			double d_d = 7_654_321.123_456_78d;
		}
	}

	@UnmodifiedInnerClass
	public static class SomeInterface_complexRepresentation {

		public interface Pre {
			long i_hex = 0x12E45A6E;
			double d_hex = 0x4.5p1f;
		}
	}
}
