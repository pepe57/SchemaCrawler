package schemacrawler.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class SchemaEdgeTest {

  @Test
  void retainsReferenceIdentityForEquivalentMetadata() {
    final NamedObjectKey key = new NamedObjectKey("FK_ORDERS_CUSTOMERS");
    final SchemaEdge first = new SchemaEdge(EdgeType.FOREIGN_KEY, key);
    final SchemaEdge second = new SchemaEdge(EdgeType.FOREIGN_KEY, key);

    assertThat(first, is(not(second)));
    assertThat(first.getEdgeType(), is(EdgeType.FOREIGN_KEY));
    assertThat(first.getReferenceKey(), is(key));
    assertNull(new SchemaEdge(EdgeType.VIEW_DEPENDENCY, null).getReferenceKey());

    final DatabaseObjectVertexId source =
        new DatabaseObjectVertexId(new NamedObjectKey("ORDERS"), SimpleDatabaseObjectType.table);
    final DatabaseObjectVertexId target =
        new DatabaseObjectVertexId(new NamedObjectKey("CUSTOMERS"), SimpleDatabaseObjectType.table);
    final Graph<DatabaseObjectVertexId, SchemaEdge> graph =
        new DirectedPseudograph<>(SchemaEdge.class);
    graph.addVertex(source);
    graph.addVertex(target);
    graph.addEdge(source, target, first);
    graph.addEdge(source, target, second);

    assertThat(graph.edgeSet().size(), is(2));
  }
}
