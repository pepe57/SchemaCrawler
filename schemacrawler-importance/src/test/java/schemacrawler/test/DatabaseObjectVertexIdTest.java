package schemacrawler.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class DatabaseObjectVertexIdTest {

  @Test
  void distinguishesTypesWithTheSameKey() {
    final NamedObjectKey key = new NamedObjectKey("PUBLIC", "ORDERS");
    final DatabaseObjectVertexId table =
        new DatabaseObjectVertexId(key, SimpleDatabaseObjectType.table);
    final DatabaseObjectVertexId view =
        new DatabaseObjectVertexId(key, SimpleDatabaseObjectType.view);

    assertThat(table, is(not(view)));
    final DatabaseObjectVertexId equivalent =
        new DatabaseObjectVertexId(key, SimpleDatabaseObjectType.table);
    assertThat(table, is(equivalent));
    assertThat(table.hashCode(), is(equivalent.hashCode()));
  }
}
