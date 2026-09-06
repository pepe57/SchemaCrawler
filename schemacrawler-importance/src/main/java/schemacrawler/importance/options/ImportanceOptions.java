/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.options;

import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.tools.text.options.BaseTextOptions;

/** Options controlling which tables appear in an importance report. */
public final class ImportanceOptions extends BaseTextOptions {

  private final InclusionRule tableInclusionRule;
  private final int maxClusters;
  private final int maxClusterSize;
  private final int maxImportantTables;

  ImportanceOptions(final ImportanceOptionsBuilder builder) {
    super(builder);
    tableInclusionRule = builder.tableInclusionRule;
    maxImportantTables = builder.maxImportantTables;
    maxClusters = builder.maxClusters;
    maxClusterSize = builder.maxClusterSize;
  }

  public int getMaxClusters() {
    return maxClusters;
  }

  public int getMaxClusterSize() {
    return maxClusterSize;
  }

  public int getMaxImportantTables() {
    return maxImportantTables;
  }

  public InclusionRule getTableInclusionRule() {
    return tableInclusionRule;
  }
}
