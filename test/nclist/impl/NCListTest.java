package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import junit.extensions.PA;
import nclist.api.IntervalI;

public class NCListTest
{

  private Random random = new Random(107);

  private Comparator<IntervalI> sorter = new RangeComparator(true);

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

    NCList<Range> ncl = new NCList<Range>(ranges);
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

    NCList<Range> ncl = new NCList<Range>(ranges);

    List<Range> overlaps = ncl.findOverlaps(121, 122);
    assertEquals(overlaps.size(), 0);

    overlaps = ncl.findOverlaps(21, 22);
    assertEquals(overlaps.size(), 2);
    assertEquals(((IntervalI) overlaps.get(0)).getBegin(), 1);
    assertEquals(((IntervalI) overlaps.get(0)).getEnd(), 100);
    assertEquals(((IntervalI) overlaps.get(1)).getBegin(), 20);
    assertEquals(((IntervalI) overlaps.get(1)).getEnd(), 50);

    overlaps = ncl.findOverlaps(110, 110);
    assertEquals(overlaps.size(), 1);
    assertEquals(((IntervalI) overlaps.get(0)).getBegin(), 70);
    assertEquals(((IntervalI) overlaps.get(0)).getEnd(), 120);
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
    NCList<SimpleFeature> ncl = new NCList<>();
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

      ncl.remove(entry);
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
  public void testRemove()
  {
    List<Range> ranges = new ArrayList<Range>();
    Range r1 = new Range(20, 30);
    ranges.add(r1);
    NCList<Range> ncl = new NCList<Range>(ranges);
    assertTrue(ncl.contains(r1));

    Range r2 = new Range(20, 30);
    assertFalse(ncl.remove(null)); // null argument
    assertFalse(ncl.remove(r2)); // never added
    assertTrue(ncl.remove(r1)); // success
    assertTrue(ncl.getEntries().isEmpty());

    /*
     * tests where object.equals() == true
     */
    NCList<SimpleFeature> features = new NCList<>();
    SimpleFeature sf1 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "type");
    features.add(sf1);
    assertEquals(sf1, sf2); // sf1.equals(sf2)
    assertFalse(features.remove(sf2)); // equality is not enough for deletion
    assertTrue(features.getEntries().contains(sf1)); // still there!
    assertTrue(features.remove(sf1));
    assertTrue(features.getEntries().isEmpty()); // gone now

    /*
     * test with duplicate objects in NCList
     */
    features.add(sf1);
    features.add(sf1);
    assertEquals(features.getEntries().size(), 2);
    assertSame(features.getEntries().get(0), sf1);
    assertSame(features.getEntries().get(1), sf1);
    assertTrue(features.remove(sf1)); // first match only is deleted
    assertTrue(features.contains(sf1));
    assertEquals(features.size(), 1);
    assertTrue(features.remove(sf1));
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

    /*
     * use PrivilegedAccessor to force invalid values into ranges, as
     * the public API should not allow construction of an invalid NCList
     */
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
     * a different but equally valid NCList structure
     */
    ranges.clear();
    ncl = new NCList<Range>(ranges);
    ncl.add(new Range(40, 50));
    ncl.add(new Range(45, 55));
    ncl.add(new Range(40, 45));
    ncl.add(new Range(41, 46));
    ncl.add(new Range(42, 42));
    ncl.add(new Range(42, 42)); // duplicates allowed
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

  /**
   * If intervals have an unambiguous order of containment, the NCList is the
   * same regardless of the order in which they are added to it
   */
  @Test(groups = "Functional")
  public void testLoadOrder()
  {
    List<SimpleFeature> ranges = new ArrayList<>();
    ranges.add(new SimpleFeature(1, 205602, "gene")); // ENSG00000157764
    ranges.add(new SimpleFeature(187, 90957, "transcript")); // ENST00000469930
    ranges.add(new SimpleFeature(187, 90957, "exon")); // ENSE00001154485
    ranges.add(new SimpleFeature(226, 90335, "cds")); // CDS:ENSP00000495858
    ranges.add(new SimpleFeature(251, 260, "indel")); // gnomAD VCF
    ranges.add(new SimpleFeature(256, 256, "SNV")); // dbSNP151 rs1225976306

    /*
     * add all features at once; initial sort and load results in a nested NCList
     */
    NCList<SimpleFeature> ncl = new NCList<>(ranges);
    assertEquals(ncl.size(), 6);
    assertEquals(ncl.getDepth(), 6);
    String asString = "[1:205602:gene [187:90957:transcript [187:90957:exon [226:90335:cds [251:260:indel [256:256:SNV]]]]]]";
    assertEquals(ncl.toString(), asString);

    /*
     * add features one at a time, from 'large' to 'small' - same result
     */
    ncl = new NCList<>();
    int[] order = new int[] { 0, 1, 2, 3, 4, 5 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
    }
    assertEquals(ncl.toString(), asString);

    /*
     * add features one at a time, from 'small' to 'large' 
     * - same result except exon/transcript nesting reversed
     */
    ncl = new NCList<>();
    order = new int[] { 5, 4, 3, 2, 1, 0 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
    }
    String asString2 = "[1:205602:gene [187:90957:exon [187:90957:transcript [226:90335:cds [251:260:indel [256:256:SNV]]]]]]";
    assertEquals(ncl.toString(), asString2);

    /*
     * add features one at a time, in mixed order - same result as previous
     */
    ncl = new NCList<>();
    order = new int[] { 3, 0, 5, 2, 4, 1 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
    }
    assertEquals(ncl.toString(), asString2);
  }

  /**
   * If intervals have alternative containment orderings, the NCList structure
   * may depend on the order in which they are added to it
   */
  @Test(groups = "Functional")
  public void testLoadOrder_alternates()
  {
    SimpleFeature domain1 = new SimpleFeature(100, 300, "domain1");
    SimpleFeature domain2 = new SimpleFeature(200, 400, "domain2");
    SimpleFeature domain3 = new SimpleFeature(240, 280, "domain3");
    SimpleFeature metal = new SimpleFeature(250, 250, "Iron-sulfur");
  
    NCList<SimpleFeature> ncl1 = new NCList<>();
    ncl1.add(domain1);
    ncl1.add(domain2);
    ncl1.add(domain3); // will be nested under domain1
    ncl1.add(metal); // will be nested under domain3
    assertTrue(ncl1.isValid());
    assertEquals(ncl1.toString(),
            "[100:300:domain1 [240:280:domain3 [250:250:Iron-sulfur]], 200:400:domain2]");
    assertEquals(ncl1.getDepth(), 3);

    /*
     * alternative order of construction
     */
    NCList<SimpleFeature> ncl3 = new NCList<>();
    ncl3.add(domain2);
    ncl3.add(metal); // will be nested under domain2
    ncl3.add(domain1);
    ncl3.add(domain3); // will be nested under domain1
    assertTrue(ncl3.isValid());
    assertEquals(ncl3.toString(),
            "[100:300:domain1 [240:280:domain3], 200:400:domain2 [250:250:Iron-sulfur]]");
    assertEquals(ncl3.getDepth(), 2);
    assertNotEquals(ncl1.toString(), ncl3.toString());

    /*
     * identical (or duplicated) intervals may end up on different branches:
     */
    NCList<SimpleFeature> ncl4 = new NCList<>();
    ncl4.add(domain2); // [200-400]
    ncl4.add(metal); // [250-250] nests in [200-400]
    ncl4.add(domain1); // [100-300]
    ncl4.add(metal); // [250-250] nested in [100-300]
    assertEquals(ncl4.toString(),
            "[100:300:domain1 [250:250:Iron-sulfur], 200:400:domain2 [250:250:Iron-sulfur]]");
  }

  @Test(groups = "Functional")
  public void testGetDepth()
  {
    NCList<Range> ncl = new NCList<>();
    assertEquals(ncl.getDepth(), 0);

    /*
     * add 3 ranges with no containment: flat NCList
     */
    ncl.add(new Range(10, 20));
    ncl.add(new Range(15, 25));
    ncl.add(new Range(30, 40));
    assertEquals(ncl.getDepth(), 1);

    /*
     * add some nested features at one level down
     */
    ncl.add(new Range(12, 18)); // within [10-20]
    ncl.add(new Range(15, 21)); // within [15-25]
    ncl.add(new Range(36, 40)); // within [30-40]
    assertEquals(ncl.getDepth(), 2);

    /*
     * add deeper nested features
     */
    ncl.add(new Range(13, 17)); // within [12-18]
    assertEquals(ncl.getDepth(), 3);
    ncl.add(new Range(13, 16)); // within [13-17]
    assertEquals(ncl.getDepth(), 4);
    ncl.add(new Range(13, 16)); // within [13-16], duplicate
    assertEquals(ncl.getDepth(), 5);
  }

  /**
   * Demonstrates cases where load order affects tree balance / depth
   */
  @Test(groups = "Functional")
  public void testLoadOrder_treeBalance()
  {
    /*
     * make a set of pairs of highly overlapping ranges
     * - fromLeft: [1, N-1], [1, N-2], [1, N-3], ... 
     * - fromRight: [2, N], [2, N-1], [2, N-3], ...
     * ++++++++
     *  oooooooo
     *  ++++++
     *   oooooo
     *   ++++
     *    oooo
     *    ++
     *     oo
     */
    int N = 8;
    List<Range> fromLeft = new ArrayList<>();
    List<Range> fromRight = new ArrayList<>();
    int leftStart = 1;
    int leftEnd = N;
    while (leftStart <= leftEnd)
    {
      fromLeft.add(new Range(leftStart, leftEnd));
      fromRight.add(new Range(leftStart + 1, leftEnd + 1));
      leftStart++;
      leftEnd--;
    }
  
    /*
     * construct NCList with all intervals (sort and build)
     * results in a wide, nearly flat tree (width = 5, depth = 2)
     */
    List<Range> all = new ArrayList<>(fromLeft);
    all.addAll(fromRight);
    NCList<Range> ncl1 = new NCList<>(all);
    assertTrue(ncl1.isValid());
    assertEquals(ncl1.getDepth(), 2);
    String flat = "[1-8, 2-9 [2-7], 3-8 [3-6], 4-7 [4-5], 5-6]";
    assertEquals(ncl1.toString(), flat);

    /*
     * add intervals small to large in pairs, fromLeft then fromRight
     * - results in a deeply nested tree under [1-8]
     */
    NCList<Range> ncl2 = new NCList<>();
    int j = fromLeft.size();
    for (int i = 1; i <= j; i++)
    {
      ncl2.add(fromLeft.get(j - i));
      ncl2.add(fromRight.get(j - i));
    }
    assertTrue(ncl2.isValid());
    assertEquals(ncl2.getDepth(), 4);
    String deepLeft = "[1-8 [2-7 [3-6 [4-5, 5-6], 4-7], 3-8], 2-9]";
    assertEquals(ncl2.toString(), deepLeft);

    /*
     * add intervals large to small in pairs, fromLeft then fromRight
     * - also results in a deeply nested tree under [1-8]
     */
    NCList<Range> ncl3 = new NCList<>();
    for (int i = 0; i < j; i++)
    {
      ncl3.add(fromLeft.get(i));
      ncl3.add(fromRight.get(i));
    }
    assertTrue(ncl3.isValid());
    assertEquals(ncl3.getDepth(), 4);
    assertEquals(ncl3.toString(), deepLeft);

    /*
     * add intervals small to large in pairs, fromRight then fromLeft
     * - results in a deeply nested tree under [2-9]
     */
    NCList<Range> ncl4 = new NCList<>();
    for (int i = 1; i <= j; i++)
    {
      ncl4.add(fromRight.get(j - i));
      ncl4.add(fromLeft.get(j - i));
    }
    assertTrue(ncl4.isValid());
    assertEquals(ncl4.getDepth(), 4);
    String deepRight = "[1-8, 2-9 [2-7, 3-8 [3-6, 4-7 [4-5, 5-6]]]]";
    assertEquals(ncl4.toString(), deepRight);

    /*
     * add all fromRight intervals, then all fromLeft (large to small)
     */
    NCList<Range> ncl5 = new NCList<>();
    for (int i = 0; i < j; i++)
    {
      ncl5.add(fromRight.get(i));
    }
    for (int i = 0; i < j; i++)
    {
      ncl5.add(fromLeft.get(i));
    }
    assertTrue(ncl5.isValid());
    assertEquals(ncl5.getDepth(), 4);
    String balanced = "[1-8 [2-7 [3-6 [4-5]]], 2-9 [3-8 [4-7 [5-6]]]]";
    assertEquals(ncl5.toString(), balanced);

    /*
     * now repeat for large N to show the wide range of depths possible
     */
    N = 100;
    fromLeft.clear();
    fromRight.clear();
    leftStart = 1;
    leftEnd = N;
    while (leftStart <= leftEnd)
    {
      fromLeft.add(new Range(leftStart, leftEnd));
      fromRight.add(new Range(leftStart + 1, leftEnd + 1));
      leftStart++;
      leftEnd--;
    }

    /*
     * construct NCList with all intervals (sort and build)
     */
    all = new ArrayList<>(fromLeft);
    all.addAll(fromRight);
    NCList<Range> ncl6 = new NCList<>(all);
    assertEquals(ncl6.getDepth(), 2);

    /*
     * add intervals large to small, fromLeft then fromRight
     * results in a deeply nested tree
     */
    NCList<Range> ncl7 = new NCList<>();
    j = fromLeft.size();
    for (int i = 0; i < j; i++)
    {
      ncl7.add(fromLeft.get(i));
    }
    for (int i = 0; i < j; i++)
    {
      ncl7.add(fromRight.get(i));
    }
    assertTrue(ncl7.isValid());
    assertEquals(ncl7.getDepth(), 50);

    /*
     * add all fromRight intervals, then all fromLeft (large to small)
     */
    NCList<Range> ncl8 = new NCList<>();
    for (int i = 0; i < j; i++)
    {
      ncl8.add(fromRight.get(i));
    }
    for (int i = 0; i < j; i++)
    {
      ncl8.add(fromLeft.get(i));
    }
    assertTrue(ncl8.isValid());
    assertEquals(ncl8.getDepth(), 50);
  }

  @Test(groups = "Functional")
  public void testIterator()
  {
    NCList<Range> ncl = new NCList<>();
    assertFalse(ncl.iterator().hasNext());

    Range r1 = new Range(20, 40);
    Range r2 = new Range(20, 25);
    Range r3 = new Range(10, 30);
    Range r4 = new Range(30, 50);
    ncl.add(r1);
    ncl.add(r2);
    ncl.add(r3);
    ncl.add(r4);
    assertEquals(ncl.toString(), "[10-30, 20-40 [20-25], 30-50]");

    Iterator<Range> it = ncl.iterator();
    assertSame(it.next(), r3);
    assertSame(it.next(), r1);
    assertSame(it.next(), r2);
    assertSame(it.next(), r4);
    assertFalse(it.hasNext());
  }
}
