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
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.ImportanceModel;
import schemacrawler.importance.model.SchemaEdge;

/** Finds directed shortest paths through table foreign-key relationships. */
public final class PathFinder {

  public static final int DEFAULT_MAX_PATH_DEPTH = 5;

  private final Graph<DatabaseObjectVertexId, SchemaEdge> fallbackGraph;
  private final Graph<DatabaseObjectVertexId, SchemaEdge> foreignKeyGraph;
  private final Set<DatabaseObjectVertexId> tableVertexIds;

  public PathFinder(final ImportanceModel importanceModel) {
    requireNonNull(importanceModel, "No importance model provided");
    tableVertexIds = Set.copyOf(importanceModel.getTableVertexIds());
    final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph =
        importanceModel.getCatalogGraph();
    foreignKeyGraph = pathGraph(catalogGraph, edge -> edge.getEdgeType() == EdgeType.FOREIGN_KEY);
    fallbackGraph =
        pathGraph(
            catalogGraph,
            edge ->
                edge.getEdgeType() == EdgeType.FOREIGN_KEY
                    || edge.getEdgeType() == EdgeType.IMPLICIT_ASSOCIATION);
  }

  public PathResult findShortestPath(
      final DatabaseObjectVertexId from, final DatabaseObjectVertexId to) {
    return findShortestPath(from, to, DEFAULT_MAX_PATH_DEPTH);
  }

  /**
   * Finds the shortest dependency path up to the supplied number of hops. Non-positive values allow
   * an unlimited path depth.
   */
  public PathResult findShortestPath(
      final DatabaseObjectVertexId from, final DatabaseObjectVertexId to, final int maxPathDepth) {
    requireTable(from, "source");
    requireTable(to, "target");
    if (from.equals(to)) {
      return new PathResult(List.of(from), false);
    }

    final GraphPath<DatabaseObjectVertexId, SchemaEdge> foreignKeyPath =
        findPath(foreignKeyGraph, from, to, maxPathDepth);
    if (foreignKeyPath != null) {
      return new PathResult(foreignKeyPath.getVertexList(), false);
    }

    final GraphPath<DatabaseObjectVertexId, SchemaEdge> fallbackPath =
        findPath(fallbackGraph, from, to, maxPathDepth);
    return fallbackPath == null
        ? new PathResult(List.of(), false)
        : new PathResult(fallbackPath.getVertexList(), true);
  }

  private GraphPath<DatabaseObjectVertexId, SchemaEdge> findPath(
      final Graph<DatabaseObjectVertexId, SchemaEdge> graph,
      final DatabaseObjectVertexId from,
      final DatabaseObjectVertexId to,
      final int maxPathDepth) {
    final GraphPath<DatabaseObjectVertexId, SchemaEdge> path =
        new DijkstraShortestPath<>(graph).getPath(from, to);
    return path != null && maxPathDepth > 0 && path.getLength() > maxPathDepth ? null : path;
  }

  private Graph<DatabaseObjectVertexId, SchemaEdge> pathGraph(
      final Graph<DatabaseObjectVertexId, SchemaEdge> catalogGraph,
      final Predicate<SchemaEdge> includeEdge) {
    final Graph<DatabaseObjectVertexId, SchemaEdge> pathGraph =
        new DirectedPseudograph<>(SchemaEdge.class);
    for (final DatabaseObjectVertexId tableVertexId : tableVertexIds) {
      pathGraph.addVertex(tableVertexId);
    }
    for (final SchemaEdge edge : catalogGraph.edgeSet()) {
      final DatabaseObjectVertexId source = catalogGraph.getEdgeSource(edge);
      final DatabaseObjectVertexId target = catalogGraph.getEdgeTarget(edge);
      if (tableVertexIds.contains(source)
          && tableVertexIds.contains(target)
          && includeEdge.test(edge)) {
        pathGraph.addEdge(source, target, edge);
      }
    }
    return new AsUnmodifiableGraph<>(pathGraph);
  }

  private void requireTable(final DatabaseObjectVertexId vertexId, final String role) {
    requireNonNull(vertexId, "No %s vertex ID provided".formatted(role));
    if (!tableVertexIds.contains(vertexId)) {
      throw new IllegalArgumentException(
          "<%s> vertex ID must identify a %s table in the graph".formatted(vertexId, role));
    }
  }
}
