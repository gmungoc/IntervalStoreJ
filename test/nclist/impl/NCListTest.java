package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import junit.extensions.PA;
import nclist.api.ContiguousI;

public class NCListTest
{

  private Random random = new Random(107);

  private Comparator<ContiguousI> sorter = new RangeComparator(true);

  /**
   * A basic sanity test of the constructor
   */
  @Test(groups = "Functional")
  public void testConstructor()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 20));
    ranges.add(new Range(10, 20));
    ranges.add(new Range(15, 30));
    ranges.add(new Range(10, 30));
    ranges.add(new Range(11, 19));
    ranges.add(new Range(10, 20));
    ranges.add(new Range(1, 100));

    NCListI<Range> ncl = new NCList<Range>(ranges);
    String expected = "[1-100 [10-30 [10-20 [10-20 [11-19]]]], 15-30 [20-20]]";
    assertEquals(ncl.toString(), expected);
    assertTrue(ncl.isValid());

    Collections.reverse(ranges);
    ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), expected);
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testFindOverlaps()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 50));
    ranges.add(new Range(30, 70));
    ranges.add(new Range(1, 100));
    ranges.add(new Range(70, 120));

    NCListI<Range> ncl = new NCList<Range>(ranges);

    List<Range> overlaps = ncl.findOverlaps(121, 122);
    assertEquals(overlaps.size(), 0);

    overlaps = ncl.findOverlaps(21, 22);
    assertEquals(overlaps.size(), 2);
    assertEquals(((ContiguousI) overlaps.get(0)).getBegin(), 1);
    assertEquals(((ContiguousI) overlaps.get(0)).getEnd(), 100);
    assertEquals(((ContiguousI) overlaps.get(1)).getBegin(), 20);
    assertEquals(((ContiguousI) overlaps.get(1)).getEnd(), 50);

    overlaps = ncl.findOverlaps(110, 110);
    assertEquals(overlaps.size(), 1);
    assertEquals(((ContiguousI) overlaps.get(0)).getBegin(), 70);
    assertEquals(((ContiguousI) overlaps.get(0)).getEnd(), 120);
  }

  @Test(groups = "Functional")
  public void testAdd_onTheEnd()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(60, 70));
    assertEquals(ncl.toString(), "[20-50, 60-70]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_inside()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(30, 40));
    assertEquals(ncl.toString(), "[20-50 [30-40]]");
  }

  @Test(groups = "Functional")
  public void testAdd_onTheFront()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(5, 15));
    assertEquals(ncl.toString(), "[5-15, 20-50]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_enclosing()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 50));
    ranges.add(new Range(30, 60));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-50, 30-60]");
    assertTrue(ncl.isValid());
    assertEquals(ncl.getStart(), 20);

    ncl.add(new Range(10, 70));
    assertEquals(ncl.toString(), "[10-70 [20-50, 30-60]]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_spanning()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(20, 40));
    ranges.add(new Range(60, 70));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-40, 60-70]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(30, 50));
    assertEquals(ncl.toString(), "[20-40, 30-50, 60-70]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(40, 65));
    assertEquals(ncl.toString(), "[20-40, 30-50, 40-65, 60-70]");
    assertTrue(ncl.isValid());
  }

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

  /**
   * Do a number of pseudo-random (reproducible) builds of an NCList, to
   * exercise as many methods of the class as possible while generating the
   * range of possible structure topologies
   * <ul>
   * <li>verify that add adds an entry and increments size</li>
   * <li>...except where the entry is already contained (by equals test)</li>
   * <li>verify that the structure is valid at all stages of construction</li>
   * <li>generate, run and verify a range of overlap queries</li>
   * <li>tear down the structure by deleting entries, verifying correctness at
   * each stage</li>
   * </ul>
   */
  @Test(groups = "Functional", dataProvider = "scalesOfLife")
  public void test_pseudoRandom(Integer scale)
  {
    NCListI<SimpleFeature> ncl = new NCList<>();
    List<SimpleFeature> features = new ArrayList<SimpleFeature>(
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
  protected void testDelete_pseudoRandom(NCListI<SimpleFeature> ncl,
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

      ncl.delete(entry);
      assertFalse(ncl.contains(entry),
              String.format(
                      "NCList still contains deleted entry [%d] '%s'!",
                      deleted, entry.toString()));
      features.remove(toDelete);
      deleted++;

      assertTrue(ncl.isValid(), String.format(
              "NCList invalid after %d deletions, last deleted was '%s'",
              deleted, entry.toString()));

      /*
       * brute force check that deleting one entry didn't delete any others
       */
      for (int i = 0; i < features.size(); i++)
      {
        SimpleFeature sf = features.get(i);
        assertTrue(ncl.contains(sf), String.format(
                "NCList doesn't contain entry [%d] %s after deleting '%s'!",
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
          NCListI<SimpleFeature> ncl, List<SimpleFeature> features)
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
      ncl.add(feature, false);
      if (features.contains(feature))
      {
        System.out.println(
                "Duplicate feature generated " + feature.toString());
      }
      else
      {
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
  protected void testFindOverlaps_pseudoRandom(NCListI<SimpleFeature> ncl,
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
  protected void verifyFindOverlaps(NCListI<SimpleFeature> ncl, int from,
          int to, List<SimpleFeature> features)
  {
    List<SimpleFeature> overlaps = ncl.findOverlaps(from, to);

    /*
     * check returned entries do indeed overlap from-to range
     */
    for (ContiguousI sf : overlaps)
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
    for (ContiguousI sf : features)
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

  @Test(groups = "Functional")
  public void testGetEntries()
  {
    List<Range> ranges = new ArrayList<Range>();
    Range r1 = new Range(20, 20);
    Range r2 = new Range(10, 20);
    Range r3 = new Range(15, 30);
    Range r4 = new Range(10, 30);
    Range r5 = new Range(11, 19);
    Range r6 = new Range(10, 20);
    ranges.add(r1);
    ranges.add(r2);
    ranges.add(r3);
    ranges.add(r4);
    ranges.add(r5);
    ranges.add(r6);

    NCList<Range> ncl = new NCList<Range>(ranges);
    Range r7 = new Range(1, 100);
    ncl.add(r7);

    List<Range> contents = ncl.getEntries();
    assertEquals(contents.size(), 7);
    assertTrue(contents.contains(r1));
    assertTrue(contents.contains(r2));
    assertTrue(contents.contains(r3));
    assertTrue(contents.contains(r4));
    assertTrue(contents.contains(r5));
    assertTrue(contents.contains(r6));
    assertTrue(contents.contains(r7));

    ncl = new NCList<Range>();
    assertTrue(ncl.getEntries().isEmpty());
  }

  @Test(groups = "Functional")
  public void testDelete()
  {
    List<Range> ranges = new ArrayList<Range>();
    Range r1 = new Range(20, 30);
    ranges.add(r1);
    NCListI<Range> ncl = new NCList<Range>(ranges);
    assertTrue(ncl.getEntries().contains(r1));

    Range r2 = new Range(20, 30);
    assertFalse(ncl.delete(null)); // null argument
    assertFalse(ncl.delete(r2)); // never added
    assertTrue(ncl.delete(r1)); // success
    assertTrue(ncl.getEntries().isEmpty());

    /*
     * tests where object.equals() == true
     */
    NCList<SimpleFeature> features = new NCList<>();
    SimpleFeature sf1 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "type");
    features.add(sf1);
    assertEquals(sf1, sf2); // sf1.equals(sf2)
    assertFalse(features.delete(sf2)); // equality is not enough for deletion
    assertTrue(features.getEntries().contains(sf1)); // still there!
    assertTrue(features.delete(sf1));
    assertTrue(features.getEntries().isEmpty()); // gone now

    /*
     * test with duplicate objects in NCList
     */
    features.add(sf1);
    features.add(sf1);
    assertEquals(features.getEntries().size(), 2);
    assertSame(features.getEntries().get(0), sf1);
    assertSame(features.getEntries().get(1), sf1);
    assertTrue(features.delete(sf1)); // first match only is deleted
    assertTrue(features.contains(sf1));
    assertEquals(features.size(), 1);
    assertTrue(features.delete(sf1));
    assertTrue(features.getEntries().isEmpty());
  }

  @Test(groups = "Functional")
  public void testAdd_overlapping()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(40, 50));
    ranges.add(new Range(20, 30));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[20-30, 40-50]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping internally
     */
    ncl.add(new Range(25, 35));
    assertEquals(ncl.toString(), "[20-30, 25-35, 40-50]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping last range
     */
    ncl.add(new Range(45, 55));
    assertEquals(ncl.toString(), "[20-30, 25-35, 40-50, 45-55]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping first range
     */
    ncl.add(new Range(15, 25));
    assertEquals(ncl.toString(), "[15-25, 20-30, 25-35, 40-50, 45-55]");
    assertTrue(ncl.isValid());
  }

  /**
   * Test the contains method (which uses object equals test)
   */
  @Test(groups = "Functional")
  public void testContains()
  {
    NCList<SimpleFeature> ncl = new NCList<>();
    SimpleFeature sf1 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf3 = new SimpleFeature(1, 10, "type2");
    ncl.add(sf1);

    assertTrue(ncl.contains(sf1));
    assertTrue(ncl.contains(sf2)); // sf1.equals(sf2)
    assertFalse(ncl.contains(sf3)); // !sf1.equals(sf3)

    /*
     * make some deeper structure in the NCList
     */
    SimpleFeature sf4 = new SimpleFeature(2, 9, "type");
    ncl.add(sf4);
    assertTrue(ncl.contains(sf4));
    SimpleFeature sf5 = new SimpleFeature(4, 5, "type");
    SimpleFeature sf6 = new SimpleFeature(6, 8, "type");
    ncl.add(sf5);
    ncl.add(sf6);
    assertTrue(ncl.contains(sf5));
    assertTrue(ncl.contains(sf6));
  }

  @Test(groups = "Functional")
  public void testIsValid()
  {
    List<Range> ranges = new ArrayList<Range>();
    Range r1 = new Range(40, 50);
    ranges.add(r1);
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertTrue(ncl.isValid());

    Range r2 = new Range(42, 44);
    ncl.add(r2);
    assertTrue(ncl.isValid());
    Range r3 = new Range(46, 48);
    ncl.add(r3);
    assertTrue(ncl.isValid());
    Range r4 = new Range(43, 43);
    ncl.add(r4);
    assertTrue(ncl.isValid());

    assertEquals(ncl.toString(), "[40-50 [42-44 [43-43], 46-48]]");
    assertTrue(ncl.isValid());

    PA.setValue(r1, "start", 43);
    assertFalse(ncl.isValid()); // r2 not inside r1
    PA.setValue(r1, "start", 40);
    assertTrue(ncl.isValid());

    PA.setValue(r3, "start", 41);
    assertFalse(ncl.isValid()); // r3 should precede r2
    PA.setValue(r3, "start", 46);
    assertTrue(ncl.isValid());

    PA.setValue(r4, "start", 41);
    assertFalse(ncl.isValid()); // r4 not inside r2
    PA.setValue(r4, "start", 43);
    assertTrue(ncl.isValid());

    PA.setValue(r4, "start", 44);
    assertFalse(ncl.isValid()); // r4 has reverse range
  }

  @Test(groups = "Functional")
  public void testPrettyPrint()
  {
    /*
     * construct NCList from a list of ranges
     * they are sorted then assembled into NCList subregions
     * notice that 42-42 end up inside 41-46
     */
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(40, 50));
    ranges.add(new Range(45, 55));
    ranges.add(new Range(40, 45));
    ranges.add(new Range(41, 46));
    ranges.add(new Range(42, 42));
    ranges.add(new Range(42, 42));
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[40-50 [40-45], 41-46 [42-42 [42-42]], 45-55]");
    String expected = "40-50\n  40-45\n41-46\n  42-42\n    42-42\n45-55\n";
    assertEquals(ncl.prettyPrint(), expected);

    /*
     * repeat but now add ranges one at a time
     * notice that 42-42 end up inside 40-50 so we get
     * a different but equal valid NCList structure
     */
    ranges.clear();
    ncl = new NCList<Range>(ranges);
    ncl.add(new Range(40, 50));
    ncl.add(new Range(45, 55));
    ncl.add(new Range(40, 45));
    ncl.add(new Range(41, 46));
    ncl.add(new Range(42, 42));
    ncl.add(new Range(42, 42));
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[40-50 [40-45 [42-42 [42-42]], 41-46], 45-55]");
    expected = "40-50\n  40-45\n    42-42\n      42-42\n  41-46\n45-55\n";
    assertEquals(ncl.prettyPrint(), expected);
  }

  /**
   * A test that shows different valid trees can be constructed from the same
   * set of ranges, depending on the order of construction
   */
  @Test(groups = "Functional")
  public void testConstructor_alternativeTrees()
  {
    List<Range> ranges = new ArrayList<Range>();
    ranges.add(new Range(10, 60));
    ranges.add(new Range(20, 30));
    ranges.add(new Range(40, 50));

    /*
     * constructor with greedy traversal of sorted ranges to build nested
     * containment lists results in 20-30 inside 10-60, 40-50 a sibling
     */
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertEquals(ncl.toString(), "[10-60 [20-30], 40-50]");
    assertTrue(ncl.isValid());

    /*
     * adding ranges one at a time results in 40-50 
     * a sibling of 20-30 inside 10-60
     */
    ncl = new NCList<Range>(new Range(10, 60));
    ncl.add(new Range(20, 30));
    ncl.add(new Range(40, 50));
    assertEquals(ncl.toString(), "[10-60 [20-30, 40-50]]");
    assertTrue(ncl.isValid());
  }
}
