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
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;

final class ImmutableSchemaGraphModel implements SchemaGraphModel {

  @Serial private static final long serialVersionUID = -2772896374981270459L;

  private final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph;
  private final List<TableCluster> tableClusters;
  private final Map<DatabaseObjectVertexId, DatabaseObject> objectsByVertexId;
  private final Set<DatabaseObjectVertexId> tableVertexIds;

  ImmutableSchemaGraphModel(
      final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph,
      final Set<DatabaseObjectVertexId> tableVertexIds,
      final Map<DatabaseObjectVertexId, DatabaseObject> objectsByVertexId,
      final List<TableCluster> tableClusters) {
    this.catalogGraph =
        new AsUnmodifiableGraph<>(
            Objects.requireNonNull(catalogGraph, "No catalog graph provided"));
    this.tableVertexIds = Collections.unmodifiableSet(new LinkedHashSet<>(tableVertexIds));
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
    if (vertexId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(objectsByVertexId.get(vertexId));
  }

  @Override
  public Optional<Table> lookupTableByVertexId(final DatabaseObjectVertexId tableVertexId) {
    return lookupByVertexId(tableVertexId).filter(Table.class::isInstance).map(Table.class::cast);
  }

  @Override
  public Optional<TableImportance> lookupTableImportance(
      final DatabaseObjectVertexId tableVertexId) {
    return lookupTableByVertexId(tableVertexId)
        .map(table -> table.<TableImportance>getAttribute(TableImportance.class.getName()));
  }
}
