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
package nclist.impl;

import java.util.Comparator;

import nclist.api.ContiguousI;

/**
 * A comparator that orders ranges by either start position or end position
 * ascending. If the position matches, ordering is resolved by end position (or
 * start position).
 * 
 * @author gmcarstairs
 *
 */
public class RangeComparator implements Comparator<ContiguousI>
{
  public static final Comparator<ContiguousI> BY_START_POSITION = new RangeComparator(
          true);

  public static final Comparator<ContiguousI> BY_END_POSITION = new RangeComparator(
          false);

  boolean byStart;

  /**
   * Constructor
   * 
   * @param byStartPosition
   *          if true, order based on start position, if false by end position
   */
  RangeComparator(boolean byStartPosition)
  {
    byStart = byStartPosition;
  }

  @Override
  public int compare(ContiguousI o1, ContiguousI o2)
  {
    int len1 = o1.getEnd() - o1.getBegin();
    int len2 = o2.getEnd() - o2.getBegin();

    if (byStart)
    {
      return compare(o1.getBegin(), o2.getBegin(), len1, len2);
    }
    else
    {
      return compare(o1.getEnd(), o2.getEnd(), len1, len2);
    }
  }

  /**
   * Compares two ranges for ordering
   * 
   * @param pos1
   *          first range positional ordering criterion
   * @param pos2
   *          second range positional ordering criterion
   * @param len1
   *          first range length ordering criterion
   * @param len2
   *          second range length ordering criterion
   * @return
   */
  public int compare(long pos1, long pos2, int len1, int len2)
  {
    int order = Long.compare(pos1, pos2);
    if (order == 0)
    {
      /*
       * if tied on position order, longer length sorts to left
       * i.e. the negation of normal ordering by length
       */
      order = -Integer.compare(len1, len2);
    }
    return order;
  }
}
