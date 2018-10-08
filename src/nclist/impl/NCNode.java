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

import java.util.ArrayList;
import java.util.List;

import nclist.api.ContiguousI;

/**
 * Each node of the NCList tree consists of a range, and (optionally) the NCList
 * of ranges it encloses
 *
 * @param <V>
 */
class NCNode<V extends ContiguousI> implements ContiguousI
{
  /*
   * deep size (number of ranges included)
   */
  private int size;

  private V region;

  /*
   * null, or an object holding contained subregions of this nodes region
   */
  private NCList<V> subregions;

  /**
   * Constructor given a list of ranges
   * 
   * @param ranges
   */
  NCNode(List<V> ranges)
  {
    build(ranges);
  }

  /**
   * Constructor given a single range
   * 
   * @param range
   */
  NCNode(V range)
  {
    List<V> ranges = new ArrayList<>();
    ranges.add(range);
    build(ranges);
  }

  NCNode(V entry, NCList<V> newNCList)
  {
    region = entry;
    subregions = newNCList;
    size = 1 + newNCList.size();
  }

  /**
   * @param ranges
   */
  protected void build(List<V> ranges)
  {
    size = ranges.size();

    if (!ranges.isEmpty())
    {
      region = ranges.get(0);
    }
    if (ranges.size() > 1)
    {
      subregions = new NCList<V>(ranges.subList(1, ranges.size()));
    }
  }

  @Override
  public int getBegin()
  {
    return region.getBegin();
  }

  @Override
  public int getEnd()
  {
    return region.getEnd();
  }

  /**
   * Formats the node as a bracketed list e.g.
   * 
   * <pre>
   * [1-100 [10-30 [10-20]], 15-30 [20-20]]
   * </pre>
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(10 * size);
    sb.append(region.getBegin()).append("-").append(region.getEnd());
    if (subregions != null)
    {
      sb.append(" ").append(subregions.toString());
    }
    return sb.toString();
  }

  void prettyPrint(StringBuilder sb, int offset, int indent)
  {
    for (int i = 0; i < offset; i++)
    {
      sb.append(" ");
    }
    sb.append(region.getBegin()).append("-").append(region.getEnd());
    if (subregions != null)
    {
      sb.append(System.lineSeparator());
      subregions.prettyPrint(sb, offset + 2, indent);
    }
  }

  /**
   * Add any ranges that overlap the from-to range to the result list
   * 
   * @param from
   * @param to
   * @param result
   */
  void findOverlaps(long from, long to, List<V> result)
  {
    if (region.getBegin() <= to && region.getEnd() >= from)
    {
      result.add(region);
    }
    if (subregions != null)
    {
      subregions.findOverlaps(from, to, result);
    }
  }

  /**
   * Add one range to this subrange
   * 
   * @param entry
   */
  synchronized void add(V entry)
  {
    if (entry.getBegin() < region.getBegin()
            || entry.getEnd() > region.getEnd())
    {
      throw new IllegalArgumentException(
              String.format("adding improper subrange %d-%d to range %d-%d",
                      entry.getBegin(), entry.getEnd(), region.getBegin(),
                      region.getEnd()));
    }
    if (subregions == null)
    {
      subregions = new NCList<V>(entry);
    }
    else
    {
      subregions.add(entry);
    }
    size++;
  }

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList, else false.
   * 
   * @return
   */
  boolean isValid()
  {
    /*
     * we don't handle reverse ranges
     */
    if (region != null && region.getBegin() > region.getEnd())
    {
      return false;
    }
    if (subregions == null)
    {
      return true;
    }
    return subregions.isValid(getBegin(), getEnd());
  }

  /**
   * Adds all contained entries to the given list
   * 
   * @param entries
   */
  void getEntries(List<V> entries)
  {
    entries.add(region);
    if (subregions != null)
    {
      subregions.getEntries(entries);
    }
  }

  /**
   * Answers true if this object contains the given entry (by object equals
   * test), else false
   * 
   * @param entry
   * @return
   */
  boolean contains(V entry)
  {
    if (entry == null)
    {
      return false;
    }
    if (entry.equals(region))
    {
      return true;
    }
    return subregions == null ? false : subregions.contains(entry);
  }

  /**
   * Answers the 'root' region modelled by this object
   * 
   * @return
   */
  V getRegion()
  {
    return region;
  }

  /**
   * Answers the (possibly null) contained regions within this object
   * 
   * @return
   */
  NCList<V> getSubRegions()
  {
    return subregions;
  }

  /**
   * Nulls the subregion reference if it is empty (after a delete entry
   * operation)
   */
  void deleteSubRegionsIfEmpty()
  {
    if (subregions != null && subregions.size() == 0)
    {
      subregions = null;
    }
  }

  /**
   * Answers the (deep) size of this node i.e. the number of ranges it models
   * 
   * @return
   */
  int size()
  {
    return size;
  }
}
