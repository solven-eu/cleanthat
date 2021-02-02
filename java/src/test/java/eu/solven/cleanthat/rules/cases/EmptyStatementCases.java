package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.rules.EmptyStatement;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverClass;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;
import eu.solven.cleanthat.rules.test.ICaseShouldNotBeImpacted;

public class EmptyStatementCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new EmptyStatement();
	}

	// https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html
	// It is unclear how this case should be fixed
	public static class CaseDoubleSemiColumn implements ICaseOverMethod {

		public Object pre() {
			int i = 5;
			i++;
			;
			return i;
		}

		public Object post() {
			int i = 5;
			i++;
			return i;
		}
	}

	// https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html
	// It is unclear how this case should be fixed
	public static class CaseIf implements ICaseOverMethod, ICaseShouldNotBeImpacted {
		public Object stable() {
			int i = 5;
			if (i > 3)
				; // violation, ";" right after if statement
			i++;

			return i;
		}
	}

	// https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html
	// It is unclear how this case should be fixed
	public static class CaseFor implements ICaseOverMethod, ICaseShouldNotBeImpacted {

		public Object stable() {
			int i = 5;
			for (i = 0; i < 5; i++)
				; // violation
			i++;
			return i;
		}
	}

	// https://checkstyle.sourceforge.io/apidocs/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html
	public static class CaseWhile implements ICaseOverClass, ICaseShouldNotBeImpacted {
		public Object stable() {
			int i = 5;
			while (i > 10) // OK
				i++;
			return i;
		}
	}
}
