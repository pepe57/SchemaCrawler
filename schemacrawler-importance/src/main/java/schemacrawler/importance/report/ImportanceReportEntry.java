/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.report;

import static java.util.Objects.requireNonNull;
import static us.fatehi.utility.Utility.requireNotBlank;

import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.TableImportance;

/** One table or view in an importance report. */
public record ImportanceReportEntry(
    DatabaseObjectVertexId vertexId, String tableFullName, TableImportance tableImportance) {

  public ImportanceReportEntry {
    requireNonNull(vertexId, "No vertex ID provided");
    requireNotBlank(tableFullName, "No table name provided");
    requireNonNull(tableImportance, "No table importance provided");
  }
}
