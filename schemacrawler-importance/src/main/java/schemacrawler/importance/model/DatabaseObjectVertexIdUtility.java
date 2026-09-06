/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model;

import java.util.Objects;
import schemacrawler.schema.DatabaseObject;
import schemacrawler.utility.MetaDataUtility;
import us.fatehi.utility.UtilityMarker;

/** Creates stable graph identifiers for SchemaCrawler database objects. */
@UtilityMarker
public final class DatabaseObjectVertexIdUtility {

  public static DatabaseObjectVertexId create(final DatabaseObject databaseObject) {
    Objects.requireNonNull(databaseObject, "No database object provided");
    return new DatabaseObjectVertexId(
        databaseObject.key(), MetaDataUtility.getSimpleTypeName(databaseObject));
  }

  private DatabaseObjectVertexIdUtility() {
    // Prevent instantiation
  }
}
