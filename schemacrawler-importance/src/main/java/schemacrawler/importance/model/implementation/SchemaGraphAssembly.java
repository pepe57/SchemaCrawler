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
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DirectedPseudograph;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.Table;

/** Holds the mutable graph state and typed catalog indexes used while building a graph model. */
final class SchemaGraphAssembly {

  private final Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph;
  private final Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject;
  private final Map<DatabaseObjectNodeId, Table> tablesByNode;

  SchemaGraphAssembly() {
    catalogGraph = new DirectedPseudograph<>(SchemaEdge.class);
    nodeToObject = new LinkedHashMap<>();
    tablesByNode = new LinkedHashMap<>();
  }

  boolean addEdge(final DatabaseObject source, final DatabaseObject target, final SchemaEdge edge) {
    if (source == null || target == null) {
      return false;
    }
    final DatabaseObjectNodeId sourceNode = DatabaseObjectNodeIdUtility.create(source);
    final DatabaseObjectNodeId targetNode = DatabaseObjectNodeIdUtility.create(target);
    if (!catalogGraph.containsVertex(sourceNode) || !catalogGraph.containsVertex(targetNode)) {
      return false;
    }
    catalogGraph.addEdge(sourceNode, targetNode, edge);
    return true;
  }

  void addNode(final DatabaseObject databaseObject) {
    requireNonNull(databaseObject, "No database object provided");
    final DatabaseObjectNodeId nodeId = DatabaseObjectNodeIdUtility.create(databaseObject);
    catalogGraph.addVertex(nodeId);
    nodeToObject.put(nodeId, databaseObject);
    if (databaseObject instanceof final Table table) {
      tablesByNode.put(nodeId, table);
    }
  }

  Graph<DatabaseObjectNodeId, SchemaEdge> catalogGraph() {
    return catalogGraph;
  }

  Map<DatabaseObjectNodeId, DatabaseObject> nodeToObject() {
    return nodeToObject;
  }

  Set<DatabaseObjectNodeId> tableNodes() {
    return tablesByNode.keySet();
  }

  Map<DatabaseObjectNodeId, Table> tablesByNode() {
    return tablesByNode;
  }

  Graph<DatabaseObjectNodeId, SchemaEdge> tableSubgraph() {
    return new AsSubgraph<>(catalogGraph, tablesByNode.keySet());
  }
}
