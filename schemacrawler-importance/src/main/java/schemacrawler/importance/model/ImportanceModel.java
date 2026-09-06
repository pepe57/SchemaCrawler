/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jgrapht.Graph;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;

/** Immutable graph and object lookup data built from a SchemaCrawler catalog. */
public interface ImportanceModel extends Serializable {

  Graph<DatabaseObjectVertexId, SchemaEdge> getCatalogGraph();

  /** Gets the immutable, ordered table clusters calculated when this model was built. */
  List<TableCluster> getTableClusters();

  Set<DatabaseObjectVertexId> getTableVertexIds();

  Optional<DatabaseObject> lookupByVertexId(DatabaseObjectVertexId vertexId);

  Optional<Table> lookupTableByVertexId(DatabaseObjectVertexId tableVertexId);

  Optional<TableImportance> lookupTableImportance(DatabaseObjectVertexId tableVertexId);
}
