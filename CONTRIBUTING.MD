# How to compile the project

    git clone git@github.com:solven-eu/cleanthat.git
    mvn install -Pfast -T 8

The `-Pfast` profile may be necessary to circumvent cleanthat depenending on itself to apply `spotless` on itself.

Once done (or re-done after a release), you can simply:

    mvn install

# Note for `mutator` contributors

## Unit-tests

Mutators are subject to many edge-cases, due to the complexity of syntaxes (including Java).

### Unmodified code

One can check a given code is not impacted with `@UnmodifiedMethod` over a nested-class. The nest-class represents a unit-case:
```
@UnmodifiedMethod
public static class CaseCollection {
    public Object pre(Collection<?> input) {
        return input.size() == 0;
    }
}
```

### Simple code comparison

On can check some code is modified as expected with `CompareMethods` over a nested-class. The nest-class represents a unit-case:
```
@CompareMethods
public static class CaseStringEqualsIgnoreCaseEmpty {
    public Object pre(String input) {
        return input.equalsIgnoreCase("");
    }

    public Object post(String input) {
        return input.isEmpty();
    }
}
```

## Code not compiling with Cleanthat own classpath

You may consider rules with advanced edge-case, requiring specific code which would not compile in `test` module classpath. It happens when testing code with a java language-level different from Cleanthat own language-level (e.g. a rule processing `JDK21` while cleanthat is compiled as `JDK11`).

One can rely on `@CompareCompilationUnitsAsResources`:

```
@CompareCompilationUnitsAsResources(
    pre =  "/source/do_not_format_me/UnnecessaryModifier/Issue807.java",
    post = "/source/do_not_format_me/UnnecessaryModifier/Issue807.java")
public static class Issue807 {
}
```


## Manual test over large code-bases

A given rule can be tested against a large set of codebases with https://github.com/solven-eu/cleanthat-showcases. See its README for more details.

# Notes for maintainers

## Deploy into Production (AWS Lambda):

        git push origin master:deploy-prd

## Release a new version (and deploy jars to Sonatype m2central):

        mvn release:clean release:prepare release:perform

### In case of failure

The release process may fail for various reasons:

- Sonatype timed-out

They, one would typically need to revert its local head, and force push master before the release. And delete the falsy tags:

        git tag -d v2.XX.RELEASE
        git push --delete origin v2.XX.RELEASE

### New computer, new GPG Key

https://stackoverflow.com/questions/29885887/gpg-no-default-secret-key-error-using-maven

        gpg --list-keys

https://keyring.debian.org/creating-key.html

        gpg --gen-key --default-new-key-algo=rsa4096/cert,sign+rsa4096/encr
        gpg --keyserver https://keyserver.ubuntu.com/ --send-key 90A8________________________________AAB7

BEWARE pool.sks-keyservers.net is down: https://www.reddit.com/r/GnuPG/comments/o5tb6a/keyservers_are_gone/

If it fails, upload manually:

        gpg \-\-armor \-\-export 90A8________________________________AAB7

## Re-run locally events in AWS:

See ITProcessLocallyDynamoDbEvent_CheckConfig

## Release Issues

Issue with Javadoc. For instance:

- https://github.com/projectlombok/lombok/issues/2737

> mvn org.apache.maven.plugins:maven-javadoc-plugin:3.4.1:jar -Dmaven.javadoc.skip=false -PSonatype
