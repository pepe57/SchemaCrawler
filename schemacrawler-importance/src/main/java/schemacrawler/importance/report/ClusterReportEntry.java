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

import java.util.List;
import java.util.UUID;
import schemacrawler.importance.model.DatabaseObjectVertexId;

/** One detected table cluster entry in an importance report. */
public record ClusterReportEntry(
    UUID id,
    DatabaseObjectVertexId anchorVertexId,
    String anchorTableFullName,
    int totalClusterSize,
    List<DatabaseObjectVertexId> memberVertexIds,
    List<String> memberTableFullNames) {

  public ClusterReportEntry {
    requireNonNull(id, "No table cluster id provided");
    requireNonNull(anchorVertexId, "No anchor vertex ID provided");
    requireNotBlank(anchorTableFullName, "No anchor table full name provided");
    requireNonNull(memberVertexIds, "No member vertex IDs provided");
    requireNonNull(memberTableFullNames, "No member table full names provided");
    memberVertexIds = List.copyOf(memberVertexIds);
    memberTableFullNames = List.copyOf(memberTableFullNames);
    if (totalClusterSize < 0) {
      throw new IllegalArgumentException("Total table cluster size cannot be negative");
    }
  }
}
