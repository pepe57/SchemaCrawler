/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.report;

import static java.util.Objects.requireNonNull;

import java.util.List;

/**
 * Top-level container record holding table clusters and table importance entries.
 *
 * @param clusters detected table clusters
 * @param tables table importance entries
 */
public record ImportanceReport(
    List<ClusterReportEntry> clusters, List<ImportanceReportEntry> tables) {

  public ImportanceReport {
    requireNonNull(clusters, "No table clusters provided");
    requireNonNull(tables, "No tables provided");
    clusters = List.copyOf(clusters);
    tables = List.copyOf(tables);
  }
}
