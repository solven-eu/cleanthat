---

name: Additional Static Mutator XXX
about: Request an additional mutator for a well-known rule (already reported by PMD, CheckStyle, Sonar, etc)
title: Additional Static Mutator YYY
labels: feature_request
assignees: ''

---

If existing, a reference to the rule from the external system :

- https://rules.sonarsource.com/java/RSPEC-1116
- https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#emptycontrolstatement
- https://javadoc.io/static/com.puppycrawl.tools/checkstyle/8.37/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html

Please also provide code examples `before` -> `after`.

---

You want also want to implement a mutator by your self:

- Consider `eu.solven.cleanthat.engine.java.refactorer.mutators.EmptyControlStatement` as an example
- Add tests in `eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.EmptyControlStatementCases`
- Wire tests through `eu.solven.cleanthat.engine.java.refactorer.cases.TestEmptyControlStatementCases`
- Open a PR from a personal fork, allowing maintainers to edit your PR.
- Thanks!

