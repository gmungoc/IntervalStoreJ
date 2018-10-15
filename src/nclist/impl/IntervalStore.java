package nclist.impl;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Comparator;
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

  /**
   * a class providing criteria for performing a binary search of a list
   */
  abstract static class SearchCriterion
  {
    /**
     * Answers true if the entry passes the search criterion test
     * 
     * @param entry
     * @return
     */
    abstract boolean compare(IntervalI entry);

    /**
     * serves a search condition for finding the first interval whose start
     * position follows a given target location
     * 
     * @param target
     * @return
     */
    static SearchCriterion byStart(final long target)
    {
      return new SearchCriterion()
      {

        @Override
        boolean compare(IntervalI entry)
        {
          return entry.getBegin() >= target;
        }
      };
    }

    /**
     * serves a search condition for finding the first interval whose end
     * position is at or follows a given target location
     * 
     * @param target
     * @return
     */
    static SearchCriterion byEnd(final long target)
    {
      return new SearchCriterion()
      {

        @Override
        boolean compare(IntervalI entry)
        {
          return entry.getEnd() >= target;
        }
      };
    }

    /**
     * serves a search condition for finding the first interval which follows
     * the given range as determined by a supplied comparator
     * 
     * @param target
     * @return
     */
    static SearchCriterion byinterval(final IntervalI to,
            final Comparator<IntervalI> rc)
    {
      return new SearchCriterion()
      {

        @Override
        boolean compare(IntervalI entry)
        {
          return rc.compare(entry, to) >= 0;
        }
      };
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
      int insertPosition = binarySearch(nonNested, SearchCriterion
              .byinterval(entry, RangeComparator.BY_START_POSITION));
      // int insertPosition = binarySearch(nonNested, SearchCriterion
      // .byStart(entry.getBegin()));

      /*
       * fail if we detect interval enclosure - of the new interval by
       * the one preceding it, or of the next interval by the new one
       */
      if (insertPosition > 0)
      {
        if (encloses(nonNested.get(insertPosition - 1), entry))
        {
          return false;
        }
      }
      if (insertPosition < nonNested.size())
      {
        if (encloses(entry, nonNested.get(insertPosition)))
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
    int pos = binarySearch(intervals, SearchCriterion.byinterval(interval,
            RangeComparator.BY_START_POSITION));
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
   * Performs a binary search of the (sorted) list to find the index of the
   * first entry which returns true for the given comparator function. Returns
   * the length of the list if there is no such entry.
   * 
   * @param intervals
   * @param sc
   * @return
   */
  protected static int binarySearch(List<? extends IntervalI> intervals,
          SearchCriterion sc)
  {
    int start = 0;
    int end = intervals.size() - 1;
    int matched = intervals.size();

    while (start <= end)
    {
      int mid = (start + end) / 2;
      IntervalI entry = intervals.get(mid);
      boolean compare = sc.compare(entry);
      if (compare)
      {
        matched = mid;
        end = mid - 1;
      }
      else
      {
        start = mid + 1;
      }
    }

    return matched;
  }

  /**
   * Answers true if range1 properly encloses range2, else false
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
    int startIndex = binarySearch(nonNested, SearchCriterion.byEnd(from));
  
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
