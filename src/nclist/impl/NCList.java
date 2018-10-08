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
import java.util.Collections;
import java.util.List;

import nclist.api.ContiguousI;

/**
 * An adapted implementation of NCList as described in the paper
 * 
 * <pre>
 * Nested Containment List (NCList): a new algorithm for accelerating
 * interval query of genome alignment and interval databases
 * - Alexander V. Alekseyenko, Christopher J. Lee
 * https://doi.org/10.1093/bioinformatics/btl647
 * </pre>
 */
public class NCList<T extends ContiguousI> implements NCListI<T>
{
  /*
   * the number of ranges represented
   */
  private int size;

  /*
   * a list, in start position order, of sublists of ranges ordered so 
   * that each contains (or is the same as) the one that follows it
   */
  private List<NCNode<T>> subranges;

  /**
   * Constructor given a list of things that are each located on a contiguous
   * interval. Note that the constructor may reorder the list.
   * <p>
   * We assume here that for each range, start &lt;= end. Behaviour for reverse
   * ordered ranges is undefined.
   * 
   * @param ranges
   */
  public NCList(List<T> ranges)
  {
    this();
    build(ranges);
  }

  /**
   * Sort and group ranges into sublists where each sublist represents a region
   * and its contained subregions
   * 
   * @param ranges
   */
  protected void build(List<T> ranges)
  {
    /*
     * sort by start ascending so that contained intervals 
     * follow their containing interval
     */
    Collections.sort(ranges, RangeComparator.BY_START_POSITION);

    List<Range> sublists = buildSubranges(ranges);

    /*
     * convert each subrange to an NCNode consisting of a range and
     * (possibly) its contained NCList
     */
    for (Range sublist : sublists)
    {
      subranges.add(new NCNode<T>(
              ranges.subList(sublist.start, sublist.end + 1)));
    }

    size = ranges.size();
  }

  public NCList(T entry)
  {
    this();
    subranges.add(new NCNode<>(entry));
    size = 1;
  }

  public NCList()
  {
    subranges = new ArrayList<NCNode<T>>();
  }

  /**
   * Traverses the sorted ranges to identify sublists, within which each
   * interval contains the one that follows it
   * 
   * @param ranges
   * @return
   */
  protected List<Range> buildSubranges(List<T> ranges)
  {
    List<Range> sublists = new ArrayList<>();

    if (ranges.isEmpty())
    {
      return sublists;
    }

    int listStartIndex = 0;
    long lastEndPos = Long.MAX_VALUE;

    for (int i = 0; i < ranges.size(); i++)
    {
      ContiguousI nextInterval = ranges.get(i);
      long nextStart = nextInterval.getBegin();
      long nextEnd = nextInterval.getEnd();
      if (nextStart > lastEndPos || nextEnd > lastEndPos)
      {
        /*
         * this interval is not contained in the preceding one 
         * close off the last sublist
         */
        sublists.add(new Range(listStartIndex, i - 1));
        listStartIndex = i;
      }
      lastEndPos = nextEnd;
    }

    sublists.add(new Range(listStartIndex, ranges.size() - 1));
    return sublists;
  }

  /**
   * Adds one entry to the stored set (with duplicates allowed)
   * 
   * @param entry
   */
  public void add(T entry)
  {
    add(entry, true);
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#add(T, boolean)
   */
  @Override
  public synchronized boolean add(T entry, boolean allowDuplicates)
  {
    if (!allowDuplicates && contains(entry))
    {
      return false;
    }

    size++;
    long start = entry.getBegin();
    long end = entry.getEnd();

    /*
     * cases:
     * - precedes all subranges: add as NCNode on front of list
     * - follows all subranges: add as NCNode on end of list
     * - enclosed by a subrange - add recursively to subrange
     * - encloses one or more subranges - push them inside it
     * - none of the above - add as a new node and resort nodes list (?)
     */

    /*
     * find the first subrange whose end does not precede entry's start
     */
    int candidateIndex = findFirstOverlap(start);
    if (candidateIndex == -1)
    {
      /*
       * all subranges precede this one - add it on the end
       */
      subranges.add(new NCNode<>(entry));
      return true;
    }

    /*
     * search for maximal span of subranges i-k that the new entry
     * encloses; or a subrange that encloses the new entry
     */
    boolean enclosing = false;
    int firstEnclosed = 0;
    int lastEnclosed = 0;
    boolean overlapping = false;

    for (int j = candidateIndex; j < subranges.size(); j++)
    {
      NCNode<T> subrange = subranges.get(j);

      if (end < subrange.getBegin() && !overlapping && !enclosing)
      {
        /*
         * new entry lies between subranges j-1 j
         */
        subranges.add(j, new NCNode<>(entry));
        return true;
      }

      if (subrange.getBegin() <= start && subrange.getEnd() >= end)
      {
        /*
         * push new entry inside this subrange as it encloses it
         */
        subrange.add(entry);
        return true;
      }

      if (start <= subrange.getBegin())
      {
        if (end >= subrange.getEnd())
        {
          /*
           * new entry encloses this subrange (and possibly preceding ones);
           * continue to find the maximal list it encloses
           */
          if (!enclosing)
          {
            firstEnclosed = j;
          }
          lastEnclosed = j;
          enclosing = true;
          continue;
        }
        else
        {
          /*
           * entry spans from before this subrange to inside it
           */
          if (enclosing)
          {
            /*
             * entry encloses one or more preceding subranges
             */
            addEnclosingRange(entry, firstEnclosed, lastEnclosed);
            return true;
          }
          else
          {
            /*
             * entry spans two subranges but doesn't enclose any
             * so just add it 
             */
            subranges.add(j, new NCNode<>(entry));
            return true;
          }
        }
      }
      else
      {
        overlapping = true;
      }
    }

    /*
     * drops through to here if new range encloses all others
     * or overlaps the last one
     */
    if (enclosing)
    {
      addEnclosingRange(entry, firstEnclosed, lastEnclosed);
    }
    else
    {
      subranges.add(new NCNode<>(entry));
    }

    return true;
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#contains(T)
   */
  @Override
  public boolean contains(T entry)
  {
    /*
     * find the first sublist that might overlap, i.e. 
     * the first whose end position is >= from
     */
    int candidateIndex = findFirstOverlap(entry.getBegin());

    if (candidateIndex == -1)
    {
      return false;
    }

    int to = entry.getEnd();

    for (int i = candidateIndex; i < subranges.size(); i++)
    {
      NCNode<T> candidate = subranges.get(i);
      if (candidate.getBegin() > to)
      {
        /*
         * we are past the end of our target range
         */
        break;
      }
      if (candidate.contains(entry))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Update the tree so that the range of the new entry encloses subranges i to
   * j (inclusive). That is, replace subranges i-j (inclusive) with a new
   * subrange that contains them.
   * 
   * @param entry
   * @param i
   * @param j
   */
  protected synchronized void addEnclosingRange(T entry, final int i,
          final int j)
  {
    NCList<T> newNCList = new NCList<>();
    newNCList.addNodes(subranges.subList(i, j + 1));
    NCNode<T> newNode = new NCNode<>(entry, newNCList);
    for (int k = j; k >= i; k--)
    {
      subranges.remove(k);
    }
    subranges.add(i, newNode);
  }

  protected void addNodes(List<NCNode<T>> nodes)
  {
    for (NCNode<T> node : nodes)
    {
      subranges.add(node);
      size += node.size();
    }
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#findOverlaps(long, long)
   */
  @Override
  public List<T> findOverlaps(long from, long to)
  {
    List<T> result = new ArrayList<>();

    findOverlaps(from, to, result);

    return result;
  }

  /**
   * Recursively searches the NCList adding any items that overlap the from-to
   * range to the result list
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findOverlaps(long from, long to, List<T> result)
  {
    /*
     * find the first sublist that might overlap, i.e. 
     * the first whose end position is >= from
     */
    int candidateIndex = findFirstOverlap(from);

    if (candidateIndex == -1)
    {
      return;
    }

    for (int i = candidateIndex; i < subranges.size(); i++)
    {
      NCNode<T> candidate = subranges.get(i);
      if (candidate.getBegin() > to)
      {
        /*
         * we are past the end of our target range
         */
        break;
      }
      candidate.findOverlaps(from, to, result);
    }

  }

  /**
   * Search subranges for the first one whose end position is not before the
   * target range's start position, i.e. the first one that may overlap the
   * target range. Returns the index in the list of the first such range found,
   * or -1 if none found.
   * 
   * @param from
   * @return
   */
  protected int findFirstOverlap(long from)
  {
    /*
     * The NCList paper describes binary search for this step,
     * but this not implemented here as (a) I haven't understood it yet
     * and (b) it seems to imply complications for adding to an NCList
     */

    int i = 0;
    if (subranges != null)
    {
      for (NCNode<T> subrange : subranges)
      {
        if (subrange.getEnd() >= from)
        {
          return i;
        }
        i++;
      }
    }
    return -1;
  }

  /**
   * Formats the tree as a bracketed list e.g.
   * 
   * <pre>
   * [1-100 [10-30 [10-20]], 15-30 [20-20]]
   * </pre>
   */
  @Override
  public String toString()
  {
    return subranges.toString();
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#prettyPrint()
   */
  @Override
  public String prettyPrint()
  {
    StringBuilder sb = new StringBuilder(512);
    int offset = 0;
    int indent = 2;
    prettyPrint(sb, offset, indent);
    sb.append(System.lineSeparator());
    return sb.toString();
  }

  /**
   * @param sb
   * @param offset
   * @param indent
   */
  void prettyPrint(StringBuilder sb, int offset, int indent)
  {
    boolean first = true;
    for (NCNode<T> subrange : subranges)
    {
      if (!first)
      {
        sb.append(System.lineSeparator());
      }
      first = false;
      subrange.prettyPrint(sb, offset, indent);
    }
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#isValid()
   */
  @Override
  public boolean isValid()
  {
    return isValid(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList bounded within the given start-end range, else false.
   * <p>
   * Each subrange must lie within start-end (inclusive). Subranges must be
   * ordered by start position ascending.
   * <p>
   * 
   * @param start
   * @param end
   * @return
   */
  boolean isValid(final int start, final int end)
  {
    int lastStart = start;
    for (NCNode<T> subrange : subranges)
    {
      if (subrange.getBegin() < lastStart)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " starts before " + lastStart);
        return false;
      }
      if (subrange.getEnd() > end)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " ends after " + end);
        return false;
      }
      lastStart = subrange.getBegin();

      if (!subrange.isValid())
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Answers the lowest start position enclosed by the ranges
   * 
   * @return
   */
  public int getStart()
  {
    return subranges.isEmpty() ? 0 : subranges.get(0).getBegin();
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#size()
   */
  @Override
  public int size()
  {
    return size;
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#getEntries()
   */
  @Override
  public List<T> getEntries()
  {
    List<T> result = new ArrayList<>();
    getEntries(result);
    return result;
  }

  /**
   * Adds all contained entries to the given list
   * 
   * @param result
   */
  void getEntries(List<T> result)
  {
    for (NCNode<T> subrange : subranges)
    {
      subrange.getEntries(result);
    }
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#delete(T)
   */
  @Override
  public synchronized boolean delete(T entry)
  {
    if (entry == null)
    {
      return false;
    }
    for (int i = 0; i < subranges.size(); i++)
    {
      NCNode<T> subrange = subranges.get(i);
      NCList<T> subRegions = subrange.getSubRegions();

      if (subrange.getRegion() == entry)
      {
        /*
         * if the subrange is rooted on this entry, promote its
         * subregions (if any) to replace the subrange here;
         * NB have to resort subranges after doing this since e.g.
         * [10-30 [12-20 [16-18], 13-19]]
         * after deleting 12-20, 16-18 is promoted to sibling of 13-19
         * but should follow it in the list of subranges of 10-30 
         */
        subranges.remove(i);
        if (subRegions != null)
        {
          subranges.addAll(subRegions.subranges);
          Collections.sort(subranges, RangeComparator.BY_START_POSITION);
        }
        size--;
        return true;
      }
      else
      {
        if (subRegions != null && subRegions.delete(entry))
        {
          size--;
          subrange.deleteSubRegionsIfEmpty();
          return true;
        }
      }
    }
    return false;
  }
}
