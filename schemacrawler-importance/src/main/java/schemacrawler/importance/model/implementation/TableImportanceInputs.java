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
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.schema.Table;
import schemacrawler.tools.utility.TableCounts;
import schemacrawler.tools.utility.TableImportanceUtility;
import schemacrawler.tools.utility.TableTraits;

/**
 * Holds complete, per-node inputs needed to build a {@code TableImportance} record - graph topology
 * metrics, table traits, and table counts - keyed by {@link DatabaseObjectNodeId}.
 */
final class TableImportanceInputs {

  /** Consolidated per-node inputs. */
  record TableImportanceInput(
      Table table,
      TableTraits tableTraits,
      TableCounts tableCounts,
      TableImportanceMetrics importanceMetrics) {}

  private final Map<DatabaseObjectNodeId, TableImportanceInput> inputs = new LinkedHashMap<>();

  Iterable<Map.Entry<DatabaseObjectNodeId, TableImportanceInput>> entries() {
    return inputs.entrySet();
  }

  void putInputs(final Table table, final TableImportanceMetrics importanceMetrics) {
    requireNonNull(table, "No table provided");
    requireNonNull(importanceMetrics, "No table importance metrics provided");
    final DatabaseObjectNodeId nodeId = DatabaseObjectNodeIdUtility.create(table);
    final TableTraits tableTraits = TableImportanceUtility.tableTraitsfrom(table);
    final TableCounts tableCounts = TableImportanceUtility.tableCountsfrom(table);

    inputs.put(
        nodeId, new TableImportanceInput(table, tableTraits, tableCounts, importanceMetrics));
  }

  TableImportance store(final DatabaseObjectNodeId nodeId, final Integer importanceScore) {
    requireNonNull(nodeId, "No node id provided");
    requireNonNull(importanceScore, "No importance score provided");

    final TableImportanceInput tableInputs = inputs.get(nodeId);
    if (tableInputs == null) {
      return null;
    }

    final TableImportance tableImportance =
        new TableImportance(
            importanceScore,
            tableInputs.importanceMetrics(),
            tableInputs.tableTraits(),
            tableInputs.tableCounts());
    tableInputs.table().setAttribute(TableImportance.class.getName(), tableImportance);

    return tableImportance;
  }
}
