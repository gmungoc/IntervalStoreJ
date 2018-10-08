/*
 * Jalview - A Sequence Alignment Editor and Viewer ($$Version-Rel$$)
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
package ontology.api;

import java.util.List;

public interface OntologyI
{
  public boolean isA(String childTerm, String parentTerm);

  /**
   * Returns a sorted list of all valid terms queried for (i.e. terms processed
   * which were valid in the SO), using the friendly description.
   * 
   * This can be used to check that any hard-coded stand-in for the full SO
   * includes all the terms needed for correct processing.
   * 
   * @return
   */
  public List<String> termsFound();

  /**
   * Returns a sorted list of all invalid terms queried for (i.e. terms
   * processed which were not found in the SO), using the friendly description.
   * 
   * This can be used to report any 'non-compliance' in data, and/or to report
   * valid terms missing from any hard-coded stand-in for the full SO.
   * 
   * @return
   */
  public List<String> termsNotFound();
}
