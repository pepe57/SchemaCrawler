package schemacrawler.importance.model.implementation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class GraphMetricsCalculatorTest {

  private static DatabaseObjectVertexId node(final String name) {
    return new DatabaseObjectVertexId(
        new NamedObjectKey("PUBLIC", name), SimpleDatabaseObjectType.table);
  }

  @Test
  void calculatesCompleteSchemaGraphMetricsIncludingUndirectedBridgeCentrality() {
    final DatabaseObjectVertexId orders = node("ORDERS");
    final DatabaseObjectVertexId customers = node("CUSTOMERS");
    final DatabaseObjectVertexId countries = node("COUNTRIES");
    final Graph<DatabaseObjectVertexId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(orders);
    graph.addVertex(customers);
    graph.addVertex(countries);
    graph.addEdge(orders, customers, new SchemaEdge(EdgeType.FOREIGN_KEY, null));
    graph.addEdge(orders, countries, new SchemaEdge(EdgeType.IMPLICIT_ASSOCIATION, null));

    final var metrics = GraphMetricsCalculator.calculate(graph);
    final TableImportanceMetrics ordersMetrics = metrics.get(orders);
    final TableImportanceMetrics customersMetrics = metrics.get(customers);
    final TableImportanceMetrics countriesMetrics = metrics.get(countries);

    assertThat(ordersMetrics.inDegree(), is(0));
    assertThat(ordersMetrics.outDegree(), is(2));
    assertThat(ordersMetrics.dependencyReachabilityCount(), is(2));
    assertThat(ordersMetrics.impactReachabilityCount(), is(0));
    assertThat(customersMetrics.inDegree(), is(1));
    assertThat(customersMetrics.outDegree(), is(0));
    assertThat(customersMetrics.dependencyReachabilityCount(), is(0));
    assertThat(customersMetrics.impactReachabilityCount(), is(1));
    assertThat(
        ordersMetrics.betweennessCentrality(),
        greaterThan(customersMetrics.betweennessCentrality()));
    assertThat(
        ordersMetrics.betweennessCentrality(),
        greaterThan(countriesMetrics.betweennessCentrality()));
  }
}
