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
package features.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import features.api.SequenceFeatureI;
import nclist.api.ContiguousI;
import nclist.impl.NCList;
import nclist.impl.NCListI;
import nclist.impl.RangeComparator;

/**
 * A data store for a set of sequence features that supports efficient lookup of
 * features overlapping a given range. Intended for (but not limited to) storage
 * of features for one sequence and feature type.
 * 
 * @author gmcarstairs
 *
 */
public class FeatureStore
{
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
    abstract boolean compare(ContiguousI entry);

    /**
     * serves a search condition for finding the first feature whose start
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
        boolean compare(ContiguousI entry)
        {
          return entry.getBegin() >= target;
        }
      };
    }

    /**
     * serves a search condition for finding the first feature whose end
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
        boolean compare(ContiguousI entry)
        {
          return entry.getEnd() >= target;
        }
      };
    }

    /**
     * serves a search condition for finding the first feature which follows the
     * given range as determined by a supplied comparator
     * 
     * @param target
     * @return
     */
    static SearchCriterion byFeature(final ContiguousI to,
            final Comparator<ContiguousI> rc)
    {
      return new SearchCriterion()
      {

        @Override
        boolean compare(ContiguousI entry)
        {
          return rc.compare(entry, to) >= 0;
        }
      };
    }
  }

  /*
   * Non-positional features have no (zero) start/end position.
   * Kept as a separate list in case this criterion changes in future.
   */
  List<SequenceFeatureI> nonPositionalFeatures;

  /*
   * An ordered list of features, with the promise that no feature in the list 
   * properly contains any other. This constraint allows bounded linear search
   * of the list for features overlapping a region.
   * Contact features are not included in this list.
   */
  List<SequenceFeatureI> nonNestedFeatures;

  /*
   * contact features ordered by first contact position
   */
  List<SequenceFeatureI> contactFeatureStarts;

  /*
   * contact features ordered by second contact position
   */
  List<SequenceFeatureI> contactFeatureEnds;

  /*
   * Nested Containment List is used to hold any features that are nested 
   * within (properly contained by) any other feature. This is a recursive tree
   * which supports depth-first scan for features overlapping a range.
   * It is used here as a 'catch-all' fallback for features that cannot be put
   * into a simple ordered list without invalidating the search methods.
   */
  NCListI<SequenceFeatureI> nestedFeatures;

  /*
   * Feature groups represented in stored positional features 
   * (possibly including null)
   */
  Set<String> positionalFeatureGroups;

  /*
   * Feature groups represented in stored non-positional features 
   * (possibly including null)
   */
  Set<String> nonPositionalFeatureGroups;

  /*
   * the total length of all positional features; contact features count 1 to
   * the total and 1 to size(), consistent with an average 'feature length' of 1
   */
  int totalExtent;

  float positionalMinScore;

  float positionalMaxScore;

  float nonPositionalMinScore;

  float nonPositionalMaxScore;

  /**
   * Constructor
   */
  public FeatureStore()
  {
    nonNestedFeatures = new ArrayList<>();
    positionalFeatureGroups = new HashSet<String>();
    nonPositionalFeatureGroups = new HashSet<String>();
    positionalMinScore = Float.NaN;
    positionalMaxScore = Float.NaN;
    nonPositionalMinScore = Float.NaN;
    nonPositionalMaxScore = Float.NaN;

    // we only construct nonPositionalFeatures, contactFeatures
    // or the NCList if we need to
  }

  /**
   * Adds one sequence feature to the store, and returns true, unless the
   * feature is already contained in the store, in which case this method
   * returns false. Containment is determined by SequenceFeature.equals()
   * comparison.
   * 
   * @param feature
   */
  public boolean addFeature(SequenceFeatureI feature)
  {
    if (contains(feature))
    {
      return false;
    }

    /*
     * keep a record of feature groups
     */
    if (!feature.isNonPositional())
    {
      positionalFeatureGroups.add(feature.getFeatureGroup());
    }

    boolean added = false;

    if (feature.isContactFeature())
    {
      added = addContactFeature(feature);
    }
    else if (feature.isNonPositional())
    {
      added = addNonPositionalFeature(feature);
    }
    else
    {
      added = addNonNestedFeature(feature);
      if (!added)
      {
        /*
         * detected a nested feature - put it in the NCList structure
         */
        added = addNestedFeature(feature);
      }
    }

    if (added)
    {
      /*
       * record the total extent of positional features, to make
       * getTotalFeatureLength possible; we count the length of a 
       * contact feature as 1
       */
      totalExtent += getFeatureLength(feature);

      /*
       * record the minimum and maximum score for positional
       * and non-positional features
       */
      float score = feature.getScore();
      if (!Float.isNaN(score))
      {
        if (feature.isNonPositional())
        {
          nonPositionalMinScore = min(nonPositionalMinScore, score);
          nonPositionalMaxScore = max(nonPositionalMaxScore, score);
        }
        else
        {
          positionalMinScore = min(positionalMinScore, score);
          positionalMaxScore = max(positionalMaxScore, score);
        }
      }
    }

    return added;
  }

  /**
   * Answers true if this store contains the given feature (testing by
   * SequenceFeature.equals), else false
   * 
   * @param feature
   * @return
   */
  public boolean contains(SequenceFeatureI feature)
  {
    if (feature.isNonPositional())
    {
      return nonPositionalFeatures == null ? false
              : nonPositionalFeatures.contains(feature);
    }

    if (feature.isContactFeature())
    {
      return contactFeatureStarts == null ? false
              : listContains(contactFeatureStarts, feature);
    }

    if (listContains(nonNestedFeatures, feature))
    {
      return true;
    }

    return nestedFeatures == null ? false
            : nestedFeatures.contains(feature);
  }

  /**
   * Answers the 'length' of the feature, counting 0 for non-positional features
   * and 1 for contact features
   * 
   * @param sf
   * @return
   */
  protected static int getFeatureLength(SequenceFeatureI sf)
  {
    if (sf.isNonPositional())
    {
      return 0;
    }
    if (sf.isContactFeature())
    {
      return 1;
    }
    return 1 + sf.getEnd() - sf.getBegin();
  }

  /**
   * Adds the feature to the list of non-positional features (with lazy
   * instantiation of the list if it is null), and returns true. The feature
   * group is added to the set of distinct feature groups for non-positional
   * features. This method allows duplicate features, so test before calling to
   * prevent this.
   * 
   * @param feature
   */
  protected boolean addNonPositionalFeature(SequenceFeatureI feature)
  {
    if (nonPositionalFeatures == null)
    {
      nonPositionalFeatures = new ArrayList<>();
    }

    nonPositionalFeatures.add(feature);

    nonPositionalFeatureGroups.add(feature.getFeatureGroup());

    return true;
  }

  /**
   * Adds one feature to the NCList that can manage nested features (creating
   * the NCList if necessary), and returns true. If the feature is already
   * stored in the NCList (by equality test), then it is not added, and this
   * method returns false.
   */
  protected synchronized boolean addNestedFeature(SequenceFeatureI feature)
  {
    if (nestedFeatures == null)
    {
      nestedFeatures = new NCList<>(feature);
      return true;
    }
    return nestedFeatures.add(feature, false);
  }

  /**
   * Add a feature to the list of non-nested features, maintaining the ordering
   * of the list. A check is made for whether the feature is nested in (properly
   * contained by) an existing feature. If there is no nesting, the feature is
   * added to the list and the method returns true. If nesting is found, the
   * feature is not added and the method returns false.
   * 
   * @param feature
   * @return
   */
  protected boolean addNonNestedFeature(SequenceFeatureI feature)
  {
    synchronized (nonNestedFeatures)
    {
      /*
       * find the first stored feature which doesn't precede the new one
       */
      int insertPosition = binarySearch(nonNestedFeatures, SearchCriterion
              .byFeature(feature, RangeComparator.BY_START_POSITION));

      /*
       * fail if we detect feature enclosure - of the new feature by
       * the one preceding it, or of the next feature by the new one
       */
      if (insertPosition > 0)
      {
        if (encloses(nonNestedFeatures.get(insertPosition - 1), feature))
        {
          return false;
        }
      }
      if (insertPosition < nonNestedFeatures.size())
      {
        if (encloses(feature, nonNestedFeatures.get(insertPosition)))
        {
          return false;
        }
      }

      /*
       * checks passed - add the feature
       */
      nonNestedFeatures.add(insertPosition, feature);

      return true;
    }
  }

  /**
   * Answers true if range1 properly encloses range2, else false
   * 
   * @param range1
   * @param range2
   * @return
   */
  protected boolean encloses(ContiguousI range1, ContiguousI range2)
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
   * Add a contact feature to the lists that hold them ordered by start (first
   * contact) and by end (second contact) position, ensuring the lists remain
   * ordered, and returns true. This method allows duplicate features to be
   * added, so test before calling to avoid this.
   * 
   * @param feature
   * @return
   */
  protected synchronized boolean addContactFeature(SequenceFeatureI feature)
  {
    if (contactFeatureStarts == null)
    {
      contactFeatureStarts = new ArrayList<>();
    }
    if (contactFeatureEnds == null)
    {
      contactFeatureEnds = new ArrayList<>();
    }

    /*
     * binary search the sorted list to find the insertion point
     */
    int insertPosition = binarySearch(contactFeatureStarts, SearchCriterion
            .byFeature(feature, RangeComparator.BY_START_POSITION));
    contactFeatureStarts.add(insertPosition, feature);
    // and resort to mak siccar...just in case insertion point not quite right
    Collections.sort(contactFeatureStarts,
            RangeComparator.BY_START_POSITION);

    insertPosition = binarySearch(contactFeatureStarts, SearchCriterion
            .byFeature(feature, RangeComparator.BY_END_POSITION));
    contactFeatureEnds.add(feature);
    Collections.sort(contactFeatureEnds, RangeComparator.BY_END_POSITION);

    return true;
  }

  /**
   * Answers true if the list contains the feature, else false. This method is
   * optimised for the condition that the list is sorted on feature start
   * position ascending, and will give unreliable results if this does not hold.
   * 
   * @param features
   * @param feature
   * @return
   */
  protected static boolean listContains(List<SequenceFeatureI> features,
          SequenceFeatureI feature)
  {
    if (features == null || feature == null)
    {
      return false;
    }

    /*
     * locate the first entry in the list which does not precede the feature
     */
    int pos = binarySearch(features, SearchCriterion.byFeature(feature,
            RangeComparator.BY_START_POSITION));
    int len = features.size();
    while (pos < len)
    {
      SequenceFeatureI sf = features.get(pos);
      if (sf.getBegin() > feature.getBegin())
      {
        return false; // no match found
      }
      if (sf.equals(feature))
      {
        return true;
      }
      pos++;
    }
    return false;
  }

  /**
   * Returns a (possibly empty) list of features whose extent overlaps the given
   * range. The returned list is not ordered. Contact features are included if
   * either of the contact points lies within the range.
   * 
   * @param start
   *          start position of overlap range (inclusive)
   * @param end
   *          end position of overlap range (inclusive)
   * @return
   */
  public List<SequenceFeatureI> findOverlappingFeatures(long start,
          long end)
  {
    List<SequenceFeatureI> result = new ArrayList<>();

    findNonNestedFeatures(start, end, result);

    findContactFeatures(start, end, result);

    if (nestedFeatures != null)
    {
      result.addAll(nestedFeatures.findOverlaps(start, end));
    }

    return result;
  }

  /**
   * Adds contact features to the result list where either the second or the
   * first contact position lies within the target range
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findContactFeatures(long from, long to,
          List<SequenceFeatureI> result)
  {
    if (contactFeatureStarts != null)
    {
      findContactStartFeatures(from, to, result);
    }
    if (contactFeatureEnds != null)
    {
      findContactEndFeatures(from, to, result);
    }
  }

  /**
   * Adds to the result list any contact features whose end (second contact
   * point), but not start (first contact point), lies in the query from-to
   * range
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findContactEndFeatures(long from, long to,
          List<SequenceFeatureI> result)
  {
    /*
     * find the first contact feature (if any) that does not lie 
     * entirely before the target range
     */
    int startPosition = binarySearch(contactFeatureEnds,
            SearchCriterion.byEnd(from));
    for (; startPosition < contactFeatureEnds.size(); startPosition++)
    {
      SequenceFeatureI sf = contactFeatureEnds.get(startPosition);
      if (!sf.isContactFeature())
      {
        System.err.println("Error! non-contact feature type " + sf.getType()
                + " in contact features list");
        continue;
      }

      int begin = sf.getBegin();
      if (begin >= from && begin <= to)
      {
        /*
         * this feature's first contact position lies in the search range
         * so we don't include it in results a second time
         */
        continue;
      }

      int end = sf.getEnd();
      if (end >= from && end <= to)
      {
        result.add(sf);
      }
      if (end > to)
      {
        break;
      }
    }
  }

  /**
   * Adds non-nested features to the result list that lie within the target
   * range. Non-positional features (start=end=0), contact features and nested
   * features are excluded.
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findNonNestedFeatures(long from, long to,
          List<SequenceFeatureI> result)
  {
    /*
     * find the first feature whose end position is
     * after the target range start
     */
    int startIndex = binarySearch(nonNestedFeatures,
            SearchCriterion.byEnd(from));

    final int startIndex1 = startIndex;
    int i = startIndex1;
    while (i < nonNestedFeatures.size())
    {
      SequenceFeatureI sf = nonNestedFeatures.get(i);
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

  /**
   * Adds contact features whose start position lies in the from-to range to the
   * result list
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findContactStartFeatures(long from, long to,
          List<SequenceFeatureI> result)
  {
    int startPosition = binarySearch(contactFeatureStarts,
            SearchCriterion.byStart(from));

    for (; startPosition < contactFeatureStarts.size(); startPosition++)
    {
      SequenceFeatureI sf = contactFeatureStarts.get(startPosition);
      if (!sf.isContactFeature())
      {
        System.err.println("Error! non-contact feature type " + sf.getType()
                + " in contact features list");
        continue;
      }
      int begin = sf.getBegin();
      if (begin >= from && begin <= to)
      {
        result.add(sf);
      }
    }
  }

  /**
   * Answers a list of all positional features stored, in no guaranteed order
   * 
   * @return
   */
  public List<SequenceFeatureI> getPositionalFeatures()
  {
    /*
     * add non-nested features (may be all features for many cases)
     */
    List<SequenceFeatureI> result = new ArrayList<>();
    result.addAll(nonNestedFeatures);

    /*
     * add any contact features - from the list by start position
     */
    if (contactFeatureStarts != null)
    {
      result.addAll(contactFeatureStarts);
    }

    /*
     * add any nested features
     */
    if (nestedFeatures != null)
    {
      result.addAll(nestedFeatures.getEntries());
    }

    return result;
  }

  /**
   * Answers a list of all contact features. If there are none, returns an
   * immutable empty list.
   * 
   * @return
   */
  public List<SequenceFeatureI> getContactFeatures()
  {
    if (contactFeatureStarts == null)
    {
      return Collections.emptyList();
    }
    return new ArrayList<>(contactFeatureStarts);
  }

  /**
   * Answers a list of all non-positional features. If there are none, returns
   * an immutable empty list.
   * 
   * @return
   */
  public List<SequenceFeatureI> getNonPositionalFeatures()
  {
    if (nonPositionalFeatures == null)
    {
      return Collections.emptyList();
    }
    return new ArrayList<>(nonPositionalFeatures);
  }

  /**
   * Deletes the given feature from the store, returning true if it was found
   * (and deleted), else false. This method makes no assumption that the feature
   * is in the 'expected' place in the store, in case it has been modified since
   * it was added.
   * 
   * @param sf
   */
  public synchronized boolean delete(SequenceFeatureI sf)
  {
    /*
     * try the non-nested positional features first
     */
    boolean removed = nonNestedFeatures.remove(sf);

    /*
     * if not found, try contact positions (and if found, delete
     * from both lists of contact positions)
     */
    if (!removed && contactFeatureStarts != null)
    {
      removed = contactFeatureStarts.remove(sf);
      if (removed)
      {
        contactFeatureEnds.remove(sf);
      }
    }

    boolean removedNonPositional = false;

    /*
     * if not found, try non-positional features
     */
    if (!removed && nonPositionalFeatures != null)
    {
      removedNonPositional = nonPositionalFeatures.remove(sf);
      removed = removedNonPositional;
    }

    /*
     * if not found, try nested features
     */
    if (!removed && nestedFeatures != null)
    {
      removed = nestedFeatures.delete(sf);
    }

    if (removed)
    {
      rescanAfterDelete();
    }

    return removed;
  }

  /**
   * Rescan all features to recompute any cached values after an entry has been
   * deleted. This is expected to be an infrequent event, so performance here is
   * not critical.
   */
  protected synchronized void rescanAfterDelete()
  {
    positionalFeatureGroups.clear();
    nonPositionalFeatureGroups.clear();
    totalExtent = 0;
    positionalMinScore = Float.NaN;
    positionalMaxScore = Float.NaN;
    nonPositionalMinScore = Float.NaN;
    nonPositionalMaxScore = Float.NaN;

    /*
     * scan non-positional features for groups and scores
     */
    for (SequenceFeatureI sf : getNonPositionalFeatures())
    {
      nonPositionalFeatureGroups.add(sf.getFeatureGroup());
      float score = sf.getScore();
      nonPositionalMinScore = min(nonPositionalMinScore, score);
      nonPositionalMaxScore = max(nonPositionalMaxScore, score);
    }

    /*
     * scan positional features for groups, scores and extents
     */
    for (SequenceFeatureI sf : getPositionalFeatures())
    {
      positionalFeatureGroups.add(sf.getFeatureGroup());
      float score = sf.getScore();
      positionalMinScore = min(positionalMinScore, score);
      positionalMaxScore = max(positionalMaxScore, score);
      totalExtent += getFeatureLength(sf);
    }
  }

  /**
   * A helper method to return the minimum of two floats, where a non-NaN value
   * is treated as 'less than' a NaN value (unlike Math.min which does the
   * opposite)
   * 
   * @param f1
   * @param f2
   */
  protected static float min(float f1, float f2)
  {
    if (Float.isNaN(f1))
    {
      return Float.isNaN(f2) ? f1 : f2;
    }
    else
    {
      return Float.isNaN(f2) ? f1 : Math.min(f1, f2);
    }
  }

  /**
   * A helper method to return the maximum of two floats, where a non-NaN value
   * is treated as 'greater than' a NaN value (unlike Math.max which does the
   * opposite)
   * 
   * @param f1
   * @param f2
   */
  protected static float max(float f1, float f2)
  {
    if (Float.isNaN(f1))
    {
      return Float.isNaN(f2) ? f1 : f2;
    }
    else
    {
      return Float.isNaN(f2) ? f1 : Math.max(f1, f2);
    }
  }

  /**
   * Answers true if this store has no features, else false
   * 
   * @return
   */
  public boolean isEmpty()
  {
    boolean hasFeatures = !nonNestedFeatures.isEmpty()
            || (contactFeatureStarts != null
                    && !contactFeatureStarts.isEmpty())
            || (nonPositionalFeatures != null
                    && !nonPositionalFeatures.isEmpty())
            || (nestedFeatures != null && nestedFeatures.size() > 0);

    return !hasFeatures;
  }

  /**
   * Answers the set of distinct feature groups stored, possibly including null,
   * as an unmodifiable view of the set. The parameter determines whether the
   * groups for positional or for non-positional features are returned.
   * 
   * @param positionalFeatures
   * @return
   */
  public Set<String> getFeatureGroups(boolean positionalFeatures)
  {
    if (positionalFeatures)
    {
      return Collections.unmodifiableSet(positionalFeatureGroups);
    }
    else
    {
      return nonPositionalFeatureGroups == null
              ? Collections.<String> emptySet()
              : Collections.unmodifiableSet(nonPositionalFeatureGroups);
    }
  }

  /**
   * Performs a binary search of the (sorted) list to find the index of the
   * first entry which returns true for the given comparator function. Returns
   * the length of the list if there is no such entry.
   * 
   * @param features
   * @param sc
   * @return
   */
  protected static int binarySearch(List<SequenceFeatureI> features,
          SearchCriterion sc)
  {
    int start = 0;
    int end = features.size() - 1;
    int matched = features.size();

    while (start <= end)
    {
      int mid = (start + end) / 2;
      ContiguousI entry = features.get(mid);
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
   * Answers the number of positional (or non-positional) features stored.
   * Contact features count as 1.
   * 
   * @param positional
   * @return
   */
  public int getFeatureCount(boolean positional)
  {
    if (!positional)
    {
      return nonPositionalFeatures == null ? 0
              : nonPositionalFeatures.size();
    }

    int size = nonNestedFeatures.size();

    if (contactFeatureStarts != null)
    {
      // note a contact feature (start/end) counts as one
      size += contactFeatureStarts.size();
    }

    if (nestedFeatures != null)
    {
      size += nestedFeatures.size();
    }

    return size;
  }

  /**
   * Answers the total length of positional features (or zero if there are
   * none). Contact features contribute a value of 1 to the total.
   * 
   * @return
   */
  public int getTotalFeatureLength()
  {
    return totalExtent;
  }

  /**
   * Answers the minimum score held for positional or non-positional features.
   * This may be Float.NaN if there are no features, are none has a non-NaN
   * score.
   * 
   * @param positional
   * @return
   */
  public float getMinimumScore(boolean positional)
  {
    return positional ? positionalMinScore : nonPositionalMinScore;
  }

  /**
   * Answers the maximum score held for positional or non-positional features.
   * This may be Float.NaN if there are no features, are none has a non-NaN
   * score.
   * 
   * @param positional
   * @return
   */
  public float getMaximumScore(boolean positional)
  {
    return positional ? positionalMaxScore : nonPositionalMaxScore;
  }

  /**
   * Answers a list of all either positional or non-positional features whose
   * feature group matches the given group (which may be null)
   * 
   * @param positional
   * @param group
   * @return
   */
  public List<SequenceFeatureI> getFeaturesForGroup(boolean positional,
          String group)
  {
    List<SequenceFeatureI> result = new ArrayList<>();

    /*
     * if we know features don't include the target group, no need
     * to inspect them for matches
     */
    if (positional && !positionalFeatureGroups.contains(group)
            || !positional && !nonPositionalFeatureGroups.contains(group))
    {
      return result;
    }

    List<SequenceFeatureI> sfs = positional ? getPositionalFeatures()
            : getNonPositionalFeatures();
    for (SequenceFeatureI sf : sfs)
    {
      String featureGroup = sf.getFeatureGroup();
      if (group == null && featureGroup == null
              || group != null && group.equals(featureGroup))
      {
        result.add(sf);
      }
    }
    return result;
  }
}
