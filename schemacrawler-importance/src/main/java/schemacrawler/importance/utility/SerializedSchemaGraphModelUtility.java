/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.utility;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.utility.SerializedObjectInputStream;
import us.fatehi.utility.UtilityMarker;

@UtilityMarker
public final class SerializedSchemaGraphModelUtility {

  private static final List<Pattern> SCHEMA_GRAPH_MODEL_CLASS_PATTERNS =
      List.of(
          Pattern.compile("org\\.jgrapht\\..*"),
          Pattern.compile("us\\.fatehi\\.utility\\.[A-Z].*"),
          Pattern.compile("us\\.fatehi\\.utility\\.property\\.[A-Z].*"),
          Pattern.compile("schemacrawler\\.(schema(crawler)?|crawl)\\.[A-Z].*"),
          Pattern.compile("schemacrawler\\.importance\\.model\\.[A-Z].*"),
          Pattern.compile("schemacrawler\\.importance\\.model\\.implementation\\.[A-Z].*"),
          Pattern.compile("schemacrawler\\.tools\\.utility\\.[A-Z].*"),
          Pattern.compile("schemacrawler\\.utility\\.MetaDataUtility\\$SimpleDatabaseObjectType"),
          Pattern.compile("schemacrawler\\.[A-Z].*"),
          Pattern.compile("(\\[L)?java\\.(lang|util)\\..*"),
          Pattern.compile("java\\.(sql|math|time|net)\\..*"),
          Pattern.compile("\\[[BC]"));

  public static SchemaGraphModel readSchemaGraphModel(final InputStream in) {
    requireNonNull(in, "No input stream provided");
    return SerializedObjectInputStream.read(in, SCHEMA_GRAPH_MODEL_CLASS_PATTERNS);
  }

  public static void saveSchemaGraphModel(
      final SchemaGraphModel schemaGraphModel, final OutputStream out) {
    requireNonNull(schemaGraphModel, "No schema graph model provided");
    requireNonNull(out, "No output stream provided");
    SerializedObjectInputStream.save(schemaGraphModel, out);
  }

  private SerializedSchemaGraphModelUtility() {
    // Prevent instantiation
  }
}
