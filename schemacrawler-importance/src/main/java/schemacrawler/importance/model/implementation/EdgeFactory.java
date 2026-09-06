/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model.implementation;

import static schemacrawler.schema.TableConstraintType.implicit_association;

import java.util.logging.Logger;
import schemacrawler.importance.model.EdgeType;
import schemacrawler.importance.model.SchemaEdge;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.schema.ReferencingObject;
import schemacrawler.schema.Routine;
import schemacrawler.schema.Synonym;
import schemacrawler.schema.Table;
import schemacrawler.schema.TableConstraint;
import schemacrawler.schema.TableReference;
import schemacrawler.schema.View;
import us.fatehi.utility.UtilityMarker;

/** Adds typed dependency edges from catalog metadata to a schema graph. */
@UtilityMarker
final class EdgeFactory {

  private static final Logger LOGGER = Logger.getLogger(EdgeFactory.class.getName());

  static void addRoutineEdges(final SchemaGraphAssembly assembly, final Routine routine) {
    addReferencedObjectEdges(routine, EdgeType.ROUTINE_DEPENDENCY, assembly);
  }

  static void addSynonymEdges(final SchemaGraphAssembly assembly, final Synonym synonym) {
    if (synonym.hasReferencedObject()) {
      addEdge(synonym, synonym.getReferencedObject(), EdgeType.SYNONYM_RESOLUTION, null, assembly);
    }
  }

  static void addTableEdges(final SchemaGraphAssembly assembly, final Table table) {
    addForeignKeyEdges(table, assembly);
    addImpliedAssociationEdges(table, assembly);
    if (table instanceof final View view) {
      addReferencedObjectEdges(view, EdgeType.VIEW_DEPENDENCY, assembly);
    }
  }

  private static void addEdge(
      final DatabaseObject source,
      final DatabaseObject target,
      final EdgeType edgeType,
      final NamedObjectKey referenceKey,
      final SchemaGraphAssembly assembly) {
    if (source == null || target == null) {
      LOGGER.warning(() -> "Skipping " + edgeType + " edge with a missing endpoint");
      return;
    }
    if (!assembly.addEdge(source, target, new SchemaEdge(edgeType, referenceKey))) {
      LOGGER.warning(
          () ->
              "Skipping "
                  + edgeType
                  + " edge because a referenced object is not part of the catalog");
    }
  }

  private static void addForeignKeyEdges(final Table table, final SchemaGraphAssembly assembly) {
    for (final ForeignKey foreignKey : table.getImportedForeignKeys()) {
      addEdge(
          table, foreignKey.getPrimaryKeyTable(), EdgeType.FOREIGN_KEY, foreignKey.key(), assembly);
    }
  }

  private static void addImpliedAssociationEdges(
      final Table table, final SchemaGraphAssembly assembly) {
    for (final TableConstraint constraint : table.getTableConstraints()) {
      if (constraint.getType() == implicit_association
          && constraint instanceof final TableReference reference) {
        addEdge(
            table,
            reference.getPrimaryKeyTable(),
            EdgeType.IMPLICIT_ASSOCIATION,
            reference.key(),
            assembly);
      }
    }
  }

  private static void addReferencedObjectEdges(
      final ReferencingObject source, final EdgeType edgeType, final SchemaGraphAssembly assembly) {
    final DatabaseObject sourceObject = (DatabaseObject) source;
    for (final DatabaseObject referencedObject : source.getReferencedObjects()) {
      addEdge(sourceObject, referencedObject, edgeType, null, assembly);
    }
  }

  private EdgeFactory() {
    // Prevent instantiation
  }
}
