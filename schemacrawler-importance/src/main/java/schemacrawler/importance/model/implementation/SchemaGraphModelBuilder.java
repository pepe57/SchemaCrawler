/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsUndirectedGraph;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Routine;
import schemacrawler.schema.Synonym;
import schemacrawler.schema.Table;
import us.fatehi.utility.Builder;

/** Builds the immutable dependency graph foundation from a SchemaCrawler catalog. */
public final class SchemaGraphModelBuilder implements Builder<SchemaGraphModel> {

  public static SchemaGraphModelBuilder builder(final Catalog catalog) {
    requireNonNull(catalog, "No catalog provided");
    return new SchemaGraphModelBuilder(catalog);
  }

  private final SchemaGraphAssembly assembly;

  private SchemaGraphModelBuilder(final Catalog catalog) {
    requireNonNull(catalog, "No catalog provided");

    final Collection<Table> tables = catalog.getTables();
    final Collection<Routine> routines = catalog.getRoutines();
    final Collection<Synonym> synonyms = catalog.getSynonyms();

    assembly = new SchemaGraphAssembly();

    // First add all vertices.
    for (final Table table : tables) {
      assembly.addNode(table);
    }
    for (final Routine routine : routines) {
      assembly.addNode(routine);
    }
    for (final Synonym synonym : synonyms) {
      assembly.addNode(synonym);
    }

    // Next add all edges
    for (final Table table : tables) {
      EdgeFactory.addTableEdges(assembly, table);
    }
    for (final Routine routine : routines) {
      EdgeFactory.addRoutineEdges(assembly, routine);
    }
    for (final Synonym synonym : synonyms) {
      EdgeFactory.addSynonymEdges(assembly, synonym);
    }
  }

  @Override
  public SchemaGraphModel build() {
    if (assembly.catalogGraph() == null) {
      throw new IllegalStateException(
          "Build vertices and edges before building the schema graph model");
    }
    final Map<DatabaseObjectVertexId, TableImportanceMetrics> topologyMetrics =
        GraphMetricsCalculator.calculate(assembly.catalogGraph());

    final TableImportanceInputs inputs = new TableImportanceInputs();
    for (final Map.Entry<DatabaseObjectVertexId, Table> entry :
        assembly.tablesByVertexId().entrySet()) {
      final DatabaseObjectVertexId vertexId = entry.getKey();
      final Table table = entry.getValue();
      inputs.putInputs(table, topologyMetrics.get(vertexId));
    }

    final Map<DatabaseObjectVertexId, Integer> importanceScores =
        ImportanceScoreCalculator.calculate(inputs);
    for (final Entry<DatabaseObjectVertexId, Integer> entry : importanceScores.entrySet()) {
      inputs.store(entry.getKey(), entry.getValue());
    }

    // Table cluster affinity is direction-independent, unlike schema dependencies
    final Graph<DatabaseObjectVertexId, SchemaEdge> tableSubgraph =
        new AsUndirectedGraph<>(assembly.tableSubgraph());
    final List<TableCluster> tableClusters =
        TableClusterDetector.detectClusters(tableSubgraph, assembly.tablesByVertexId());

    return new ImmutableSchemaGraphModel(
        assembly.catalogGraph(),
        assembly.tableVertexIds(),
        assembly.objectsByVertexId(),
        tableClusters);
  }
}
