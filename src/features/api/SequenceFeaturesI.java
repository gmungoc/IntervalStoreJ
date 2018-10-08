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
package features.api;

import java.util.List;
import java.util.Set;

public interface SequenceFeaturesI
{

  /**
   * Adds one sequence feature to the store, and returns true, unless the
   * feature is already contained in the store, in which case this method
   * returns false. Containment is determined by SequenceFeature.equals()
   * comparison. Answers false, and does not add the feature, if feature type is
   * null.
   * 
   * @param sf
   */
  boolean add(SequenceFeatureI sf);

  /**
   * Returns a (possibly empty) list of features, optionally restricted to
   * specified types, which overlap the given (inclusive) sequence position
   * range
   * 
   * @param from
   * @param to
   * @param type
   * @return
   */
  List<SequenceFeatureI> findFeatures(int from, int to, String... type);

  /**
   * Answers a list of all features stored, in no particular guaranteed order.
   * Positional features may optionally be restricted to specified types, but
   * all non-positional features (if any) are always returned.
   * <p>
   * To filter non-positional features by type, use
   * getNonPositionalFeatures(type).
   * 
   * @param type
   * @return
   */
  List<SequenceFeatureI> getAllFeatures(String... type);

  /**
   * Answers a list of all positional (or non-positional) features which are in
   * the specified feature group, optionally restricted to features of specified
   * types.
   * 
   * @param positional
   *          if true returns positional features, else non-positional features
   * @param group
   *          the feature group to be matched (which may be null)
   * @param type
   *          optional feature types to filter by
   * @return
   */
  List<SequenceFeatureI> getFeaturesForGroup(boolean positional,
          String group, String... type);

  /**
   * Answers a list of all features stored, whose type either matches, or is a
   * specialisation (in the Sequence Ontology) of, one of the given terms.
   * Results are returned in no particular order.
   * 
   * @param ontologyTerm
   * @return
   */
  List<SequenceFeatureI> getFeaturesByOntology(String... ontologyTerm);

  /**
   * Answers the number of (positional or non-positional) features, optionally
   * restricted to specified feature types. Contact features are counted as 1.
   * 
   * @param positional
   * @param type
   * @return
   */
  int getFeatureCount(boolean positional, String... type);

  /**
   * Answers the total length of positional features, optionally restricted to
   * specified feature types. Contact features are counted as length 1.
   * 
   * @param type
   * @return
   */
  int getTotalFeatureLength(String... type);

  /**
   * Answers a list of all positional features, optionally restricted to
   * specified types, in no particular guaranteed order
   * 
   * @param type
   * @return
   */
  List<SequenceFeatureI> getPositionalFeatures(String... type);

  /**
   * Answers a list of all contact features, optionally restricted to specified
   * types, in no particular guaranteed order
   * 
   * @return
   */
  List<SequenceFeatureI> getContactFeatures(String... type);

  /**
   * Answers a list of all non-positional features, optionally restricted to
   * specified types, in no particular guaranteed order
   * 
   * @param type
   *          if no type is specified, all are returned
   * @return
   */
  List<SequenceFeatureI> getNonPositionalFeatures(String... type);

  /**
   * Deletes the given feature from the store, returning true if it was found
   * (and deleted), else false. This method makes no assumption that the feature
   * is in the 'expected' place in the store, in case it has been modified since
   * it was added.
   * 
   * @param sf
   */
  boolean delete(SequenceFeatureI sf);

  /**
   * Answers true if this store contains at least one feature, else false
   * 
   * @return
   */
  boolean hasFeatures();

  /**
   * Returns a set of the distinct feature groups present in the collection. The
   * set may include null. The boolean parameter determines whether the groups
   * for positional or for non-positional features are returned. The optional
   * type parameter may be used to restrict to groups for specified feature
   * types.
   * 
   * @param positionalFeatures
   * @param type
   * @return
   */
  Set<String> getFeatureGroups(boolean positionalFeatures, String... type);

  /**
   * Answers the set of distinct feature types for which there is at least one
   * feature with one of the given feature group(s). The boolean parameter
   * determines whether the groups for positional or for non-positional features
   * are returned.
   * 
   * @param positionalFeatures
   * @param groups
   * @return
   */
  Set<String> getFeatureTypesForGroups(boolean positionalFeatures,
          String... groups);

  /**
   * Answers a set of the distinct feature types for which a feature is stored.
   * The types may optionally be restricted to those which match, or are a
   * subtype of, given sequence ontology terms
   * 
   * @return
   */
  Set<String> getFeatureTypes(String... soTerm);

  /**
   * Answers the minimum score held for positional or non-positional features
   * for the specified type. This may be Float.NaN if there are no features, or
   * none has a non-NaN score.
   * 
   * @param type
   * @param positional
   * @return
   */
  float getMinimumScore(String type, boolean positional);

  /**
   * Answers the maximum score held for positional or non-positional features
   * for the specified type. This may be Float.NaN if there are no features, or
   * none has a non-NaN score.
   * 
   * @param type
   * @param positional
   * @return
   */
  float getMaximumScore(String type, boolean positional);
}