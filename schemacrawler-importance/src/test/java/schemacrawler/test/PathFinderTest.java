package schemacrawler.test;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.path.PathFinder;
import schemacrawler.importance.path.PathResult;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.test.utility.LightSchemaGraphModel;
import schemacrawler.test.utility.crawl.LightTable;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class PathFinderTest {

  private record Edge(
      DatabaseObjectVertexId source, DatabaseObjectVertexId target, SchemaEdge edge) {}

  private static Edge edge(
      final DatabaseObjectVertexId source,
      final DatabaseObjectVertexId target,
      final EdgeType edgeType) {
    return new Edge(source, target, new SchemaEdge(edgeType, new NamedObjectKey(edgeType.name())));
  }

  private static PathFinder pathFinder(
      final List<DatabaseObjectVertexId> nodes, final Edge... graphEdges) {
    final Graph<DatabaseObjectVertexId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    nodes.forEach(graph::addVertex);
    for (final Edge edge : graphEdges) {
      graph.addEdge(edge.source(), edge.target(), edge.edge());
    }
    final Set<DatabaseObjectVertexId> tableVertexIds = Set.copyOf(nodes);
    final Map<DatabaseObjectVertexId, LightTable> nodeToTable =
        nodes.stream().collect(toMap(identity(), node -> new LightTable(node.key().toString())));
    return new PathFinder(new LightSchemaGraphModel(graph, tableVertexIds, nodeToTable, List.of()));
  }

  private static DatabaseObjectVertexId table(final String name) {
    return new DatabaseObjectVertexId(
        new NamedObjectKey("PUBLIC", name), SimpleDatabaseObjectType.table);
  }

  @Test
  void allowsUnlimitedPathDepth() {
    final DatabaseObjectVertexId table1 = table("TABLE1");
    final DatabaseObjectVertexId table2 = table("TABLE2");
    final DatabaseObjectVertexId table3 = table("TABLE3");
    final DatabaseObjectVertexId table4 = table("TABLE4");
    final DatabaseObjectVertexId table5 = table("TABLE5");
    final DatabaseObjectVertexId table6 = table("TABLE6");
    final DatabaseObjectVertexId table7 = table("TABLE7");
    final PathFinder pathFinder =
        pathFinder(
            List.of(table1, table2, table3, table4, table5, table6, table7),
            edge(table1, table2, EdgeType.FOREIGN_KEY),
            edge(table2, table3, EdgeType.FOREIGN_KEY),
            edge(table3, table4, EdgeType.FOREIGN_KEY),
            edge(table4, table5, EdgeType.FOREIGN_KEY),
            edge(table5, table6, EdgeType.FOREIGN_KEY),
            edge(table6, table7, EdgeType.FOREIGN_KEY));

    assertThat(
        pathFinder.findShortestPath(table1, table7, -1).path(),
        contains(table1, table2, table3, table4, table5, table6, table7));
  }

  @Test
  void cachesOnlyEligibleTableEdges() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final DatabaseObjectVertexId procedure =
        new DatabaseObjectVertexId(
            new NamedObjectKey("PUBLIC", "REFRESH_ORDERS"), SimpleDatabaseObjectType.procedure);
    final Graph<DatabaseObjectVertexId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(orders);
    graph.addVertex(customers);
    graph.addVertex(procedure);
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.IMPLICIT_ASSOCIATION).edge());
    graph.addEdge(procedure, customers, edge(procedure, customers, EdgeType.FOREIGN_KEY).edge());
    final Map<DatabaseObjectVertexId, LightTable> nodeToTable =
        Map.of(
            orders,
            new LightTable(orders.key().toString()),
            customers,
            new LightTable(customers.key().toString()));
    final PathFinder pathFinder =
        new PathFinder(
            new LightSchemaGraphModel(graph, Set.of(orders, customers), nodeToTable, List.of()));

    assertThat(pathFinder.findShortestPath(orders, customers).usesImpliedAssociations(), is(false));
    assertThrows(
        IllegalArgumentException.class, () -> pathFinder.findShortestPath(procedure, customers));
  }

  @Test
  void cachesTablePathTopology() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final Graph<DatabaseObjectVertexId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(orders);
    graph.addVertex(customers);
    final Map<DatabaseObjectVertexId, LightTable> nodeToTable =
        Map.of(
            orders,
            new LightTable(orders.key().toString()),
            customers,
            new LightTable(customers.key().toString()));
    final PathFinder pathFinder =
        new PathFinder(
            new LightSchemaGraphModel(graph, Set.of(orders, customers), nodeToTable, List.of()));

    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());

    assertThat(pathFinder.findShortestPath(orders, customers).path(), empty());
  }

  @Test
  void fallsBackToImplicitAssociations() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final PathFinder pathFinder =
        pathFinder(
            List.of(orders, customers), edge(orders, customers, EdgeType.IMPLICIT_ASSOCIATION));

    final PathResult result = pathFinder.findShortestPath(orders, customers);

    assertThat(result.path(), contains(orders, customers));
    assertThat(result.usesImpliedAssociations(), is(true));
  }

  @Test
  void handlesNoPathSameNodeAndUnsupportedNodes() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final DatabaseObjectVertexId procedure =
        new DatabaseObjectVertexId(
            new NamedObjectKey("PUBLIC", "REFRESH_ORDERS"), SimpleDatabaseObjectType.procedure);
    final PathFinder pathFinder = pathFinder(List.of(orders, customers));

    assertThat(pathFinder.findShortestPath(orders, customers).path(), empty());
    assertThat(pathFinder.findShortestPath(orders, orders).path(), contains(orders));
    assertThrows(
        IllegalArgumentException.class, () -> pathFinder.findShortestPath(procedure, customers));
  }

  @Test
  void prefersAnAvailableForeignKeyPath() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final DatabaseObjectVertexId countries = table("COUNTRIES");
    final PathFinder pathFinder =
        pathFinder(
            List.of(orders, customers, countries),
            edge(orders, customers, EdgeType.FOREIGN_KEY),
            edge(customers, countries, EdgeType.FOREIGN_KEY),
            edge(orders, countries, EdgeType.IMPLICIT_ASSOCIATION));

    final PathResult result = pathFinder.findShortestPath(orders, countries);

    assertThat(result.path(), contains(orders, customers, countries));
    assertThat(result.usesImpliedAssociations(), is(false));
  }

  @Test
  void returnsThePathWithFewestEdges() {
    final DatabaseObjectVertexId orders = table("ORDERS");
    final DatabaseObjectVertexId customers = table("CUSTOMERS");
    final DatabaseObjectVertexId countries = table("COUNTRIES");
    final DatabaseObjectVertexId regions = table("REGIONS");
    final PathFinder pathFinder =
        pathFinder(
            List.of(orders, customers, countries, regions),
            edge(orders, customers, EdgeType.FOREIGN_KEY),
            edge(customers, countries, EdgeType.FOREIGN_KEY),
            edge(orders, regions, EdgeType.FOREIGN_KEY),
            edge(regions, countries, EdgeType.FOREIGN_KEY),
            edge(orders, countries, EdgeType.FOREIGN_KEY));

    assertThat(pathFinder.findShortestPath(orders, countries).path(), contains(orders, countries));
  }
}
