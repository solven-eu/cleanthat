// https://scalacenter.github.io/scalafix/docs/rules/LeakingImplicitClassVal.html
implicit class XtensionVal(val str: String) extends AnyVal {
  def doubled: String = str + str
}
"message".str