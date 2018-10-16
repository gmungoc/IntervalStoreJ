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
package nclist.api;

public interface IntervalI
{
  int getBegin();

  int getEnd();

  /**
   * Answers true if this interval contains (or matches) the given interval
   * 
   * @param i
   * @return
   */
  default boolean containsInterval(IntervalI i)
  {
    return i != null
            && i.getBegin() >= getBegin() && i.getEnd() <= getEnd();
  }

  /**
   * Answers true if this interval properly contains the given interval, that
   * is, it contains it and is larger than it
   * 
   * @param i
   * @return
   */
  default boolean properlyContainsInterval(IntervalI i)
  {
    return containsInterval(i)
            && (i.getBegin() > getBegin() || i.getEnd() < getEnd());
  }

  default boolean equalsInterval(IntervalI i)
  {
    return i != null && i.getBegin() == getBegin()
            && i.getEnd() == getEnd();
  }

  default boolean overlapsInterval(IntervalI i)
  {
    if (i == null)
    {
      return false;
    }
    if (i.getBegin() < getBegin())
    {
      return i.getEnd() >= getBegin();
    }
    if (i.getEnd() > getEnd())
    {
      return i.getBegin() <= getEnd();
    }
    return true; // i internal to this
  }
}
