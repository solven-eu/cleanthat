public record SomeRecord(int i) {
	public interface SomeInterface {
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

	public @interface SomeAnnotation {
	}

	public enum SomeEnum {
	}

	public record SomeNestedRecord(int i) {
	}
}
