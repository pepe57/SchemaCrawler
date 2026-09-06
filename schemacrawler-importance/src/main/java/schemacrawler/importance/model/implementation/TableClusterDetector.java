/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.jgrapht.Graph;
import org.jgrapht.alg.clustering.LabelPropagationClustering;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.Table;
import us.fatehi.utility.UtilityMarker;

/** Detects table clusters over schema graph. */
@UtilityMarker
final class TableClusterDetector {

  private static final int MIN_CLUSTER_SIZE = 3;

  static List<TableCluster> detectClusters(
      final Graph<DatabaseObjectVertexId, SchemaEdge> tableSubgraph,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    requireNonNull(tableSubgraph, "No table graph provided");
    requireNonNull(tablesByVertexId, "No table vertex ID map provided");

    if (tableSubgraph.vertexSet().isEmpty()) {
      return List.of();
    }

    final List<TableCluster> tableClusters = createTableClusters(tableSubgraph, tablesByVertexId);
    return sortTableClusters(tableClusters, tablesByVertexId);
  }

  private static TableCluster createTableCluster(
      final Set<DatabaseObjectVertexId> cluster,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    // Sorting establishes both the anchor and deterministic member output.
    final List<DatabaseObjectVertexId> sortedMembers = new ArrayList<>(cluster);
    sortedMembers.sort(
        Comparator.comparingInt(
                (final DatabaseObjectVertexId vertexId) ->
                    getImportanceScore(vertexId, tablesByVertexId))
            .reversed()
            .thenComparing(vertexId -> getTableFullName(vertexId, tablesByVertexId)));
    final DatabaseObjectVertexId anchorVertexId = sortedMembers.get(0);
    final String anchorFullName = getTableFullName(anchorVertexId, tablesByVertexId);
    final UUID clusterId =
        UUID.nameUUIDFromBytes(("cluster:" + anchorFullName).getBytes(StandardCharsets.UTF_8));
    return new TableCluster(clusterId, anchorVertexId, sortedMembers);
  }

  private static List<TableCluster> createTableClusters(
      final Graph<DatabaseObjectVertexId, SchemaEdge> tableSubgraph,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    final ClusteringAlgorithm.Clustering<DatabaseObjectVertexId> clustering =
        new LabelPropagationClustering<>(tableSubgraph, 100, new Random(0)).getClustering();

    final List<TableCluster> tableClusters = new ArrayList<>();
    for (final Set<DatabaseObjectVertexId> cluster : clustering.getClusters()) {
      if (cluster != null && cluster.size() >= MIN_CLUSTER_SIZE) {
        tableClusters.add(createTableCluster(cluster, tablesByVertexId));
      }
    }
    return tableClusters;
  }

  private static int getImportanceScore(
      final DatabaseObjectVertexId vertexId,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    final Table table = tablesByVertexId.get(vertexId);
    if (table != null) {
      final TableImportance importance = table.getAttribute(TableImportance.class.getName());
      if (importance != null) {
        return importance.importanceScore();
      }
    }
    return 0;
  }

  private static String getTableFullName(
      final DatabaseObjectVertexId vertexId,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    final Table table = tablesByVertexId.get(vertexId);
    if (table != null && table.getFullName() != null) {
      return table.getFullName();
    }
    return vertexId.key().toString();
  }

  private static List<TableCluster> sortTableClusters(
      final List<TableCluster> tableClusters,
      final Map<DatabaseObjectVertexId, Table> tablesByVertexId) {
    final List<TableCluster> sortedTableClusters = new ArrayList<>(tableClusters);
    sortedTableClusters.sort(
        Comparator.comparingInt(
                (final TableCluster tableCluster) ->
                    getImportanceScore(tableCluster.anchorVertexId(), tablesByVertexId))
            .reversed()
            .thenComparing(
                tableCluster -> getTableFullName(tableCluster.anchorVertexId(), tablesByVertexId)));
    return List.copyOf(sortedTableClusters);
  }

  private TableClusterDetector() {
    // Prevent instantiation
  }
}
