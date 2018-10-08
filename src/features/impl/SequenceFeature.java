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

import features.api.SequenceFeatureI;

/**
 * A class that models a single contiguous feature on a sequence. If flag
 * 'contactFeature' is true, the start and end positions are interpreted instead
 * as two contact points.
 */
public class SequenceFeature implements SequenceFeatureI
{
  private static final float NO_SCORE = 0f;

  /*
   * type, begin, end, featureGroup, score and contactFeature are final to ensure
   * that the integrity of SequenceFeatures data store can't be broken by direct
   * update of these fields
   */
  public final String type;

  public final int begin;

  public final int end;

  public final String featureGroup;

  public final float score;

  private final boolean contactFeature;

  public String description;

  /**
   * Constructs a duplicate feature. Note: Uses makes a shallow copy of the
   * otherDetails map, so the new and original SequenceFeature may reference the
   * same objects in the map.
   * 
   * @param cpy
   */
  public SequenceFeature(SequenceFeature cpy)
  {
    this(cpy, cpy.getBegin(), cpy.getEnd(), cpy.getFeatureGroup(),
            cpy.getScore());
  }

  /**
   * Constructor
   * 
   * @param theType
   * @param theDesc
   * @param theBegin
   * @param theEnd
   * @param group
   */
  public SequenceFeature(String theType, String theDesc, int theBegin,
          int theEnd, String group)
  {
    this(theType, theDesc, theBegin, theEnd, NO_SCORE, group);
  }

  /**
   * Constructor including a score value
   * 
   * @param theType
   * @param theDesc
   * @param theBegin
   * @param theEnd
   * @param theScore
   * @param group
   */
  public SequenceFeature(String theType, String theDesc, int theBegin,
          int theEnd, float theScore, String group)
  {
    this.type = theType;
    this.description = theDesc;
    this.begin = theBegin;
    this.end = theEnd;
    this.featureGroup = group;
    this.score = theScore;

    /*
     * for now, only "Disulfide/disulphide bond" is treated as a contact feature
     */
    this.contactFeature = "disulfide bond".equalsIgnoreCase(type)
            || "disulphide bond".equalsIgnoreCase(type);
  }

  /**
   * A copy constructor that allows the value of final fields to be 'modified'
   * 
   * @param sf
   * @param newType
   * @param newBegin
   * @param newEnd
   * @param newGroup
   * @param newScore
   */
  public SequenceFeature(SequenceFeatureI sf, String newType, int newBegin,
          int newEnd, String newGroup, float newScore)
  {
    this(newType, sf.getDescription(), newBegin, newEnd, newScore,
            newGroup);
  }

  /**
   * A copy constructor that allows the value of final fields to be 'modified'
   * 
   * @param sf
   * @param newBegin
   * @param newEnd
   * @param newGroup
   * @param newScore
   */
  public SequenceFeature(SequenceFeatureI sf, int newBegin, int newEnd,
          String newGroup, float newScore)
  {
    this(sf, sf.getType(), newBegin, newEnd, newGroup, newScore);
  }

  /**
   * Two features are considered equal if they have the same type, group,
   * description, start, end, phase, strand, and (if present) 'Name', ID' and
   * 'Parent' attributes.
   * 
   * Note we need to check Parent to distinguish the same exon occurring in
   * different transcripts (in Ensembl GFF). This allows assembly of transcript
   * sequences from their component exon regions.
   */
  @Override
  public boolean equals(Object o)
  {
    return equals(o, false);
  }

  /**
   * Overloaded method allows the equality test to optionally ignore the
   * 'Parent' attribute of a feature. This supports avoiding adding many
   * superficially duplicate 'exon' or CDS features to genomic or protein
   * sequence.
   * 
   * @param o
   * @param ignoreParent
   * @return
   */
  public boolean equals(Object o, boolean ignoreParent)
  {
    if (o == null || !(o instanceof SequenceFeature))
    {
      return false;
    }

    SequenceFeature sf = (SequenceFeature) o;
    boolean sameScore = Float.isNaN(score) ? Float.isNaN(sf.score)
            : score == sf.score;
    if (begin != sf.begin || end != sf.end || !sameScore)
    {
      return false;
    }

    if (!(type + description + featureGroup)
            .equals(sf.type + sf.description + sf.featureGroup))
    {
      return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getBegin()
   */
  @Override
  public int getBegin()
  {
    return begin;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getEnd()
   */
  @Override
  public int getEnd()
  {
    return end;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getType()
   */
  @Override
  public String getType()
  {
    return type;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getDescription()
   */
  @Override
  public String getDescription()
  {
    return description;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#setDescription(java.lang.String)
   */
  @Override
  public void setDescription(String desc)
  {
    description = desc;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getFeatureGroup()
   */
  @Override
  public String getFeatureGroup()
  {
    return featureGroup;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#getScore()
   */
  @Override
  public float getScore()
  {
    return score;
  }

  /**
   * Readable representation, for debug only, not guaranteed not to change
   * between versions
   */
  @Override
  public String toString()
  {
    return String.format("%d %d %s %s", getBegin(), getEnd(), getType(),
            getDescription());
  }

  /**
   * Overridden to ensure that whenever two objects are equal, they have the
   * same hashCode
   */
  @Override
  public int hashCode()
  {
    String s = getType() + getDescription() + getFeatureGroup();
    return s.hashCode() + getBegin() + getEnd() + (int) getScore();
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#isContactFeature()
   */
  @Override
  public boolean isContactFeature()
  {
    return contactFeature;
  }

  /* (non-Javadoc)
   * @see features.impl.SequenceFeatureI#isNonPositional()
   */
  @Override
  public boolean isNonPositional()
  {
    return begin == 0 && end == 0;
  }
}
