/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2026, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */

package schemacrawler.importance.model;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.utility.MetaDataUtility.SimpleDatabaseObjectType;

/** Identifies a schema graph vertex by its database key and object type. */
public record DatabaseObjectNodeId(NamedObjectKey key, SimpleDatabaseObjectType type)
    implements Serializable {

  public DatabaseObjectNodeId {
    requireNonNull(key, "No object key provided");
    requireNonNull(type, "No object type provided");
  }
}
