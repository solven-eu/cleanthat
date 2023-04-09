package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ThreadRunToThreadStart;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestThreadRunToThreadStartCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ThreadRunToThreadStart();
	}

	@CompareMethods
	public static class Nominal {
		public void pre(Runnable r) {
			Thread myThread = new Thread(r);
			myThread.run();
		}

		public void post(Runnable r) {
			Thread myThread = new Thread(r);
			myThread.start();
		}
	}

	@UnmodifiedMethod
	public static class RunnableRun {
		public void pre(Runnable r) {
			r.run();
		}
	}

}
