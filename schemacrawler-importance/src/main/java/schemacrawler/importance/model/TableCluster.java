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
 * @param anchorNode node with the highest importance score in the community
 * @param memberNodes all member table and view nodes in the community
 */
public record TableCluster(
    UUID id, DatabaseObjectNodeId anchorNode, List<DatabaseObjectNodeId> memberNodes)
    implements Serializable {

  public TableCluster {
    requireNonNull(id, "No community id provided");
    requireNonNull(anchorNode, "No anchor node provided");
    requireNonNull(memberNodes, "No member nodes provided");
    memberNodes = List.copyOf(memberNodes);
    if (!memberNodes.contains(anchorNode)) {
      throw new IllegalArgumentException(
          "Member nodes must contain the anchor node: " + anchorNode);
    }
  }
}
