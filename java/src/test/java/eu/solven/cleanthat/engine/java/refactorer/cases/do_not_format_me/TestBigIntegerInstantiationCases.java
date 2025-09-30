package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.BigIntegerInstantiation;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestBigIntegerInstantiationCases extends AJavaparserRefactorerCases {

    @Override
    public IJavaparserAstMutator getTransformer() {
        return new BigIntegerInstantiation();
    }

    @CompareMethods
    public static class BigIntegerZero {
        public Object pre() {
            return new BigInteger("0");
        }

        public Object post() {
            return BigInteger.ZERO;
        }
    }

    @CompareMethods
    public static class BigIntegerOne {
        public Object pre() {
            return new BigInteger("1");
        }

        public Object post() {
            return BigInteger.ONE;
        }
    }

    @CompareMethods
    public static class BigIntegerTen {
        public Object pre() {
            return new BigInteger("10");
        }

        public Object post() {
            return BigInteger.TEN;
        }
    }

    @CompareMethods
    public static class BigDecimalZero {
        public Object pre() {
            return new BigDecimal("0");
        }

        public Object post() {
            return BigDecimal.ZERO;
        }
    }

    @CompareMethods
    public static class BigDecimalOne {
        public Object pre() {
            return new BigDecimal("1");
        }

        public Object post() {
            return BigDecimal.ONE;
        }
    }

    @CompareMethods
    public static class BigDecimalTen {
        public Object pre() {
            return new BigDecimal("10");
        }

        public Object post() {
            return BigDecimal.TEN;
        }
    }

    @CompareMethods
    public static class BigDecimalZeroInt {
        public Object pre() {
            return new BigDecimal(0);
        }

        public Object post() {
            return BigDecimal.ZERO;
        }
    }

    @CompareMethods
    public static class BigDecimalZeroLong {
        public Object pre() {
            return new BigDecimal(0L);
        }

        public Object post() {
            return BigDecimal.ZERO;
        }
    }

    @CompareMethods
    public static class BigDecimalZeroDouble {
        public Object pre() {
            return new BigDecimal(0d);
        }

        public Object post() {
            return BigDecimal.ZERO;
        }
    }

    @CompareMethods
    public static class BigDecimalZeroFloat {
        public Object pre() {
            return new BigDecimal(0f);
        }

        public Object post() {
            return BigDecimal.ZERO;
        }
    }

    @CompareMethods
    public static class BigDecimalOneDouble {
        public Object pre() {
            return new BigDecimal(1.);
        }

        public Object post() {
            return BigDecimal.ONE;
        }
    }

    @CompareMethods
    public static class BigDecimalTenDouble {
        public Object pre() {
            return new BigDecimal(10.d);
        }

        public Object post() {
            return BigDecimal.TEN;
        }
    }

    @UnmodifiedMethod
    public static class BigIntegerInvalidTen {
        public Object pre() {
            return new BigInteger("1_0");
        }
    }

    @UnmodifiedMethod
    public static class BigIntegerNegativeOne {
        public Object pre() {
            return new BigInteger("-1");
        }
    }

    @UnmodifiedMethod
    public static class BigDecimalNegativeOne {
        public Object pre() {
            return new BigDecimal(-1);
        }
    }

    @UnmodifiedMethod
    public static class BigIntegerThree {
        public Object pre() {
            return new BigInteger("3");
        }
    }

    @UnmodifiedMethod
    public static class BigDecimalThreeDouble {
        public Object pre() {
            return new BigDecimal(3d);
        }
    }

    @UnmodifiedMethod
    public static class BigIntegerEmpty {
        public Object pre() {
            return new BigInteger("");
        }
    }

    @UnmodifiedMethod
    public static class BigDecimalEmpty {
        public Object pre() {
            return new BigDecimal("");
        }
    }

    @UnmodifiedMethod
    public static class BigDecimalRandomDouble {
        public Object pre() {
            return new BigDecimal(10.00000001);
        }
    }

    @UnmodifiedMethod
    public static class BigDecimalRandomString {
        public Object pre() {
            return new BigDecimal("10.00000001");
        }
    }

    // Could be supported, but it's not worth adding complexity for a marginal case
    @UnmodifiedMethod
    public static class BigDecimalTenUnderscore {
        public Object pre() {
            return new BigDecimal(1_0);
        }
    }

    // Turns NumberFormatException into a BigInteger
    @CompareMethods
    public static class BigIntegerInvalidOne {
        public Object pre() {
            return new BigInteger("1.");
        }

        public Object post() {
            return BigInteger.ONE;
        }
    }

}
