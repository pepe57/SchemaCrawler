/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.TableCluster;
import schemacrawler.importance.model.TableImportance;
import schemacrawler.importance.model.TableImportanceMetrics;
import schemacrawler.importance.options.ImportanceOptions;
import schemacrawler.importance.options.ImportanceOptionsBuilder;
import schemacrawler.importance.report.ImportanceReportEntry;
import schemacrawler.importance.report.ImportanceReportGenerator;
import schemacrawler.inclusionrule.RegularExpressionRule;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.schema.Table;
import schemacrawler.test.utility.LightSchemaGraphModel;
import schemacrawler.test.utility.crawl.LightTable;
import schemacrawler.tools.utility.TableCounts;
import schemacrawler.tools.utility.TableTraits;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

class ReportServiceTest {

  @Test
  void returnsFilteredEntriesOrderedByImportanceScoreThenCentralityThenFullName() {
    final Table alpha = table("ALPHA");
    final Table beta = table("BETA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final DatabaseObjectNodeId betaNode = node("BETA");
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode, betaNode),
            Map.of(alphaNode, alpha, betaNode, beta),
            List.of());

    final var report = new ImportanceReportGenerator(schemaGraphModel).report(options(".*", -1));

    assertThat(report.tables(), contains(entry(betaNode, "BETA"), entry(alphaNode, "ALPHA")));
    assertThat(report.tables().get(0).nodeId(), is(betaNode));
    assertThat(report.tables().get(0).tableFullName(), is("BETA"));
    assertThat(report.communities(), empty());
  }

  @Test
  void fallsBackToCentralityThenFullNameWhenImportanceScoresAreEqual() {
    final Table alpha = tableWithScore("ALPHA", 5, 0.0);
    final Table beta = tableWithScore("BETA", 5, 1.0);
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final DatabaseObjectNodeId betaNode = node("BETA");
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode, betaNode),
            Map.of(alphaNode, alpha, betaNode, beta),
            List.of());

    final var report = new ImportanceReportGenerator(schemaGraphModel).report(options(".*", -1));

    assertThat(report.tables().get(0).tableFullName(), is("BETA"));
    assertThat(report.tables().get(1).tableFullName(), is("ALPHA"));
  }

  @Test
  void appliesTheSuppliedInclusionRule() {
    final Table alpha = table("ALPHA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode),
            Map.of(alphaNode, alpha),
            List.of());

    final var report =
        new ImportanceReportGenerator(schemaGraphModel).report(options(".*BETA", -1));

    assertThat(report.tables().isEmpty(), is(true));
  }

  @Test
  void truncatesEntriesWhenMaxTablesIsPositive() {
    final Table alpha = table("ALPHA");
    final Table beta = table("BETA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final DatabaseObjectNodeId betaNode = node("BETA");
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode, betaNode),
            Map.of(alphaNode, alpha, betaNode, beta),
            List.of());

    final var report = new ImportanceReportGenerator(schemaGraphModel).report(options(".*", 1));

    assertThat(report.tables().size(), is(1));
    assertThat(report.tables().get(0).tableFullName(), is("BETA"));
  }

  @Test
  void returnsAllEntriesWhenMaxTablesIsZeroOrNegative() {
    final Table alpha = table("ALPHA");
    final Table beta = table("BETA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final DatabaseObjectNodeId betaNode = node("BETA");
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode, betaNode),
            Map.of(alphaNode, alpha, betaNode, beta),
            List.of());

    final var reportZero =
        new ImportanceReportGenerator(schemaGraphModel).report(options(".*", -1));
    assertThat(reportZero.tables().size(), is(2));

    final var reportNegative =
        new ImportanceReportGenerator(schemaGraphModel).report(options(".*", -1));
    assertThat(reportNegative.tables().size(), is(2));
  }

  @Test
  void usesCommunitiesCachedOnTheSchemaGraphModel() {
    final Table alpha = table("ALPHA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final TableCluster cachedTableCluster =
        new TableCluster(UUID.randomUUID(), alphaNode, List.of(alphaNode));
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode),
            Map.of(alphaNode, alpha),
            List.of(cachedTableCluster));

    final var report = new ImportanceReportGenerator(schemaGraphModel).report(options(".*", -1));

    assertThat(report.communities(), hasSize(1));
    assertThat(report.communities().get(0).id(), is(cachedTableCluster.id()));
  }

  @Test
  void limitsTooManyTableClusters() {
    final Table alpha = table("ALPHA");
    final Table beta = table("BETA");
    final DatabaseObjectNodeId alphaNode = node("ALPHA");
    final DatabaseObjectNodeId betaNode = node("BETA");
    final TableCluster firstCluster =
        new TableCluster(UUID.randomUUID(), alphaNode, List.of(alphaNode));
    final TableCluster secondCluster =
        new TableCluster(UUID.randomUUID(), betaNode, List.of(betaNode));
    final SchemaGraphModel schemaGraphModel =
        schemaGraphModel(
            new DefaultDirectedGraph<>(SchemaEdge.class),
            Set.of(alphaNode, betaNode),
            Map.of(alphaNode, alpha, betaNode, beta),
            List.of(firstCluster, secondCluster));

    final ImportanceOptions options =
        ImportanceOptionsBuilder.builder()
            .withTableInclusionRule(new RegularExpressionRule(".*", ""))
            .withMaxCommunities(1)
            .toOptions();
    final var report = new ImportanceReportGenerator(schemaGraphModel).report(options);

    assertThat(report.communities(), hasSize(1));
    assertThat(report.communities().get(0).id(), is(firstCluster.id()));
  }

  private static ImportanceReportEntry entry(
      final DatabaseObjectNodeId nodeId, final String tableFullName) {
    return new ImportanceReportEntry(
        nodeId,
        tableFullName,
        new TableImportance(
            score(tableFullName), metrics(tableFullName), new TableTraits(), new TableCounts()));
  }

  private static SchemaGraphModel schemaGraphModel(
      final DefaultDirectedGraph<DatabaseObjectNodeId, SchemaEdge> catalogGraph,
      final Set<DatabaseObjectNodeId> tableNodes,
      final Map<DatabaseObjectNodeId, Table> nodeToObject,
      final List<TableCluster> tableClusters) {
    return new LightSchemaGraphModel(catalogGraph, tableNodes, nodeToObject, tableClusters);
  }

  private static ImportanceOptions options(final String pattern, final int maxImportantTables) {
    return ImportanceOptionsBuilder.builder()
        .withTableInclusionRule(new RegularExpressionRule(pattern, ""))
        .withMaxImportantTables(maxImportantTables)
        .toOptions();
  }

  private static Table table(final String name) {
    return tableWithScore(name, score(name), "BETA".equals(name) ? 1.0 : 0.0);
  }

  private static Table tableWithScore(
      final String name, final int importanceScore, final double betweennessCentrality) {
    final Table table = new LightTable(name);
    table.setAttribute(
        TableImportance.class.getName(),
        new TableImportance(
            importanceScore,
            new TableImportanceMetrics(0, 0, betweennessCentrality, 0, 0),
            new TableTraits(),
            new TableCounts()));
    return table;
  }

  private static int score(final String name) {
    return "BETA".equals(name) ? 10 : 5;
  }

  private static TableImportanceMetrics metrics(final String name) {
    return new TableImportanceMetrics(0, 0, "BETA".equals(name) ? 1.0 : 0.0, 0, 0);
  }

  private static DatabaseObjectNodeId node(final String name) {
    return new DatabaseObjectNodeId(new NamedObjectKey(name), SimpleDatabaseObjectType.table);
  }
}
