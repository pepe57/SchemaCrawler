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
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.tools.utility.TableCounts;
import schemacrawler.tools.utility.TableTraits;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class TableImportanceInputsTest {

  @Test
  void getReturnsNullForAnUnknownNode() {
    final TableImportanceInputs inputs = new TableImportanceInputs();

    assertThat(inputs.get(node("ORPHAN")), is(nullValue()));
  }

  @Test
  void getReturnsTheCompleteInputForAPopulatedNode() {
    final TableImportanceInputs inputs = new TableImportanceInputs();
    final DatabaseObjectNodeId nodeId = node("ORDERS");
    final TableImportanceMetrics metrics = new TableImportanceMetrics(1, 2, 3.0, 4, 5);
    final TableTraits traits = new TableTraits();
    final TableCounts counts = new TableCounts();

    inputs.put(nodeId, traits, counts, metrics);

    assertThat(
        inputs.get(nodeId),
        is(new TableImportanceInputs.TableImportanceInput(traits, counts, metrics)));
  }

  @Test
  void puttingACompleteInputForTheSameNodeOverwritesTheEarlierInput() {
    final TableImportanceInputs inputs = new TableImportanceInputs();
    final DatabaseObjectNodeId nodeId = node("ORDERS");
    final TableImportanceMetrics firstMetrics = new TableImportanceMetrics(1, 1, 1.0, 1, 1);
    final TableImportanceMetrics secondMetrics = new TableImportanceMetrics(2, 2, 2.0, 2, 2);

    inputs.put(nodeId, new TableTraits(), new TableCounts(), firstMetrics);
    inputs.put(nodeId, new TableTraits(), new TableCounts(), secondMetrics);

    assertThat(inputs.get(nodeId).importanceMetrics(), is(secondMetrics));
  }

  private static DatabaseObjectNodeId node(final String name) {
    return new DatabaseObjectNodeId(
        new NamedObjectKey("PUBLIC", name), SimpleDatabaseObjectType.table);
  }
}
