/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;
import schemacrawler.tools.utility.TableImportanceUtility;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;
import us.fatehi.utility.Builder;

/** Builds the immutable dependency graph foundation from a SchemaCrawler catalog. */
public final class SchemaGraphModelBuilder implements Builder<SchemaGraphModel> {

  public static SchemaGraphModelBuilder builder(final Catalog catalog) {
    requireNonNull(catalog, "No catalog provided");
    return new SchemaGraphModelBuilder(catalog);
  }

  private final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph;
  private final Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject;
  private final Set<DatabaseObjectNodeId> tableNodes;

  private SchemaGraphModelBuilder(final Catalog catalog) {
    requireNonNull(catalog, "No catalog provided");

    catalogGraph = new DirectedPseudograph<>(SchemaEdge.class);
    nodeToObject = new LinkedHashMap<>();
    tableNodes = new LinkedHashSet<>();

    for (final Table table : catalog.getTables()) {
      addNode(table);
    }
    for (final schemacrawler.schema.Routine routine : catalog.getRoutines()) {
      addNode(routine);
    }
    for (final schemacrawler.schema.Synonym synonym : catalog.getSynonyms()) {
      addNode(synonym);
    }
    EdgeFactory.addEdges(
        catalog.getTables(), catalog.getRoutines(), catalog.getSynonyms(), catalogGraph);
  }

  @Override
  public SchemaGraphModel build() {
    if (catalogGraph == null) {
      throw new IllegalStateException(
          "Build nodes and edges before building the schema graph model");
    }
    final Map<DatabaseObjectNodeId, TableImportanceMetrics> topologyMetrics =
        GraphMetricsCalculator.calculate(catalogGraph);

    final TableImportanceInputs inputs = new TableImportanceInputs();
    for (final Map.Entry<DatabaseObjectNodeId, TableImportanceMetrics> entry :
        topologyMetrics.entrySet()) {
      inputs.put(entry.getKey(), entry.getValue());
    }
    for (final Map.Entry<DatabaseObjectNodeId, DatabaseObject> entry : nodeToObject.entrySet()) {
      if (entry.getValue() instanceof final Table table) {
        inputs.put(entry.getKey(), TableImportanceUtility.tableTraitsfrom(table));
        inputs.put(entry.getKey(), TableImportanceUtility.tableCountsfrom(table));
      }
    }

    final Map<DatabaseObjectNodeId, Integer> importanceScores =
        ImportanceScoreCalculator.calculate(inputs);

    storeTableImportance(inputs, importanceScores);
    final List<TableCluster> tableClusters =
        CommunityDetector.detectCommunities(catalogGraph, tableNodes, nodeToObject);
    return new ImmutableSchemaGraphModel(catalogGraph, tableNodes, nodeToObject, tableClusters);
  }

  private void addNode(final DatabaseObject databaseObject) {
    final DatabaseObjectNodeId nodeId = DatabaseObjectNodeIdUtility.create(databaseObject);
    catalogGraph.addVertex(nodeId);
    nodeToObject.put(nodeId, databaseObject);
    if (nodeId.type() == SimpleDatabaseObjectType.table
        || nodeId.type() == SimpleDatabaseObjectType.view) {
      tableNodes.add(nodeId);
    }
  }

  private void storeTableImportance(
      final TableImportanceInputs inputs,
      final Map<DatabaseObjectNodeId, Integer> importanceScores) {
    for (final Map.Entry<DatabaseObjectNodeId, DatabaseObject> entry : nodeToObject.entrySet()) {
      final DatabaseObjectNodeId nodeId = entry.getKey();
      if (entry.getValue() instanceof final Table table) {
        final TableImportanceInputs.TableImportanceInput tableInputs = inputs.get(nodeId);
        table.setAttribute(
            TableImportance.class.getName(),
            new TableImportance(
                importanceScores.get(nodeId),
                tableInputs.importanceMetrics(),
                tableInputs.tableTraits(),
                tableInputs.tableCounts()));
      }
    }
  }
}
