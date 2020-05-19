# Code guidelines

## Prohibit inheritance by default

Write classes as `final` when no inheritance is to be done.

If a class is not final it must be expected to be extended, so it must be designed and documented as such.

Cases where final is not acceptable:

-   Configuration (`@Configuration`)
-   Components
-   Tests

Source: https://stackoverflow.com/questions/218744/good-reasons-to-prohibit-inheritance-in-java

## Referencing imported beans

In a `Component`:

-   Initialize Beans in a constructor instead of using `@Autowired`.
    -   It allows to make `final` attributes to reference to Beans.

In tests (`src/test/java/`)

-   Use `@Autowired` to create attributes that reference to beans.
    -   That is so mocked Beans made with `@MockBean` are properly distinguished from other beans.

Source: https://stackoverflow.com/questions/40620000/spring-autowire-on-properties-vs-constructor
