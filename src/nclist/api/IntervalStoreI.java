package nclist.api;

import java.util.Collection;
import java.util.List;

public interface IntervalStoreI<T extends IntervalI> extends Collection<T>
{

  /**
   * Returns a (possibly empty) list of items whose extent overlaps the given
   * range
   * 
   * @param from
   *          start of overlap range (inclusive)
   * @param to
   *          end of overlap range (inclusive)
   * @return
   */
  List<T> findOverlaps(long from, long to);

  /**
   * Returns a string representation of the data where containment is shown by
   * indentation on new lines
   * 
   * @return
   */
  String prettyPrint();

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList, else false.
   * 
   * @return
   */
  boolean isValid();

  /**
   * Answers the level of nesting of NCList/NCNode in the data tree, where 1
   * means there are no contained sub-intervals
   * 
   * @return
   */
  int getDepth();

}