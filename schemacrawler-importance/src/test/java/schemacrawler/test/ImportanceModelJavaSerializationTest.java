/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.test;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static schemacrawler.test.utility.DatabaseTestUtility.getCatalog;
import static schemacrawler.test.utility.DatabaseTestUtility.schemaCrawlerOptionsWithMaximumSchemaInfoLevel;
import static schemacrawler.test.utility.DatabaseTestUtility.validateSchema;
import static us.fatehi.test.utility.TestUtility.fileHeaderOf;
import static us.fatehi.utility.IOUtility.isFileReadable;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import schemacrawler.importance.model.DatabaseObjectVertexId;
import schemacrawler.importance.model.ImportanceModel;
import schemacrawler.importance.model.VertexUtility;
import schemacrawler.importance.model.implementation.ImportanceModelBuilder;
import schemacrawler.importance.utility.SerializedImportanceModelUtility;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.test.utility.WithTestDatabase;
import us.fatehi.utility.IOUtility;
import us.fatehi.utility.datasource.DatabaseConnectionSource;

@WithTestDatabase
class ImportanceModelJavaSerializationTest {

  private Catalog catalog;
  private ImportanceModel importanceModel;

  @BeforeEach
  void loadImportanceModel(final Connection connection) {
    final SchemaCrawlerOptions schemaCrawlerOptions =
        schemaCrawlerOptionsWithMaximumSchemaInfoLevel;
    try {
      catalog = getCatalog(connection, schemaCrawlerOptions);
    } catch (final Exception e) {
      fail("Catalog not loaded", e);
    }
    validateSchema(catalog);
    importanceModel = ImportanceModelBuilder.builder(catalog).build();
  }

  @Test
  void rejectsNullSerializationInputs() {
    assertThrows(
        NullPointerException.class,
        () -> SerializedImportanceModelUtility.readImportanceModel(null));
    assertThrows(
        NullPointerException.class,
        () ->
            SerializedImportanceModelUtility.saveImportanceModel(
                null, OutputStream.nullOutputStream()));
    assertThrows(
        NullPointerException.class,
        () -> SerializedImportanceModelUtility.saveImportanceModel(importanceModel, null));
  }

  @Test
  void importanceModelSerializationWithJava(final DatabaseConnectionSource connectionSource)
      throws Exception {
    final Path testOutputFile = IOUtility.createTempFilePath("sc_importance_model", "ser");
    SerializedImportanceModelUtility.saveImportanceModel(
        importanceModel, Files.newOutputStream(testOutputFile, WRITE, CREATE, TRUNCATE_EXISTING));
    assertThat("Importance model was not serialized", isFileReadable(testOutputFile), is(true));
    assertThat(fileHeaderOf(testOutputFile), is("ACED"));

    final ImportanceModel deserializedImportanceModel =
        SerializedImportanceModelUtility.readImportanceModel(newInputStream(testOutputFile, READ));

    assertThat(deserializedImportanceModel, is(notNullValue()));
    assertThat(
        deserializedImportanceModel.getCatalogGraph().vertexSet(),
        hasSize(importanceModel.getCatalogGraph().vertexSet().size()));
    assertThat(
        deserializedImportanceModel.getCatalogGraph().edgeSet(),
        hasSize(importanceModel.getCatalogGraph().edgeSet().size()));
    assertThat(
        deserializedImportanceModel.getTableVertexIds(),
        hasSize(importanceModel.getTableVertexIds().size()));
    assertThat(deserializedImportanceModel.getTableClusters(), hasSize(greaterThan(0)));

    final Table table = catalog.getTables().stream().findFirst().orElseThrow();
    final DatabaseObjectVertexId tableVertexId = VertexUtility.createVertexId(table);
    assertThat(
        deserializedImportanceModel.lookupTableByVertexId(tableVertexId).isPresent(), is(true));
    assertThat(
        deserializedImportanceModel.lookupTableImportance(tableVertexId).isPresent(), is(true));
  }
}
