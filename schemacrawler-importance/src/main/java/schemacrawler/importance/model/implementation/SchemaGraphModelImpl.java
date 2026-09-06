/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsUnmodifiableGraph;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;

final class SchemaGraphModelImpl implements SchemaGraphModel {

  @Serial private static final long serialVersionUID = -2772896374981270459L;

  private final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph;
  private final List<TableCluster> tableClusters;
  private final Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject;
  private final Set<DatabaseObjectNodeId> tableNodes;

  SchemaGraphModelImpl(
      final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph,
      final Set<DatabaseObjectNodeId> tableNodes,
      final Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject,
      final List<TableCluster> tableClusters) {
    this.catalogGraph =
        new AsUnmodifiableGraph<>(
            Objects.requireNonNull(catalogGraph, "No catalog graph provided"));
    this.tableNodes = Collections.unmodifiableSet(new LinkedHashSet<>(tableNodes));
    this.nodeToObject = Map.copyOf(nodeToObject);
    this.tableClusters = List.copyOf(tableClusters);
  }

  @Override
  public Graph<DatabaseObjectNodeId, SchemaEdge> getCatalogGraph() {
    return catalogGraph;
  }

  @Override
  public List<TableCluster> getTableClusters() {
    return tableClusters;
  }

  @Override
  public Set<DatabaseObjectNodeId> getTableNodes() {
    return tableNodes;
  }

  @Override
  public Optional<DatabaseObject> lookupByVertexNodeId(final DatabaseObjectNodeId vertexNodeId) {
    if (vertexNodeId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(nodeToObject.get(vertexNodeId));
  }

  @Override
  public Optional<Table> lookupTableByVertexNodeId(final DatabaseObjectNodeId tableNodeId) {
    return lookupByVertexNodeId(tableNodeId).filter(Table.class::isInstance).map(Table.class::cast);
  }

  @Override
  public Optional<TableImportance> lookupTableImportance(final DatabaseObjectNodeId tableNodeId) {
    return lookupTableByVertexNodeId(tableNodeId)
        .map(table -> table.<TableImportance>getAttribute(TableImportance.class.getName()));
  }
}
