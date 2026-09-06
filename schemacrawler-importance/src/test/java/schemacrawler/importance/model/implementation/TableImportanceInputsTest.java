/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.Table;
import schemacrawler.test.utility.crawl.LightTable;

class TableImportanceInputsTest {

  @Test
  void getReturnsNullForAnUnknownNode() {
    final TableImportanceInputs inputs = new TableImportanceInputs();

    assertThat(
        inputs.store(DatabaseObjectNodeIdUtility.create(new LightTable("ORPHAN")), 1),
        is(nullValue()));
  }

  @Test
  void getReturnsTheCompleteInputForAPopulatedNode() {
    final TableImportanceInputs inputs = new TableImportanceInputs();
    final Table table = new LightTable("ORDERS");
    final DatabaseObjectNodeId nodeId = DatabaseObjectNodeIdUtility.create(table);
    final TableImportanceMetrics metrics = new TableImportanceMetrics(1, 2, 3.0, 4, 5);

    inputs.putInputs(table, metrics);

    final var importance = inputs.store(nodeId, 1);

    assertThat(importance.importanceMetrics(), is(metrics));
    assertThat(table.getAttribute(TableImportance.class.getName()), is(importance));
  }

  @Test
  void puttingACompleteInputForTheSameNodeOverwritesTheEarlierInput() {
    final TableImportanceInputs inputs = new TableImportanceInputs();
    final Table table = new LightTable("ORDERS");
    final DatabaseObjectNodeId nodeId = DatabaseObjectNodeIdUtility.create(table);
    final TableImportanceMetrics firstMetrics = new TableImportanceMetrics(1, 1, 1.0, 1, 1);
    final TableImportanceMetrics secondMetrics = new TableImportanceMetrics(2, 2, 2.0, 2, 2);

    inputs.putInputs(table, firstMetrics);
    inputs.putInputs(table, secondMetrics);

    assertThat(inputs.store(nodeId, 1).importanceMetrics(), is(secondMetrics));
  }
}
