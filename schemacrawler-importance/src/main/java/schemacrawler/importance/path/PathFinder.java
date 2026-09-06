/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.path;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DirectedPseudograph;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;

/** Finds directed shortest paths through table foreign-key relationships. */
public final class PathFinder {

  public static final int DEFAULT_MAX_PATH_DEPTH = 5;

  private final Graph<DatabaseObjectNodeId, SchemaEdge> fallbackGraph;
  private final Graph<DatabaseObjectNodeId, SchemaEdge> foreignKeyGraph;
  private final Set<DatabaseObjectNodeId> tableNodes;

  public PathFinder(final SchemaGraphModel schemaGraphModel) {
    requireNonNull(schemaGraphModel, "No schema graph model provided");
    tableNodes = Set.copyOf(schemaGraphModel.getTableNodes());
    final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph = schemaGraphModel.getCatalogGraph();
    foreignKeyGraph = pathGraph(catalogGraph, edge -> edge.getEdgeType() == EdgeType.FOREIGN_KEY);
    fallbackGraph =
        pathGraph(
            catalogGraph,
            edge ->
                edge.getEdgeType() == EdgeType.FOREIGN_KEY
                    || edge.getEdgeType() == EdgeType.IMPLICIT_ASSOCIATION);
  }

  public PathResult findShortestPath(
      final DatabaseObjectNodeId from, final DatabaseObjectNodeId to) {
    return findShortestPath(from, to, DEFAULT_MAX_PATH_DEPTH);
  }

  /**
   * Finds the shortest dependency path up to the supplied number of hops. Non-positive values allow
   * an unlimited path depth.
   */
  public PathResult findShortestPath(
      final DatabaseObjectNodeId from, final DatabaseObjectNodeId to, final int maxPathDepth) {
    requireTable(from, "source");
    requireTable(to, "target");
    if (from.equals(to)) {
      return new PathResult(List.of(from), false);
    }

    final GraphPath<DatabaseObjectNodeId, SchemaEdge> foreignKeyPath =
        findPath(foreignKeyGraph, from, to, maxPathDepth);
    if (foreignKeyPath != null) {
      return new PathResult(foreignKeyPath.getVertexList(), false);
    }

    final GraphPath<DatabaseObjectNodeId, SchemaEdge> fallbackPath =
        findPath(fallbackGraph, from, to, maxPathDepth);
    return fallbackPath == null
        ? new PathResult(List.of(), false)
        : new PathResult(fallbackPath.getVertexList(), true);
  }

  private GraphPath<DatabaseObjectNodeId, SchemaEdge> findPath(
      final Graph<DatabaseObjectNodeId, SchemaEdge> graph,
      final DatabaseObjectNodeId from,
      final DatabaseObjectNodeId to,
      final int maxPathDepth) {
    final GraphPath<DatabaseObjectNodeId, SchemaEdge> path =
        new DijkstraShortestPath<>(graph).getPath(from, to);
    return path != null && maxPathDepth > 0 && path.getLength() > maxPathDepth ? null : path;
  }

  private Graph<DatabaseObjectNodeId, SchemaEdge> pathGraph(
      final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph,
      final Predicate<SchemaEdge> includeEdge) {
    final Graph<DatabaseObjectNodeId, SchemaEdge> pathGraph =
        new DirectedPseudograph<>(SchemaEdge.class);
    for (final DatabaseObjectNodeId tableNode : tableNodes) {
      pathGraph.addVertex(tableNode);
    }
    for (final SchemaEdge edge : catalogGraph.edgeSet()) {
      final DatabaseObjectNodeId source = catalogGraph.getEdgeSource(edge);
      final DatabaseObjectNodeId target = catalogGraph.getEdgeTarget(edge);
      if (tableNodes.contains(source) && tableNodes.contains(target) && includeEdge.test(edge)) {
        pathGraph.addEdge(source, target, edge);
      }
    }
    return new AsUnmodifiableGraph<>(pathGraph);
  }

  private void requireTable(final DatabaseObjectNodeId nodeId, final String role) {
    requireNonNull(nodeId, "No %s node provided".formatted(role));
    if (!tableNodes.contains(nodeId)) {
      throw new IllegalArgumentException(
          "<%s> node must identify a %s table in the graph".formatted(nodeId, role));
    }
  }
}
