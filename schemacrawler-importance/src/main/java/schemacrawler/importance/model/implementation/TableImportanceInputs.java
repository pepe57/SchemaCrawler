/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import java.util.LinkedHashMap;
import java.util.Map;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.tools.utility.TableCounts;
import schemacrawler.tools.utility.TableTraits;

/**
 * Holds complete, per-node inputs needed to build a {@code TableImportance} record - graph topology
 * metrics, table traits, and table counts - keyed by {@link DatabaseObjectNodeId}.
 */
final class TableImportanceInputs {

  /** Consolidated per-node inputs. */
  record TableImportanceInput(
      TableTraits tableTraits, TableCounts tableCounts, TableImportanceMetrics importanceMetrics) {}

  private final Map<DatabaseObjectNodeId, TableImportanceInput> inputs = new LinkedHashMap<>();

  Iterable<Map.Entry<DatabaseObjectNodeId, TableImportanceInput>> entries() {
    return inputs.entrySet();
  }

  TableImportanceInput get(final DatabaseObjectNodeId nodeId) {
    return inputs.get(nodeId);
  }

  void put(
      final DatabaseObjectNodeId nodeId,
      final TableTraits tableTraits,
      final TableCounts tableCounts,
      final TableImportanceMetrics importanceMetrics) {
    inputs.put(nodeId, new TableImportanceInput(tableTraits, tableCounts, importanceMetrics));
  }
}
