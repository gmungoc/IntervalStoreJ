package nclist.impl;

import java.util.Comparator;
import java.util.List;

import nclist.api.IntervalI;

public class BinarySearcher
{

  /**
   * singletons providing comparison criteria for performing a binary search of
   * a list
   */
    /**
     * serves a search condition for finding the first interval whose start
     * position follows a given target location
     * 
     * @param target
     * @return
     */
  static Comparable<IntervalI> byStart(final long target)
  {
    return new Comparable<IntervalI>()
    {
      @Override
      public int compareTo(IntervalI entry)
      {
        return (int) (entry.getBegin() - target);
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
  static Comparable<IntervalI> byEnd(final long target)
  {
    return new Comparable<IntervalI>()
    {
      @Override
      public int compareTo(IntervalI entry)
      {
        return (int) (entry.getEnd() - target);
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
  static Comparable<IntervalI> byInterval(final IntervalI to,
            final Comparator<IntervalI> rc)
  {
    return new Comparable<IntervalI>()
    {

      @Override
      public int compareTo(IntervalI entry)
      {
        return rc.compare(entry, to);
      }
    };
  }

  /**
   * Performs a binary search of the (sorted) list to find the index of the
   * first entry which satisfies the given comparator function. Returns the
   * length of the list if there is no such entry.
   * 
   * @param intervals
   * @param sc
   * @return
   */
  protected static int binarySearch(List<? extends IntervalI> intervals,
          Comparable<IntervalI> sc)
  {
    int start = 0;
    int end = intervals.size() - 1;
    int matched = intervals.size();
  
    while (start <= end)
    {
      int mid = (start + end) / 2;
      IntervalI entry = intervals.get(mid);
      boolean compare = sc.compareTo(entry) >= 0;
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

}
