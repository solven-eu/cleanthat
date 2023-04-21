package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.IOException;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.AvoidUncheckedExceptionsInSignatures;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestAvoidUncheckedExceptionsInSignaturesCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new AvoidUncheckedExceptionsInSignatures();
	}

	@CompareMethods
	public static class CaseRuntimeException {
		public void pre() throws RuntimeException {
		}

		public void post() {
		}
	}

	@CompareMethods
	public static class CaseInterlaced {
		public void pre() throws IOException, RuntimeException, ReflectiveOperationException, IllegalArgumentException {
		}

		public void post() throws IOException, ReflectiveOperationException {
		}
	}

	@CompareMethods
	public static class CaseIllegalArgumentException {
		public void pre() throws IllegalArgumentException {
		}

		public void post() {
		}
	}

	// Deep in hierarchy
	@CompareMethods
	public static class CaseArrayIndexOutOfBoundsException {
		public void pre() throws ArrayIndexOutOfBoundsException {
		}

		public void post() {
		}
	}

	@UnmodifiedMethod
	public static class CaseException {
		public void pre() throws Exception {
		}
	}

	@UnmodifiedMethod
	public static class CaseIOException {
		public void pre() throws IOException {
		}
	}

	@UnmodifiedMethod
	public static class CaseError {
		public void pre() throws Error {
		}
	}

	@UnmodifiedCompilationUnitAsString(pre = "package hudson.security;\n" + "\n"
			+ "public abstract class FederatedLoginService implements ExtensionPoint {\n"
			+ "\n"
			+ "    public abstract class FederatedIdentity implements Serializable {\n"
			+ "        @NonNull\n"
			+ "        public User signin() throws UnclaimedIdentityException {\n"
			+ "            throw new UnclaimedIdentityException(this);\n"
			+ "        }\n"
			+ "\n"
			+ "    }\n"
			+ "\n"
			+ "    public static class UnclaimedIdentityException extends RuntimeException implements HttpResponse {\n"
			+ "        public final FederatedIdentity identity;\n"
			+ "\n"
			+ "        public UnclaimedIdentityException(FederatedIdentity identity) {\n"
			+ "            this.identity = identity;\n"
			+ "        }\n"
			+ "    }\n"
			+ "}\n"
			+ "")
	public static class UnknownType {
	}

}
