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
import schemacrawler.importance.model.DatabaseObjectNodeId;
import schemacrawler.importance.model.DatabaseObjectNodeIdUtility;
import schemacrawler.importance.model.SchemaGraphModel;
import schemacrawler.importance.model.implementation.SchemaGraphModelBuilder;
import schemacrawler.importance.utility.SerializedSchemaGraphModelUtility;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.test.utility.WithTestDatabase;
import us.fatehi.utility.IOUtility;
import us.fatehi.utility.datasource.DatabaseConnectionSource;

@WithTestDatabase
class SchemaGraphModelJavaSerializationTest {

  private Catalog catalog;
  private SchemaGraphModel schemaGraphModel;

  @BeforeEach
  void loadSchemaGraphModel(final Connection connection) {
    final SchemaCrawlerOptions schemaCrawlerOptions =
        schemaCrawlerOptionsWithMaximumSchemaInfoLevel;
    try {
      catalog = getCatalog(connection, schemaCrawlerOptions);
    } catch (final Exception e) {
      fail("Catalog not loaded", e);
    }
    validateSchema(catalog);
    schemaGraphModel = SchemaGraphModelBuilder.builder(catalog).build();
  }

  @Test
  void schemaGraphModelSerializationWithJava(final DatabaseConnectionSource connectionSource)
      throws Exception {
    final Path testOutputFile = IOUtility.createTempFilePath("sc_schema_graph_model", "ser");
    SerializedSchemaGraphModelUtility.saveSchemaGraphModel(
        schemaGraphModel, Files.newOutputStream(testOutputFile, WRITE, CREATE, TRUNCATE_EXISTING));
    assertThat("Schema graph model was not serialized", isFileReadable(testOutputFile), is(true));
    assertThat(fileHeaderOf(testOutputFile), is("ACED"));

    final SchemaGraphModel deserializedSchemaGraphModel =
        SerializedSchemaGraphModelUtility.readSchemaGraphModel(
            newInputStream(testOutputFile, READ));

    assertThat(deserializedSchemaGraphModel, is(notNullValue()));
    assertThat(
        deserializedSchemaGraphModel.getCatalogGraph().vertexSet(),
        hasSize(schemaGraphModel.getCatalogGraph().vertexSet().size()));
    assertThat(
        deserializedSchemaGraphModel.getCatalogGraph().edgeSet(),
        hasSize(schemaGraphModel.getCatalogGraph().edgeSet().size()));
    assertThat(
        deserializedSchemaGraphModel.getTableNodes(),
        hasSize(schemaGraphModel.getTableNodes().size()));
    assertThat(deserializedSchemaGraphModel.getTableClusters(), hasSize(greaterThan(0)));

    final Table table = catalog.getTables().stream().findFirst().orElseThrow();
    final DatabaseObjectNodeId tableNodeId = DatabaseObjectNodeIdUtility.create(table);
    assertThat(
        deserializedSchemaGraphModel.lookupTableByVertexNodeId(tableNodeId).isPresent(), is(true));
    assertThat(
        deserializedSchemaGraphModel.lookupTableImportance(tableNodeId).isPresent(), is(true));
  }

  @Test
  void rejectsNullSerializationInputs() {
    assertThrows(
        NullPointerException.class,
        () -> SerializedSchemaGraphModelUtility.readSchemaGraphModel(null));
    assertThrows(
        NullPointerException.class,
        () ->
            SerializedSchemaGraphModelUtility.saveSchemaGraphModel(
                null, OutputStream.nullOutputStream()));
    assertThrows(
        NullPointerException.class,
        () -> SerializedSchemaGraphModelUtility.saveSchemaGraphModel(schemaGraphModel, null));
  }
}
