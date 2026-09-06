/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.Table;
import schemacrawler.test.utility.crawl.LightPrimaryKey;
import schemacrawler.test.utility.crawl.LightTable;

class ImportanceScoreCalculatorTest {

  private static void put(
      final TableImportanceInputs inputs,
      final Table table,
      final TableImportanceMetrics metrics,
      final boolean hasPrimaryKey,
      final boolean hasIndexes) {
    doReturn(hasPrimaryKey).when(table).hasPrimaryKey();
    doReturn(hasIndexes).when(table).hasIndexes();
    inputs.putInputs(table, metrics);
  }

  private static Table table(final String name) {
    final LightTable table = new LightTable(name);
    table.setPrimaryKey(new LightPrimaryKey(table.addColumn("ID")));
    return spy(table);
  }

  @Test
  void connectedTableOutranksDisconnectedTable() {
    final Table connectedTable = table("AUTHORS");
    final DatabaseObjectNodeId connectedNode = DatabaseObjectNodeIdUtility.create(connectedTable);
    final Table disconnectedTable = table("BOOKAUTHORS");
    final DatabaseObjectNodeId disconnectedNode =
        DatabaseObjectNodeIdUtility.create(disconnectedTable);

    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, connectedTable, new TableImportanceMetrics(1, 1, 0.1, 1, 1), false, false);
    put(inputs, disconnectedTable, new TableImportanceMetrics(0, 0, 0.0, 0, 0), false, false);

    final Map<DatabaseObjectNodeId, Integer> scores = ImportanceScoreCalculator.calculate(inputs);

    assertThat(scores.get(connectedNode), greaterThan(scores.get(disconnectedNode)));
  }

  @Test
  void missingPrimaryKeyOrIndexesDampensWithoutZeroingOutTheScore() {
    final Table wellFormedTable = table("WELL_FORMED");
    final DatabaseObjectNodeId wellFormed = DatabaseObjectNodeIdUtility.create(wellFormedTable);
    final Table noPrimaryKeyOrIndexesTable = table("NO_PK_NO_INDEXES");
    final DatabaseObjectNodeId noPrimaryKeyOrIndexes =
        DatabaseObjectNodeIdUtility.create(noPrimaryKeyOrIndexesTable);

    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, wellFormedTable, new TableImportanceMetrics(2, 2, 1.0, 2, 2), true, true);
    put(
        inputs,
        noPrimaryKeyOrIndexesTable,
        new TableImportanceMetrics(2, 2, 1.0, 2, 2),
        false,
        false);

    final Map<DatabaseObjectNodeId, Integer> scores = ImportanceScoreCalculator.calculate(inputs);

    final int dampened = scores.get(noPrimaryKeyOrIndexes);
    final int undampened = scores.get(wellFormed);
    assertThat(dampened, lessThan(undampened));
    assertThat(dampened, greaterThan(0));
  }

  @Test
  void scoreIsAlwaysWithinZeroToOneHundred() {
    final Table table = table("MAXED_OUT");
    final DatabaseObjectNodeId maxed = DatabaseObjectNodeIdUtility.create(table);
    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, table, new TableImportanceMetrics(100, 100, 1000.0, 500, 500), true, true);

    final Map<DatabaseObjectNodeId, Integer> scores = ImportanceScoreCalculator.calculate(inputs);

    assertThat(scores.get(maxed), greaterThanOrEqualTo(0));
    assertThat(scores.get(maxed), lessThanOrEqualTo(100));
  }

  @Test
  void scoreIsDeterministicAndReproducibleForTheSameInputs() {
    final Table table = table("ORDERS");
    final DatabaseObjectNodeId node = DatabaseObjectNodeIdUtility.create(table);
    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, table, new TableImportanceMetrics(3, 4, 2.5, 5, 6), true, true);

    final int firstRun = ImportanceScoreCalculator.calculate(inputs).get(node);
    final int secondRun = ImportanceScoreCalculator.calculate(inputs).get(node);

    assertThat(firstRun, is(equalTo(secondRun)));
  }

  @Test
  void wellConnectedTableOutranksPoorlyConnectedTable() {
    final Table smallTable = table("SMALL_LOOKUP");
    final DatabaseObjectNodeId smallNode = DatabaseObjectNodeIdUtility.create(smallTable);
    final Table connectedTable = table("BOOKAUTHORS");
    final DatabaseObjectNodeId connectedNode = DatabaseObjectNodeIdUtility.create(connectedTable);

    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, smallTable, new TableImportanceMetrics(0, 0, 0.0, 0, 0), false, false);
    put(inputs, connectedTable, new TableImportanceMetrics(10, 10, 50.0, 20, 20), false, false);

    final Map<DatabaseObjectNodeId, Integer> scores = ImportanceScoreCalculator.calculate(inputs);

    assertThat(scores.get(connectedNode), greaterThan(scores.get(smallNode)));
  }

  @Test
  void zeroGraphSignalsProduceAValidScore() {
    final Table table = table("ONLY_TABLE");
    final DatabaseObjectNodeId onlyTable = DatabaseObjectNodeIdUtility.create(table);

    final TableImportanceInputs inputs = new TableImportanceInputs();
    put(inputs, table, new TableImportanceMetrics(0, 0, 0.0, 0, 0), true, true);

    final Map<DatabaseObjectNodeId, Integer> scores = ImportanceScoreCalculator.calculate(inputs);

    assertThat(scores.get(onlyTable), greaterThanOrEqualTo(0));
    assertThat(scores.get(onlyTable), lessThanOrEqualTo(100));
  }
}
