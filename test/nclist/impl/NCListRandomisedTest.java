package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nclist.api.IntervalI;

/**
 * Does a number of pseudo-random (reproducible) tests of an NCList, to exercise
 * as many methods of the class as possible while generating the range of
 * possible structure topologies
 * <ul>
 * <li>verifies that <code>add</code> adds an entry and increments size</li>
 * <li>verifies that the structure is valid at all stages of construction</li>
 * <li>generates, runs and verifies (by brute force) a range of overlap
 * queries</li>
 * <li>tears down the structure by deleting entries, verifying correctness after
 * each deletion</li>
 * </ul>
 */
public class NCListRandomisedTest
{
  /*
   * use a fixed random seed for reproducible test behaviour
   */
  private Random random = new Random(107);

  private Comparator<IntervalI> sorter = new RangeComparator(true);

  /**
   * Provides the scales for pseudo-random NCLists i.e. the range of the maximal
   * [0-scale] interval to be stored
   * 
   * @return
   */
  @DataProvider(name = "scalesOfLife")
  public Object[][] getScales()
  {
    return new Object[][] { new Integer[] { 10 }, new Integer[] { 100 } };
  }

  @Test(groups = "Functional", dataProvider = "scalesOfLife")
  public void test_pseudoRandom(Integer scale)
  {
    NCList<SimpleFeature> ncl = new NCList<>();
    List<SimpleFeature> features = new ArrayList<>(
            scale);

    testAdd_pseudoRandom(scale, ncl, features);

    /*
     * sort the list of added ranges - this doesn't affect the test,
     * just makes it easier to inspect the data in the debugger
     */
    Collections.sort(features, sorter);

    testFindOverlaps_pseudoRandom(ncl, scale, features);

    testDelete_pseudoRandom(ncl, features);
  }

  /**
   * Pick randomly selected entries to delete in turn, checking the NCList size
   * and validity at each stage, until it is empty
   * 
   * @param ncl
   * @param features
   */
  protected void testDelete_pseudoRandom(NCList<SimpleFeature> ncl,
          List<SimpleFeature> features)
  {
    int deleted = 0;

    while (!features.isEmpty())
    {
      assertEquals(ncl.size(), features.size());
      int toDelete = random.nextInt(features.size());
      SimpleFeature entry = features.get(toDelete);
      assertTrue(ncl.contains(entry),
              String.format("NCList doesn't contain entry [%d] '%s'!",
                      deleted, entry.toString()));

      String pp = ncl.prettyPrint();
      ncl.remove(entry);
      assertFalse(ncl.contains(entry),
              String.format(
                      "NCList still contains deleted entry [%d] '%s'!",
                      deleted, entry.toString()));
      features.remove(toDelete);
      deleted++;

      boolean valid = ncl.isValid();
      if (!valid)
      {
        System.err.println(String.format("Before\n%s\nAfter\n%s\n", pp,
                ncl.prettyPrint()));
      }
      assertTrue(valid, String.format(
              "NCList invalid after %d deletions, last deleted was '%s'",
              deleted, entry.toString()));

      /*
       * brute force check that deleting one entry didn't delete any others
       */
      for (int i = 0; i < features.size(); i++)
      {
        SimpleFeature sf = features.get(i);
        assertTrue(ncl.contains(sf), String.format(
                "NCList doesn't contain entry [%d] '%s' after deleting '%s'!",
                i, sf.toString(), entry.toString()));
      }
    }
    assertEquals(ncl.size(), 0); // all gone
  }

  /**
   * Randomly generate entries and add them to the NCList, checking its validity
   * and size at each stage. A few entries should be duplicates (by equals test)
   * so not get added.
   * 
   * @param scale
   * @param ncl
   * @param features
   */
  protected void testAdd_pseudoRandom(Integer scale,
          NCList<SimpleFeature> ncl, List<SimpleFeature> features)
  {
    int count = 0;
    final int size = 50;

    for (int i = 0; i < size; i++)
    {
      int r1 = random.nextInt(scale + 1);
      int r2 = random.nextInt(scale + 1);
      int from = Math.min(r1, r2);
      int to = Math.max(r1, r2);

      /*
       * choice of two feature types means that occasionally an identical
       * feature may be generated, in which case it should not be added 
       */
      String desc = i % 2 == 0 ? "Pfam" : "Cath";
      SimpleFeature feature = new SimpleFeature(from, to, desc);

      /*
       * add to NCList - with duplicate entries (by equals) disallowed
       */
      if (features.contains(feature))
      {
        assertTrue(ncl.contains(feature));
        System.out.println(
                "Duplicate feature generated " + feature.toString());
      }
      else
      {
        ncl.add(feature);
        features.add(feature);
        count++;
      }

      /*
       * check list format is valid at each stage of its construction
       */
      assertTrue(ncl.isValid(),
              String.format("Failed for scale = %d, i=%d", scale, i));
      assertEquals(ncl.size(), count);
    }
    // System.out.println(ncl.prettyPrint());
  }

  /**
   * A helper method that generates pseudo-random range queries and veries that
   * findOverlaps returns the correct matches
   * 
   * @param ncl
   *          the NCList to query
   * @param scale
   *          ncl maximal range is [0, scale]
   * @param features
   *          a list of the ranges stored in ncl
   */
  protected void testFindOverlaps_pseudoRandom(NCList<SimpleFeature> ncl,
          int scale, List<SimpleFeature> features)
  {
    int halfScale = scale / 2;
    int minIterations = 20;

    /*
     * generates ranges in [-halfScale, scale+halfScale]
     * - some should be internal to [0, scale] P = 1/4
     * - some should lie before 0 P = 1/16
     * - some should lie after scale P = 1/16
     * - some should overlap left P = 1/4
     * - some should overlap right P = 1/4
     * - some should enclose P = 1/8
     * 
     * 50 iterations give a 96% probability of including the
     * unlikeliest case; keep going until we have done all!
     */
    boolean inside = false;
    boolean enclosing = false;
    boolean before = false;
    boolean after = false;
    boolean overlapLeft = false;
    boolean overlapRight = false;
    boolean allCasesCovered = false;

    int i = 0;
    while (i < minIterations || !allCasesCovered)
    {
      i++;
      int r1 = random.nextInt((scale + 1) * 2);
      int r2 = random.nextInt((scale + 1) * 2);
      int from = Math.min(r1, r2) - halfScale;
      int to = Math.max(r1, r2) - halfScale;

      /*
       * ensure all cases of interest get covered
       */
      inside |= from >= 0 && to <= scale;
      enclosing |= from <= 0 && to >= scale;
      before |= to < 0;
      after |= from > scale;
      overlapLeft |= from < 0 && to >= 0 && to <= scale;
      overlapRight |= from >= 0 && from <= scale && to > scale;
      if (!allCasesCovered)
      {
        allCasesCovered |= inside && enclosing && before && after
                && overlapLeft && overlapRight;
        if (allCasesCovered)
        {
          System.out.println(String.format(
                  "Covered all findOverlaps cases after %d iterations for scale %d",
                  i, scale));
        }
      }

      verifyFindOverlaps(ncl, from, to, features);
    }
  }

  /**
   * A helper method that verifies that overlaps found by interrogating an
   * NCList correctly match those found by brute force search
   * 
   * @param ncl
   * @param from
   * @param to
   * @param features
   */
  protected void verifyFindOverlaps(NCList<SimpleFeature> ncl, int from,
          int to, List<SimpleFeature> features)
  {
    List<SimpleFeature> overlaps = ncl.findOverlaps(from, to);

    /*
     * check returned entries do indeed overlap from-to range
     */
    for (IntervalI sf : overlaps)
    {
      int begin = sf.getBegin();
      int end = sf.getEnd();
      assertTrue(begin <= to && end >= from,
              String.format(
                      "[%d, %d] does not overlap query range [%d, %d]",
                      begin, end, from, to));
    }

    /*
     * check overlapping ranges are included in the results
     * (the test above already shows non-overlapping ranges are not)
     */
    for (IntervalI sf : features)
    {
      int begin = sf.getBegin();
      int end = sf.getEnd();
      if (begin <= to && end >= from)
      {
        boolean found = overlaps.contains(sf);
        assertTrue(found,
                String.format("[%d, %d] missing in query range [%d, %d]",
                        begin, end, from, to));
      }
    }
  }
}
