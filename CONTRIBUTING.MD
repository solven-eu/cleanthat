# Notes for maintainers

## Compile locally

        git clone git@github.com:solven-eu/cleanthat.git
        mvn install -Pfast

The `-Pfast` profile may be necessary to circumvent cleanthat depenending on itself to apply `spotless` on itself.

Once done (or re-done after a release), you can simply:

        mvn install

## Deploy into Production (AWS):

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

# Note for Mutator contributors

A given rule can be tested against a large set of codebases with https://github.com/solven-eu/cleanthat-showcases. See its README for more details