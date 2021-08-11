package eu.solven.cleanthat.language.scala.scalafmt;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Configuration for ScalaFmt formatter
 * 
 * See https://github.com/scalameta/scalafmt
 *
 * @author Benoit Lacelle
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ScalafmtProperties {
}
