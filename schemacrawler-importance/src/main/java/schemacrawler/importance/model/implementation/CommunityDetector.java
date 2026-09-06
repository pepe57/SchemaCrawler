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
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.schema.Table;
import us.fatehi.utility.UtilityMarker;

/** Detects functional domain communities over schema graph table and view nodes. */
@UtilityMarker
final class CommunityDetector {

  private static final int MIN_CLUSTER_SIZE = 3;

  static List<TableCluster> detectCommunities(
      final Graph<DatabaseObjectNodeId, SchemaEdge> tableSubgraph,
      final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    requireNonNull(tableSubgraph, "No table graph provided");
    requireNonNull(tablesByNode, "No table-node map provided");

    if (tableSubgraph.vertexSet().isEmpty()) {
      return List.of();
    }

    final List<TableCluster> tableClusters = createTableClusters(tableSubgraph, tablesByNode);
    return sortTableClusters(tableClusters, tablesByNode);
  }

  private static TableCluster createTableCluster(
      final Set<DatabaseObjectNodeId> cluster,
      final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    // Sorting establishes both the anchor and deterministic member output.
    final List<DatabaseObjectNodeId> sortedMembers = new ArrayList<>(cluster);
    sortedMembers.sort(
        Comparator.comparingInt(
                (final DatabaseObjectNodeId nodeId) -> getImportanceScore(nodeId, tablesByNode))
            .reversed()
            .thenComparing(nodeId -> getTableFullName(nodeId, tablesByNode)));
    final DatabaseObjectNodeId anchorNode = sortedMembers.get(0);
    final String anchorFullName = getTableFullName(anchorNode, tablesByNode);
    final UUID communityId =
        UUID.nameUUIDFromBytes(("community:" + anchorFullName).getBytes(StandardCharsets.UTF_8));
    return new TableCluster(communityId, anchorNode, sortedMembers);
  }

  private static List<TableCluster> createTableClusters(
      final Graph<DatabaseObjectNodeId, SchemaEdge> tableSubgraph,
      final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    final ClusteringAlgorithm.Clustering<DatabaseObjectNodeId> clustering =
        new LabelPropagationClustering<>(tableSubgraph, 100, new Random(0)).getClustering();

    final List<TableCluster> tableClusters = new ArrayList<>();
    for (final Set<DatabaseObjectNodeId> cluster : clustering.getClusters()) {
      if (cluster != null && cluster.size() >= MIN_CLUSTER_SIZE) {
        tableClusters.add(createTableCluster(cluster, tablesByNode));
      }
    }
    return tableClusters;
  }

  private static int getImportanceScore(
      final DatabaseObjectNodeId nodeId, final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    final Table table = tablesByNode.get(nodeId);
    if (table != null) {
      final TableImportance importance = table.getAttribute(TableImportance.class.getName());
      if (importance != null) {
        return importance.importanceScore();
      }
    }
    return 0;
  }

  private static String getTableFullName(
      final DatabaseObjectNodeId nodeId, final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    final Table table = tablesByNode.get(nodeId);
    if (table != null && table.getFullName() != null) {
      return table.getFullName();
    }
    return nodeId.key().toString();
  }

  private static List<TableCluster> sortTableClusters(
      final List<TableCluster> tableClusters, final Map<DatabaseObjectNodeId, Table> tablesByNode) {
    final List<TableCluster> sortedTableClusters = new ArrayList<>(tableClusters);
    sortedTableClusters.sort(
        Comparator.comparingInt(
                (final TableCluster tableCluster) ->
                    getImportanceScore(tableCluster.anchorNode(), tablesByNode))
            .reversed()
            .thenComparing(
                tableCluster -> getTableFullName(tableCluster.anchorNode(), tablesByNode)));
    return List.copyOf(sortedTableClusters);
  }

  private CommunityDetector() {
    // Prevent instantiation
  }
}
