/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * A detected cluster of database tables in the schema graph.
 *
 * @param id synthetic unique identifier for the cluster
 * @param anchorVertexId vertex with the highest importance score in the table cluster
 * @param memberVertexIds all member table and view vertex IDs in the table cluster
 */
public record TableCluster(
    UUID id, DatabaseObjectVertexId anchorVertexId, List<DatabaseObjectVertexId> memberVertexIds)
    implements Serializable {

  public TableCluster {
    requireNonNull(id, "No table cluster id provided");
    requireNonNull(anchorVertexId, "No anchor vertex ID provided");
    requireNonNull(memberVertexIds, "No member vertex IDs provided");
    memberVertexIds = List.copyOf(memberVertexIds);
    if (!memberVertexIds.contains(anchorVertexId)) {
      throw new IllegalArgumentException(
          "Member vertex IDs must contain the anchor vertex ID: " + anchorVertexId);
    }
  }
}
