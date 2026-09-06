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
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

public final class LightSchemaGraphModel implements SchemaGraphModel {

  private static final long serialVersionUID = -7403567390131889799L;

  private final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph;
  private final List<TableCluster> tableClusters;
  private final Map<DatabaseObjectVertexId, DatabaseObject> objectsByVertexId;
  private final Set<DatabaseObjectVertexId> tableVertexIds;

  public LightSchemaGraphModel(
      final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph,
      final Set<DatabaseObjectVertexId> tableVertexIds,
      final Map<DatabaseObjectVertexId, ? extends DatabaseObject> objectsByVertexId,
      final List<TableCluster> tableClusters) {
    this.catalogGraph = catalogGraph;
    this.tableVertexIds = Set.copyOf(tableVertexIds);
    this.objectsByVertexId = Map.copyOf(objectsByVertexId);
    this.tableClusters = List.copyOf(tableClusters);
  }

  @Override
  public Graph<DatabaseObjectVertexId, SchemaEdge> getCatalogGraph() {
    return catalogGraph;
  }

  @Override
  public List<TableCluster> getTableClusters() {
    return tableClusters;
  }

  @Override
  public Set<DatabaseObjectVertexId> getTableVertexIds() {
    return tableVertexIds;
  }

  @Override
  public Optional<DatabaseObject> lookupByVertexId(final DatabaseObjectVertexId vertexId) {
    if (vertexId == null || !objectsByVertexId.containsKey(vertexId)) {
      return Optional.empty();
    }
    return Optional.ofNullable(objectsByVertexId.get(vertexId));
  }

  @Override
  public Optional<Table> lookupTableByVertexId(final DatabaseObjectVertexId tableVertexId) {
    if (tableVertexId == null || tableVertexId.type() != SimpleDatabaseObjectType.table) {
      return Optional.empty();
    }
    return lookupByVertexId(tableVertexId).filter(Table.class::isInstance).map(Table.class::cast);
  }

  @Override
  public Optional<TableImportance> lookupTableImportance(
      final DatabaseObjectVertexId tableVertexId) {
    final Optional<Table> optionalTableByVertexId = lookupTableByVertexId(tableVertexId);
    if (optionalTableByVertexId.isEmpty()) {
      return Optional.empty();
    }
    final Table table = optionalTableByVertexId.get();
    return table.getAttribute(TableImportance.class.getName());
  }
}
