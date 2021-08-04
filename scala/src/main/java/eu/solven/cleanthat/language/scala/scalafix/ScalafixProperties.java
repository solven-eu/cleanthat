package eu.solven.cleanthat.language.scala.scalafix;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Configuration for ScalaFmt formatter
 *
 * @author Benoit Lacelle
 * @see https://github.com/scalameta/scalafmt
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ScalafixProperties {
}
