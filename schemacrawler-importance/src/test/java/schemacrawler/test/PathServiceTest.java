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
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.service.PathResult;
import schemacrawler.importance.service.PathService;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.test.utility.LightSchemaGraphModel;
import schemacrawler.test.utility.crawl.LightTable;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class PathServiceTest {

  private record Edge(DatabaseObjectNodeId source, DatabaseObjectNodeId target, SchemaEdge edge) {}

  private static Edge edge(
      final DatabaseObjectNodeId source,
      final DatabaseObjectNodeId target,
      final EdgeType edgeType) {
    return new Edge(source, target, new SchemaEdge(edgeType, new NamedObjectKey(edgeType.name())));
  }

  private static PathService pathService(
      final List<DatabaseObjectNodeId> nodes, final Edge... graphEdges) {
    final Graph<DatabaseObjectNodeId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    nodes.forEach(graph::addVertex);
    for (final Edge edge : graphEdges) {
      graph.addEdge(edge.source(), edge.target(), edge.edge());
    }
    final Set<DatabaseObjectNodeId> tableNodes = Set.copyOf(nodes);
    final Map<DatabaseObjectNodeId, LightTable> nodeToTable =
        nodes.stream().collect(toMap(identity(), node -> new LightTable(node.key().toString())));
    return new PathService(new LightSchemaGraphModel(graph, tableNodes, nodeToTable, List.of()));
  }

  private static DatabaseObjectNodeId table(final String name) {
    return new DatabaseObjectNodeId(
        new NamedObjectKey("PUBLIC", name), SimpleDatabaseObjectType.table);
  }

  @Test
  void allowsUnlimitedPathDepth() {
    final DatabaseObjectNodeId table1 = table("TABLE1");
    final DatabaseObjectNodeId table2 = table("TABLE2");
    final DatabaseObjectNodeId table3 = table("TABLE3");
    final DatabaseObjectNodeId table4 = table("TABLE4");
    final DatabaseObjectNodeId table5 = table("TABLE5");
    final DatabaseObjectNodeId table6 = table("TABLE6");
    final DatabaseObjectNodeId table7 = table("TABLE7");
    final PathService pathService =
        pathService(
            List.of(table1, table2, table3, table4, table5, table6, table7),
            edge(table1, table2, EdgeType.FOREIGN_KEY),
            edge(table2, table3, EdgeType.FOREIGN_KEY),
            edge(table3, table4, EdgeType.FOREIGN_KEY),
            edge(table4, table5, EdgeType.FOREIGN_KEY),
            edge(table5, table6, EdgeType.FOREIGN_KEY),
            edge(table6, table7, EdgeType.FOREIGN_KEY));

    assertThat(
        pathService.findShortestPath(table1, table7, -1).path(),
        contains(table1, table2, table3, table4, table5, table6, table7));
  }

  @Test
  void cachesOnlyEligibleTableEdges() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final DatabaseObjectNodeId procedure =
        new DatabaseObjectNodeId(
            new NamedObjectKey("PUBLIC", "REFRESH_ORDERS"), SimpleDatabaseObjectType.procedure);
    final Graph<DatabaseObjectNodeId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(orders);
    graph.addVertex(customers);
    graph.addVertex(procedure);
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());
    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.IMPLICIT_ASSOCIATION).edge());
    graph.addEdge(procedure, customers, edge(procedure, customers, EdgeType.FOREIGN_KEY).edge());
    final Map<DatabaseObjectNodeId, LightTable> nodeToTable =
        Map.of(
            orders,
            new LightTable(orders.key().toString()),
            customers,
            new LightTable(customers.key().toString()));
    final PathService pathService =
        new PathService(
            new LightSchemaGraphModel(graph, Set.of(orders, customers), nodeToTable, List.of()));

    assertThat(
        pathService.findShortestPath(orders, customers).usesImpliedAssociations(), is(false));
    assertThrows(
        IllegalArgumentException.class, () -> pathService.findShortestPath(procedure, customers));
  }

  @Test
  void cachesTablePathTopology() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final Graph<DatabaseObjectNodeId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(orders);
    graph.addVertex(customers);
    final Map<DatabaseObjectNodeId, LightTable> nodeToTable =
        Map.of(
            orders,
            new LightTable(orders.key().toString()),
            customers,
            new LightTable(customers.key().toString()));
    final PathService pathService =
        new PathService(
            new LightSchemaGraphModel(graph, Set.of(orders, customers), nodeToTable, List.of()));

    graph.addEdge(orders, customers, edge(orders, customers, EdgeType.FOREIGN_KEY).edge());

    assertThat(pathService.findShortestPath(orders, customers).path(), empty());
  }

  @Test
  void fallsBackToImplicitAssociations() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final PathService pathService =
        pathService(
            List.of(orders, customers), edge(orders, customers, EdgeType.IMPLICIT_ASSOCIATION));

    final PathResult result = pathService.findShortestPath(orders, customers);

    assertThat(result.path(), contains(orders, customers));
    assertThat(result.usesImpliedAssociations(), is(true));
  }

  @Test
  void handlesNoPathSameNodeAndUnsupportedNodes() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final DatabaseObjectNodeId procedure =
        new DatabaseObjectNodeId(
            new NamedObjectKey("PUBLIC", "REFRESH_ORDERS"), SimpleDatabaseObjectType.procedure);
    final PathService pathService = pathService(List.of(orders, customers));

    assertThat(pathService.findShortestPath(orders, customers).path(), empty());
    assertThat(pathService.findShortestPath(orders, orders).path(), contains(orders));
    assertThrows(
        IllegalArgumentException.class, () -> pathService.findShortestPath(procedure, customers));
  }

  @Test
  void prefersAnAvailableForeignKeyPath() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final DatabaseObjectNodeId countries = table("COUNTRIES");
    final PathService pathService =
        pathService(
            List.of(orders, customers, countries),
            edge(orders, customers, EdgeType.FOREIGN_KEY),
            edge(customers, countries, EdgeType.FOREIGN_KEY),
            edge(orders, countries, EdgeType.IMPLICIT_ASSOCIATION));

    final PathResult result = pathService.findShortestPath(orders, countries);

    assertThat(result.path(), contains(orders, customers, countries));
    assertThat(result.usesImpliedAssociations(), is(false));
  }

  @Test
  void returnsThePathWithFewestEdges() {
    final DatabaseObjectNodeId orders = table("ORDERS");
    final DatabaseObjectNodeId customers = table("CUSTOMERS");
    final DatabaseObjectNodeId countries = table("COUNTRIES");
    final DatabaseObjectNodeId regions = table("REGIONS");
    final PathService pathService =
        pathService(
            List.of(orders, customers, countries, regions),
            edge(orders, customers, EdgeType.FOREIGN_KEY),
            edge(customers, countries, EdgeType.FOREIGN_KEY),
            edge(orders, regions, EdgeType.FOREIGN_KEY),
            edge(regions, countries, EdgeType.FOREIGN_KEY),
            edge(orders, countries, EdgeType.FOREIGN_KEY));

    assertThat(pathService.findShortestPath(orders, countries).path(), contains(orders, countries));
  }
}
