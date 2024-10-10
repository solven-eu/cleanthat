public record SomeRecord(int i) {
	public static abstract interface SomeInterface {
	}

	public static class SomeNestedClass {
	}

	public class SomeInnerClass {
		{
			System.out.println(i);
		}
	}

	public static abstract class SomeAbstractNestedClass {
	}

	public abstract class SomeAbstractInnerClass {
		{
			System.out.println(i);
		}
	}

	public static abstract @interface SomeAnnotation {
	}

	public static enum SomeEnum {
	}

	public static final record SomeRecord(int i) {
	}
}
