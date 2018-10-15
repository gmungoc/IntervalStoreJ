package nclist.impl;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import nclist.api.IntervalI;
import nclist.api.IntervalStoreI;

/**
 * A collection class to store interval-associated data, with O(log N)
 * performance for overlap queries, insertion and deletion (where N is the size
 * of the store). Accepts duplicate entries but not null values.
 * 
 * @author gmcarstairs
 *
 * @param <T>
 *          any type providing <code>getBegin()</code> and <code>getEnd()</code>
 */
public class IntervalStore<T extends IntervalI>
        extends AbstractCollection<T> implements IntervalStoreI<T>
{
  /**
   * An iterator over the intervals held in this store, with no particular
   * ordering guaranteed. The iterator does not support the optional
   * <code>remove</code> operation (throws
   * <code>UnsupportedOperationException</code> if attempted).
   * 
   * @author gmcarstairs
   *
   * @param <V>
   */
  private class IntervalIterator<V extends IntervalI> implements Iterator<V>
  {
    /*
     * iterator over top level non-nested intervals
     */
    Iterator<? extends IntervalI> topLevelIterator;

    /*
     * iterator over NCList (if any)
     */
    Iterator<? extends IntervalI> nestedIterator;

    /**
     * Constructor initialises iterators over the top level list and any nested
     * NCList
     * 
     * @param intervalStore
     */
    public IntervalIterator(
            IntervalStore<? extends IntervalI> intervalStore)
    {
      topLevelIterator = nonNested.iterator();
      if (nested != null)
      {
        nestedIterator = nested.iterator();
      }
    }

    @Override
    public boolean hasNext()
    {
      return topLevelIterator.hasNext() ? true
              : (nestedIterator != null && nestedIterator.hasNext());
    }

    @Override
    public V next()
    {
      if (topLevelIterator.hasNext())
      {
        return (V) topLevelIterator.next();
      }
      if (nestedIterator != null)
      {
        return (V) nestedIterator.next();
      }
      throw new NoSuchElementException();
    }

  }

  private List<T> nonNested;

  private NCList<T> nested;

  /**
   * Constructor
   */
  public IntervalStore()
  {
    nonNested = new ArrayList<>();
  }

  /**
   * Adds one interval to the store.
   * 
   * @param interval
   */
  @Override
  public boolean add(T interval)
  {
    if (interval == null)
    {
      return false;
    }
    if (!addNonNestedInterval(interval))
    {
      /*
       * detected a nested interval - put it in the NCList structure
       */
      addNestedInterval(interval);
    }
    return true;
  }

  @Override
  public boolean contains(Object entry)
  {
    if (listContains(nonNested, entry))
    {
      return true;
    }

    return nested == null ? false : nested.contains(entry);
  }

  protected boolean addNonNestedInterval(T entry)
  {
    synchronized (nonNested)
    {
      /*
       * find the first stored interval which doesn't precede the new one
       */
      int insertPosition = BinarySearcher.binarySearch(nonNested,
              BinarySearcher.byInterval(entry,
                      RangeComparator.BY_START_POSITION));
      // int insertPosition = binarySearch(nonNested, SearchCriterion
      // .byStart(entry.getBegin()));

      /*
       * fail if we detect interval enclosure - of the new interval by
       * the one preceding it, or of the next interval by the new one
       */
      if (insertPosition > 0)
      {
        if (Range.encloses(nonNested.get(insertPosition - 1), entry))
        {
          return false;
        }
      }
      if (insertPosition < nonNested.size())
      {
        if (Range.encloses(entry, nonNested.get(insertPosition)))
        {
          return false;
        }
      }

      /*
       * checks passed - add the interval
       */
      nonNested.add(insertPosition, entry);

      return true;
    }
  }

  @Override
  public List<T> findOverlaps(long from, long to)
  {
    List<T> result = new ArrayList<>();

    findNonNestedOverlaps(from, to, result);

    if (nested != null)
    {
      result.addAll(nested.findOverlaps(from, to));
    }

    return result;
  }

  @Override
  public String prettyPrint()
  {
    String pp = nonNested.toString();
    if (nested != null)
    {
      pp += System.lineSeparator() + nested.prettyPrint();
    }
    return pp;
  }

  @Override
  public boolean isValid()
  {
    for (int i = 0; i < nonNested.size() - 1; i++)
    {
      IntervalI i1 = nonNested.get(i);
      IntervalI i2 = nonNested.get(i + 1);
      if (i2.getBegin() < i1.getBegin())
      {
        System.err.println("nonNested wrong order : " + i1.toString() + ", "
                + i2.toString());
        return false;
      }
      if (i2.getEnd() < i1.getEnd() || (i2.getBegin() > i2.getBegin()
              && i1.getEnd() == i2.getEnd()))
      {
        System.err.println("nonNested contains nested!: " + i1.toString()
                + ", " + i2.toString());
        return false;
      }
    }
    return nested == null ? true : nested.isValid();
  }

  @Override
  public int size()
  {
    int i = nonNested.size();
    if (nested != null)
    {
      i += nested.size();
    }
    return i;
  }

  @Override
  public boolean remove(Object o)
  {
    try
    {
      @SuppressWarnings("unchecked")
      T entry = (T) o;

      /*
       * try the non-nested positional intervals first
       */
      boolean removed = nonNested.remove(entry);

      /*
       * if not found, try nested intervals
       */
      if (!removed && nested != null)
      {
        removed = nested.remove(entry);
      }

      return removed;
    } catch (ClassCastException e)
    {
      return false;
    }
  }

  @Override
  public int getDepth()
  {
    return 1 + (nested == null ? 0 : nested.getDepth());
  }

  /**
   * Adds one interval to the NCList that can manage nested intervals (creating
   * the NCList if necessary)
   */
  protected synchronized void addNestedInterval(T interval)
  {
    if (nested == null)
    {
      nested = new NCList<T>();
    }
    nested.add(interval);
  }

  /**
   * Answers true if the list contains the interval, else false. This method is
   * optimised for the condition that the list is sorted on interval start
   * position ascending, and will give unreliable results if this does not hold.
   * 
   * @param intervals
   * @param entry
   * @return
   */
  protected boolean listContains(List<T> intervals, Object entry)
  {
    if (intervals == null || entry == null || !(entry instanceof IntervalI))
    {
      return false;
    }

    IntervalI interval = (IntervalI) entry;

    /*
     * locate the first entry in the list which does not precede the interval
     */
    int pos = BinarySearcher.binarySearch(intervals, BinarySearcher
            .byInterval(interval, RangeComparator.BY_START_POSITION));
    // int pos = binarySearch(intervals,
    // SearchCriterion.byStart(interval.getBegin()));
    int len = intervals.size();
    while (pos < len)
    {
      T sf = intervals.get(pos);
      if (sf.getBegin() > interval.getBegin())
      {
        return false; // no match found
      }
      if (sf.equals(interval))
      {
        return true;
      }
      pos++;
    }
    return false;
  }

  /**
   * Answers an iterator over the intervals in the store, with no particular
   * ordering guaranteed. The iterator does not support the optional
   * <code>remove</code> operation (throws
   * <code>UnsupportedOperationException</code> if attempted).
   */
  @Override
  public Iterator<T> iterator()
  {
    return new IntervalIterator<T>(this);
  }

  @Override
  public void clear()
  {
    this.nonNested.clear();
    this.nested = new NCList<>();
  }

  /**
   * Adds non-nested intervals to the result list that lie within the target
   * range
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findNonNestedOverlaps(long from, long to,
          List<T> result)
  {
    /*
     * find the first interval whose end position is
     * after the target range start
     */
    int startIndex = BinarySearcher.binarySearch(nonNested,
            BinarySearcher.byEnd(from));
  
    final int startIndex1 = startIndex;
    int i = startIndex1;
    while (i < nonNested.size())
    {
      T sf = nonNested.get(i);
      if (sf.getBegin() > to)
      {
        break;
      }
      if (sf.getBegin() <= to && sf.getEnd() >= from)
      {
        result.add(sf);
      }
      i++;
    }
  }

  @Override
  public String toString()
  {
    String s = nonNested.toString();
    if (nested != null)
    {
      s = s + System.lineSeparator() + nested.toString();
    }
    return s;
  }
}
