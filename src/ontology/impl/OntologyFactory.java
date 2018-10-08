/*
 * Jalview - A  Alignment Editor and Viewer ($$Version-Rel$$)
 * Copyright (C) $$Year-Rel$$ The Jalview Authors
 * 
 * This file is part of Jalview.
 * 
 * Jalview is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *  
 * Jalview is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Jalview.  If not, see <http://www.gnu.org/licenses/>.
 * The Jalview Authors are detailed in the 'AUTHORS' file.
 */
package ontology.impl;

import ontology.api.OntologyI;

/**
 * A factory class that returns a model of an Ontology
 * 
 * @author gmcarstairs
 */
public class OntologyFactory
{
  public enum Ontology
  {
    SO, GO
  }

  private static OntologyI instance;

  /**
   * Serves an Ontology model of the requested type if available, else null
   * 
   * @param o
   * @return
   */
  public static synchronized OntologyI getInstance(Ontology o)
  {
    switch (o) {
    case SO:
    if (instance == null)
    {
      instance = new SequenceOntologyLite();
    }
    return instance;
    default:
      System.err.println("Only SO is supported");
      return null;
    }
  }

  public static void setInstance(OntologyI so)
  {
    instance = so;
  }
}
