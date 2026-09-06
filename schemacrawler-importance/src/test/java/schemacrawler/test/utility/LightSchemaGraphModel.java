/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.test.utility;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jgrapht.Graph;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.SchemaCommunity;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

public final class LightSchemaGraphModel implements SchemaGraphModel {

  private final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph;
  private final List<SchemaCommunity> communities;
  private final Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject;
  private final Set<DatabaseObjectNodeId> tableNodes;

  public LightSchemaGraphModel(
      final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph,
      final Set<DatabaseObjectNodeId> tableNodes,
      final Map<DatabaseObjectNodeId, ? extends DatabaseObject> nodeToObject,
      final List<SchemaCommunity> communities) {
    this.catalogGraph = catalogGraph;
    this.tableNodes = Set.copyOf(tableNodes);
    this.nodeToObject = Map.copyOf(nodeToObject);
    this.communities = List.copyOf(communities);
  }

  @Override
  public Graph<DatabaseObjectNodeId, SchemaEdge> getCatalogGraph() {
    return catalogGraph;
  }

  @Override
  public List<SchemaCommunity> getCommunities() {
    return communities;
  }

  @Override
  public Set<DatabaseObjectNodeId> getTableNodes() {
    return tableNodes;
  }

  @Override
  public Optional<DatabaseObject> lookupByVertexNodeId(final DatabaseObjectNodeId vertexNodeId) {
    if (vertexNodeId == null || !nodeToObject.containsKey(vertexNodeId)) {
      return Optional.empty();
    }
    return Optional.ofNullable(nodeToObject.get(vertexNodeId));
  }

  @Override
  public Optional<Table> lookupTableByVertexNodeId(final DatabaseObjectNodeId tableNodeId) {
    if (tableNodeId == null || tableNodeId.type() != SimpleDatabaseObjectType.table) {
      return Optional.empty();
    }
    return lookupByVertexNodeId(tableNodeId).filter(Table.class::isInstance).map(Table.class::cast);
  }

  @Override
  public Optional<TableImportance> lookupTableImportance(final DatabaseObjectNodeId tableNodeId) {
    final Optional<Table> optionalTableByVertexNodeId = lookupTableByVertexNodeId(tableNodeId);
    if (optionalTableByVertexNodeId.isEmpty()) {
      return Optional.empty();
    }
    final Table table = optionalTableByVertexNodeId.get();
    return table.getAttribute(TableImportance.class.getName());
  }
}
