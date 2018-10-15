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

import nclist.api.IntervalI;

/**
 * An immutable data bean that models a start-end range
 */
public class Range implements IntervalI
{
  public final int start;

  public final int end;

  @Override
  public int getBegin()
  {
    return start;
  }

  @Override
  public int getEnd()
  {
    return end;
  }

  public Range(int i, int j)
  {
    start = i;
    end = j;
  }

  @Override
  public String toString()
  {
    return String.valueOf(start) + "-" + String.valueOf(end);
  }

  @Override
  public int hashCode()
  {
    return start * 31 + end;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Range)
    {
      Range r = (Range) obj;
      return (start == r.start && end == r.end);
    }
    return false;
  }

  /**
   * Answers true if range1 properly encloses range2, else false. Assumes that
   * begin <= end for both ranges.
   * 
   * @param range1
   * @param range2
   * @return
   */
  protected static boolean encloses(IntervalI range1, IntervalI range2)
  {
    int begin1 = range1.getBegin();
    int begin2 = range2.getBegin();
    int end1 = range1.getEnd();
    int end2 = range2.getEnd();

    if (begin1 > end1 || begin2 > end2)
    {
      // reverse range not supported
      return false;
    }

    if (begin1 == begin2 && end1 > end2)
    {
      return true;
    }
    if (begin1 < begin2 && end1 >= end2)
    {
      return true;
    }
    return false;
  }
}
