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
import schemacrawler.importance.model.DatabaseObjectNodeId;

/** One detected table cluster entry in an importance report. */
public record ClusterReportEntry(
    UUID id,
    DatabaseObjectNodeId anchorNodeId,
    String anchorTableFullName,
    int totalClusterSize,
    List<DatabaseObjectNodeId> memberNodeIds,
    List<String> memberTableFullNames) {

  public ClusterReportEntry {
    requireNonNull(id, "No table cluster id provided");
    requireNonNull(anchorNodeId, "No anchor node id provided");
    requireNotBlank(anchorTableFullName, "No anchor table full name provided");
    requireNonNull(memberNodeIds, "No member node ids provided");
    requireNonNull(memberTableFullNames, "No member table full names provided");
    memberNodeIds = List.copyOf(memberNodeIds);
    memberTableFullNames = List.copyOf(memberTableFullNames);
    if (totalClusterSize < 0) {
      throw new IllegalArgumentException("Total table cluster size cannot be negative");
    }
  }
}
