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
package ontology.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ontology.api.OntologyI;

/**
 * An implementation of SequenceOntologyI that hard codes selected terms of
 * interest
 *
 * @author gmcarstairs
 */
public class SequenceOntologyLite implements OntologyI
{
  /*
   * initial selection of types of interest when processing Ensembl features
   * NB unlike the full SequenceOntology we don't traverse indirect
   * child-parent relationships here so e.g. need to list every sub-type
   * (direct or indirect) that is of interest
   */
  // @formatter:off
  private final String[][] TERMS = new String[][] {

    /*
     * gene sub-types:
     */
    { "gene", "gene" }, 
    { "ncRNA_gene", "gene" }, 
    { "snRNA_gene", "gene" },
    { "miRNA_gene", "gene" },
    { "lincRNA_gene", "gene" },
    { "rRNA_gene", "gene" },
    
    /*
     * transcript sub-types:
     */
    { "transcript", "transcript" }, 
    { "mature_transcript", "transcript" }, 
    { "processed_transcript", "transcript" }, 
    { "aberrant_processed_transcript", "transcript" },
    { "ncRNA", "transcript" },
    { "snRNA", "transcript" },
    { "miRNA", "transcript" },
    { "lincRNA", "transcript" },
    { "rRNA", "transcript" },
    { "mRNA", "transcript" },
    // there are many more sub-types of ncRNA...
    
    /*
     * sequence_variant sub-types
     */
    { "sequence_variant", "sequence_variant" },
    { "structural_variant", "sequence_variant" },
    { "feature_variant", "sequence_variant" },
    { "gene_variant", "sequence_variant" },
    { "transcript_variant", "sequence_variant" },
    // NB Ensembl uses NMD_transcript_variant as if a 'transcript'
    // but we model it here correctly as per the SO
    { "NMD_transcript_variant", "sequence_variant" },
    { "missense_variant", "sequence_variant" },
    { "synonymous_variant", "sequence_variant" },
    { "frameshift_variant", "sequence_variant" },
    { "5_prime_UTR_variant", "sequence_variant" },
    { "3_prime_UTR_variant", "sequence_variant" },
    { "stop_gained", "sequence_variant" },
    { "stop_lost", "sequence_variant" },
    { "inframe_deletion", "sequence_variant" },
    { "inframe_insertion", "sequence_variant" },
    { "splice_region_variant", "sequence_variant" },
    
    /*
     * no sub-types of exon or CDS yet seen in Ensembl
     * some added here for testing purposes
     */
    { "exon", "exon" },
    { "coding_exon", "exon" },
    { "CDS", "CDS" },
    { "CDS_predicted", "CDS" },
    
    /*
     * terms used in exonerate or PASA GFF
     */
    { "protein_match", "protein_match"},
    { "nucleotide_match", "nucleotide_match"},
    { "cDNA_match", "nucleotide_match"},
    
    /*
     * used in InterProScan GFF
     */
    { "polypeptide", "polypeptide" }
  };
  // @formatter:on

  /*
   * hard-coded list of any parents (direct or indirect) 
   * that we care about for a term
   */
  private Map<String, List<String>> parents;

  private List<String> termsFound;

  private List<String> termsNotFound;

  public SequenceOntologyLite()
  {
    termsFound = new ArrayList<>();
    termsNotFound = new ArrayList<>();
    loadStaticData();
  }

  /**
   * Loads hard-coded data into a lookup table of {term, {list_of_parents}}
   */
  private void loadStaticData()
  {
    parents = new HashMap<>();
    for (String[] pair : TERMS)
    {
      List<String> p = parents.get(pair[0]);
      if (p == null)
      {
        p = new ArrayList<>();
        parents.put(pair[0], p);
      }
      p.add(pair[1]);
    }
  }

  /**
   * Answers true if 'child' isA 'parent' (including equality). In this
   * implementation, based only on hard-coded values.
   */
  @Override
  public boolean isA(String child, String parent)
  {
    if (child == null || parent == null)
    {
      return false;
    }
    if (child.equals(parent))
    {
      termFound(child);
      return true;
    }

    List<String> p = parents.get(child);
    if (p == null)
    {
      termNotFound(child);
      return false;
    }
    termFound(child);
    if (p.contains(parent))
    {
      return true;
    }
    return false;
  }

  /**
   * Records a valid term queried for, for reporting purposes
   * 
   * @param term
   */
  private void termFound(String term)
  {
    if (!termsFound.contains(term))
    {
      synchronized (termsFound)
      {
        termsFound.add(term);
      }
    }
  }

  /**
   * Records an invalid term queried for, for reporting purposes
   * 
   * @param term
   */
  private void termNotFound(String term)
  {
    synchronized (termsNotFound)
    {
      if (!termsNotFound.contains(term))
      {
        // suppress logging here as it reports Uniprot sequence features
        // (which do not use SO terms) when auto-configuring feature colours
        // System.out.println("SO term " + term
        // + " not known - add to model if needed in "
        // + getClass().getName());
        termsNotFound.add(term);
      }
    }
  }

  /**
   * Sorts (case-insensitive) and returns the list of valid terms queried for
   */
  @Override
  public List<String> termsFound()
  {
    synchronized (termsFound)
    {
      Collections.sort(termsFound, String.CASE_INSENSITIVE_ORDER);
      return termsFound;
    }
  }

  /**
   * Sorts (case-insensitive) and returns the list of invalid terms queried for
   */
  @Override
  public List<String> termsNotFound()
  {
    synchronized (termsNotFound)
    {
      Collections.sort(termsNotFound, String.CASE_INSENSITIVE_ORDER);
      return termsNotFound;
    }
  }
}
