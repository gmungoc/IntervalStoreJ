package nclist.impl;

import java.util.List;

import nclist.api.ContiguousI;

public interface NCListI<T extends ContiguousI>
{

  /**
   * Adds one entry to the stored set, and returns true, unless allowDuplicates
   * is set to false and it is already contained (by object equality test), in
   * which case it is not added and this method returns false.
   * 
   * @param entry
   * @param allowDuplicates
   * @return
   */
  boolean add(T entry, boolean allowDuplicates);

  /**
   * Answers true if this NCList contains the given entry (by object equality
   * test), else false
   * 
   * @param entry
   * @return
   */
  boolean contains(T entry);

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
   * Returns the number of ranges held (deep count)
   * 
   * @return
   */
  int size();

  /**
   * Returns a list of all entries stored
   * 
   * @return
   */
  List<T> getEntries();

  /**
   * Deletes the given entry from the store, returning true if it was found (and
   * deleted), else false. This method makes no assumption that the entry is in
   * the 'expected' place in the store, in case it has been modified since it
   * was added. Only the first 'same object' match is deleted, not 'equal' or
   * multiple objects.
   * 
   * @param entry
   */
  boolean delete(T entry);

}