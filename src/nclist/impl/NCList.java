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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import nclist.api.IntervalI;

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
public class NCList<T extends IntervalI> extends AbstractCollection<T>
{
  /**
   * A depth-first iterator over the elements stored in the NCList
   */
  private class NCListIterator implements Iterator<T>
  {
    int subrangeIndex;

    Iterator<T> nodeIterator;

    /**
     * Constructor bootstraps a pointer to an iterator over the first subrange
     * (if any)
     */
    NCListIterator()
    {
      subrangeIndex = nextSubrange(-1);
    }

    /**
     * Moves the subrange iterator to the next subrange which is not empty, or
     * sets to null if none found. Answers the index of the corresponding
     * subrange in the list.
     * 
     * @return
     */
    private int nextSubrange(int after)
    {
      for (int i = after + 1; i < subranges.size(); i++)
      {
        if (subranges.get(i).size() > 0)
        {
          nodeIterator = subranges.get(i).iterator();
          return i;
        }
      }
      nodeIterator = null;
      return -1;
    }

    @Override
    public boolean hasNext()
    {
      return nodeIterator != null && nodeIterator.hasNext();
    }

    /**
     * Answers the next element returned by the current NCNode's iterator, and
     * advances the iterator (to the next NCNode if necessary)
     */
    @Override
    public T next()
    {
      if (nodeIterator == null || !nodeIterator.hasNext())
      {
        throw new NoSuchElementException();
      }
      T result = nodeIterator.next();

      if (!nodeIterator.hasNext())
      {
        subrangeIndex = nextSubrange(subrangeIndex);
      }
      return result;
    }

  }

  /*
   * the number of interval instances represented
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
    for (IntervalI sublist : sublists)
    {
      subranges.add(new NCNode<>(
              ranges.subList(sublist.getBegin(), sublist.getEnd() + 1)));
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
    subranges = new ArrayList<>();
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

    IntervalI lastParent = ranges.get(0);
    boolean first = true;

    for (int i = 0; i < ranges.size(); i++)
    {
      IntervalI nextInterval = ranges.get(i);
      if (!first && !lastParent.properlyContainsInterval(nextInterval))
      {
        /*
         * this interval is not properly contained in the parent; 
         * close off the last sublist
         */
        sublists.add(new Range(listStartIndex, i - 1));
        listStartIndex = i;
        lastParent = nextInterval;
      }
      first = false;
    }

    sublists.add(new Range(listStartIndex, ranges.size() - 1));
    return sublists;
  }

  /**
   * Adds one entry to the stored set
   * 
   * @param entry
   */
  @Override
  public synchronized boolean add(final T entry)
  {
    final NCNode<T> newNode = new NCNode<>(entry);
    addNode(newNode);
    size++;
    return true;
  }

  /**
   * Adds one NCNode to this NCList
   * <p>
   * This method does not update the <code>size</code> (interval count) of this
   * NCList, as it may be used to rearrange nodes without changing their count.
   * Callers should increment the count if needed.
   * 
   * @param newNode
   */
  protected void addNode(final NCNode<T> newNode)
  {
    final long start = newNode.getBegin();
    final long end = newNode.getEnd();

    /*
     * cases:
     * 1) precedes all subranges - add as NCNode on front of list
     * 2) follows all subranges - add as NCNode on end of list
     * 3) matches a subrange - add as a sibling in the list
     * 4) properly enclosed by a subrange - add recursively to subrange
     * 5) properly encloses one or more subranges - push them inside it
     * 6) spans two subranges - insert between them
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
      subranges.add(newNode);
      return;
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

      if (subrange.equalsInterval(newNode))
      {
        /*
         * matching interval - insert adjacent
         */
        subranges.add(j, newNode);
        return;
      }

      if (end < subrange.getBegin() && !overlapping && !enclosing)
      {
        /*
         * new entry lies between subranges j-1 j
         */
        subranges.add(j, newNode);
        return;
      }

      if (subrange.properlyContainsInterval(newNode))
      {
        /*
         * push new entry inside this subrange as it encloses it
         */
        subrange.addNode(newNode);
        return;
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
            push(newNode, firstEnclosed, lastEnclosed);
          }
          else
          {
            /*
             * entry overlaps two subranges but doesn't enclose either
             * so just add it 
             */
            subranges.add(j, newNode);
          }
          return;
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
      push(newNode, firstEnclosed, lastEnclosed);
    }
    else
    {
      subranges.add(newNode);
    }
  }

  @Override
  public boolean contains(Object entry)
  {
    if (!(entry instanceof IntervalI))
    {
      return false;
    }
    IntervalI interval = (IntervalI) entry;

    /*
     * find the first sublist that might overlap, i.e. 
     * the first whose end position is >= from
     */
    int candidateIndex = findFirstOverlap(interval.getBegin());

    if (candidateIndex == -1)
    {
      return false;
    }

    int to = interval.getEnd();

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
      if (candidate.contains(interval))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Update the tree so that the new node encloses current subranges i to j
   * (inclusive). That is, replace subranges i-j (inclusive) with a new subrange
   * that contains them.
   * 
   * @param node
   * @param i
   * @param j
   * @throws IllegalArgumentException
   *           if any of the subranges is not contained by the node's start-end
   *           range
   */
  protected synchronized void push(NCNode<T> node, final int i,
          final int j)
  {
    for (int k = i; k <= j; k++)
    {
      NCNode<T> n = subranges.get(k);
      if (!node.containsInterval(n)) {
        throw new IllegalArgumentException("Can't push " + n.toString()
                + " inside " + node.toString());
      }
      node.addNode(n);
    }

    for (int k = j; k >= i; k--)
    {
      subranges.remove(k);
    }
    subranges.add(i, node);
  }

  /**
   * Answers a list of contained intervals that overlap the given range
   * 
   * @param from
   * @param to
   * @return
   */
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
    return BinarySearcher.binarySearch(subranges,
            BinarySearcher.byEnd(from));
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

  /**
   * Answers the NCList as an indented list
   * 
   * @return
   */
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

  /**
   * Answers true if the store's structure is valid (nesting containment rules
   * are obeyed), else false. For use in testing and debugging.
   * 
   * @return
   */
  public boolean isValid()
  {
    return isValid(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList bounded within the given start-end range, else false.
   * <p>
   * Each subrange must lie within start-end (inclusive). Subranges must be
   * ordered by start position ascending, and within that by end position
   * descending.
   * <p>
   * 
   * @param start
   * @param end
   * @return
   */
  boolean isValid(final int start, final int end)
  {
    NCNode<T> lastRange = null;

    for (NCNode<T> subrange : subranges)
    {
      if (subrange.getBegin() < start)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " starts before " + end);
        return false;
      }
      if (subrange.getEnd() > end)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " ends after " + end);
        return false;
      }

      if (lastRange != null)
      {
        if (subrange.getBegin() < lastRange.getBegin())
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " starts before " + lastRange.toString());
          return false;
        }
        if (subrange.properlyContainsInterval(lastRange))
        // subrange.getBegin() == lastRange.getBegin()
        // && subrange.getEnd() > lastRange.getEnd())
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " encloses preceding: " + lastRange.toString());
          return false;
        }
        if (lastRange.properlyContainsInterval(subrange))
        // subrange.getBegin() >= lastRange.getBegin()
        // && subrange.getEnd() <= lastRange.getEnd())
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " enclosed by preceding: " + lastRange.toString());
          return false;
        }
      }
      lastRange = subrange;

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

  /**
   * Answers the number of intervals stored
   * 
   * @return
   */
  @Override
  public int size()
  {
    return size;
  }

  /* (non-Javadoc)
   * @see nclist.impl.NCListI#getEntries()
   */
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

  /**
   * Removes the first interval <code>I</code>found that is equal to T
   * (<code>I.equals(T)</code>). Answers true if an interval is removed, false
   * if no match is found. This method is synchronized so thread-safe.
   * 
   * @param entry
   * @return
   */
  public synchronized boolean remove(T entry)
  {
    if (entry == null)
    {
      return false;
    }
    for (int i = 0; i < subranges.size(); i++)
    {
      NCNode<T> subrange = subranges.get(i);
      NCList<T> subRegions = subrange.getSubRegions();

      if (subrange.getRegion().equals(entry))
      {
        /*
         * if the subrange is rooted on this entry, promote its
         * subregions (if any)  
         */
        subranges.remove(i);
        if (subRegions != null)
        {
          for (NCNode<T> r : subRegions.subranges)
          {
            addNode(r);
          }
        }
        size--;
        return true;
      }
      else
      {
        if (subRegions != null && subRegions.remove(entry))
        {
          size--;
          subrange.deleteSubRegionsIfEmpty();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Answers the depth of interval nesting of this object, where 1 means there
   * are no nested sub-intervals
   * 
   * @return
   */
  public int getDepth()
  {
    int subDepth = 0;
    for (NCNode<T> subrange : subranges)
    {
      subDepth = Math.max(subDepth, subrange.getDepth());
    }

    return subDepth;
  }

  @Override
  public Iterator<T> iterator()
  {
    return new NCListIterator();
  }
}
