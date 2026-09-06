/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jgrapht.Graph;
import schemacrawler.schema.DatabaseObject;

/** Immutable graph and object lookup data built from a SchemaCrawler catalog. */
public interface SchemaGraphModel {

  /** Gets the immutable, ordered communities calculated when this model was built. */
  List<SchemaCommunity> getCommunities();

  Graph<DatabaseObjectNodeId, SchemaEdge> getCatalogGraph();

  Optional<DatabaseObject> lookupByVertexNodeId(DatabaseObjectNodeId vertexNodeId);

  Optional<TableImportance> lookupTableImportance(DatabaseObjectNodeId tableNodeId);

  Set<DatabaseObjectNodeId> getTableNodes();
}
