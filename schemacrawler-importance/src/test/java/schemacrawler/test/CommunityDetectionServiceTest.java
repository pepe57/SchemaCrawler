/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.implementation.SchemaGraphModelBuilder;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.schema.Table;
import schemacrawler.test.utility.crawl.LightTable;

class CommunityDetectionServiceTest {

  private static Catalog catalog(final List<Table> tables) {
    final Catalog catalog = mock(Catalog.class);
    when(catalog.getTables()).thenReturn(tables);
    when(catalog.getRoutines()).thenReturn(List.of());
    when(catalog.getSynonyms()).thenReturn(List.of());
    return catalog;
  }

  private static Table table(final String name) {
    return spy(new LightTable(name));
  }

  @Test
  void returnsEmptyListForEmptyCatalog() {
    final Catalog catalog = catalog(List.of());
    final SchemaGraphModel graphModel = SchemaGraphModelBuilder.builder(catalog).build();
    final List<TableCluster> tableClusters = graphModel.getTableClusters();

    assertThat(tableClusters, hasSize(0));
    assertThat(graphModel.getTableClusters(), hasSize(0));
  }

  @Test
  void detectsAndAnchorsCommunitiesForConnectedTables() {
    final Table customers = table("CUSTOMERS");
    final Table orders = table("ORDERS");
    final Table orderItems = table("ORDER_ITEMS");

    final ForeignKey fkOrdersCustomers = mock(ForeignKey.class);
    when(fkOrdersCustomers.getPrimaryKeyTable()).thenReturn(customers);
    when(fkOrdersCustomers.key()).thenReturn(new NamedObjectKey("FK_ORDERS_CUSTOMERS"));
    doReturn(List.of(fkOrdersCustomers)).when(orders).getImportedForeignKeys();

    final ForeignKey fkItemsOrders = mock(ForeignKey.class);
    when(fkItemsOrders.getPrimaryKeyTable()).thenReturn(orders);
    when(fkItemsOrders.key()).thenReturn(new NamedObjectKey("FK_ITEMS_ORDERS"));
    doReturn(List.of(fkItemsOrders)).when(orderItems).getImportedForeignKeys();

    final Catalog catalog = catalog(List.of(customers, orders, orderItems));
    final SchemaGraphModel graphModel = SchemaGraphModelBuilder.builder(catalog).build();
    final List<TableCluster> tableClusters = graphModel.getTableClusters();

    assertThat(tableClusters.size(), greaterThan(0));
    for (final TableCluster tableCluster : tableClusters) {
      assertThat(tableCluster.id(), notNullValue());
      assertThat(tableCluster.anchorNode(), notNullValue());
      assertThat(tableCluster.memberNodes().contains(tableCluster.anchorNode()), is(true));
    }
    assertThat(graphModel.getTableClusters(), is(tableClusters));
  }
}
