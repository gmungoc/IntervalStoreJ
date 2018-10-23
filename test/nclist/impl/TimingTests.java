package nclist.impl;

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TimingTests
{
  /*
   * use a fixed random seed for repeatable tests
   */
  static final int RANDOM_SEED = 732;

  /*
   * repeat count for each test, to check consistency
   */
  static final int REPEATS = 10;

  private Random rand;

  @BeforeClass
  public void setUp()
  {
    rand = new Random(RANDOM_SEED);
    System.out.println("Test\tsize\titeration\tms\tsize/ms");
  }

  /**
   * Data provider for the number of intervals to add in each test run
   * 
   * @return
   */
  @DataProvider(name = "intervalCount")
  public Object[][] getIntervalCount()
  {
    return new Object[][] { new Integer[] { 10000 },
        new Integer[]
        { 100000 } };
  }

  /**
   * Timing tests of loading an NCList, with all intervals loaded in the
   * constructor
   */
  @Test(groups = "Timing", dataProvider = "intervalCount")
  public void testLoadTiming_nclist_oneOff(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      NCList<Range> ncl = new NCList<>(ranges);
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "NCList oneOff", count, (i + 1), elapsed, ratio));
      assertTrue(ncl.isValid());
    }
  }

  /**
   * Generates a list of <code>count</code> intervals in the range [1, 4*count]
   * 
   * @param count
   * @return
   */
  protected List<Range> generateIntervals(Integer count)
  {
    int maxPos = 4 * count;
    List<Range> ranges = new ArrayList<>();
    for (int j = 0; j < count; j++)
    {
      int from = 1 + rand.nextInt(maxPos);
      int to = from + 50; // 1 + rand.nextInt(maxPos);
      ranges.add(new Range(Math.min(from, to), Math.max(from, to)));
    }
    return ranges;
  }

  /**
   * Timing tests of loading an NCList, with intervals loaded one at a time
   */
  @Test(groups = "Timing", dataProvider = "intervalCount")
  public void testLoadTiming_nclist_incremental(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      NCList<Range> ncl = new NCList<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      ncl.addAll(ranges);
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "NCList incr", count, (i + 1), elapsed, ratio));
      assertTrue(ncl.isValid());
    }

  }

  /**
   * Timing tests of loading a simple list, with all intervals loaded in the
   * constructor
   */
  @Test(groups = "Timing", dataProvider = "intervalCount")
  public void testLoadTiming_simpleList_load(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      List<Range> simple = new ArrayList<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      simple.addAll(ranges);
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "Simple load", count, (i + 1), elapsed, ratio));
    }
  }

  /**
   * Timing tests of loading a simple list, with intervals loaded one at a time
   */
  // disabled by default as rather slow running, set enabled = true to run
  @Test(groups = "Timing", dataProvider = "intervalCount", enabled = false)
  public void testLoadTiming_simplelist_loadNoDups(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      List<Range> simple = new ArrayList<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range r : ranges)
      {
        if (!simple.contains(r))
          simple.addAll(ranges);
      }
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "Simple noDups", count, (i + 1), elapsed, ratio));
    }
  }

  /**
   * Timing tests of loading an NCList, with intervals loaded one at a time
   */
  @Test(groups = "Timing", dataProvider = "intervalCount")
  public void testLoadTiming_nclist_incrementalNoDuplicates(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      NCList<Range> ncl = new NCList<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range r : ranges)
      {
        if (!ncl.contains(r))
        {
          ncl.add(r);
        }
      }
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "NCList incrNoDup", count, (i + 1), elapsed, ratio));
      assertTrue(ncl.isValid());
    }
  
  }

  /**
   * Timing tests of querying an NCList for overlaps
   */
  @Test(groups = "Timing", dataProvider = "intervalCount")
  public void testQueryTiming_nclist(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      List<Range> ranges = generateIntervals(count);
      NCList<Range> ncl = new NCList<>(ranges);

      List<Range> queries = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range q : queries)
      {
        ncl.findOverlaps(q.getBegin(), q.getEnd());
      }
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "NCList overlaps", count, (i + 1), elapsed, ratio));
      assertTrue(ncl.isValid());
    }
  }

  /**
   * Timing tests of querying an NCList for overlaps
   */
  // disabled by default as rather slow running, set enabled = true to run
  @Test(groups = "Timing", dataProvider = "intervalCount", enabled = false)
  public void testQueryTiming_simple(Integer count)
  {
    for (int i = 0; i < REPEATS; i++)
    {
      List<Range> ranges = generateIntervals(count);
  
      List<Range> queries = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range q : queries)
      {
        findOverlaps(ranges, q);
      }
      long elapsed = System.currentTimeMillis() - now;
      float ratio = elapsed == 0 ? 0 : count / (float) elapsed;
      System.out.println(String.format("%s\t%d\t%d\t%d\t%.1f",
              "Simple overlaps", count, (i + 1), elapsed, ratio));
    }
  }

  /**
   * 'Naive' exhaustive search of an list of intervals for overlaps
   * 
   * @param ranges
   * @param begin
   * @param end
   */
  private List<Range> findOverlaps(List<Range> ranges, Range query)
  {
    List<Range> result = new ArrayList<>();
    for (Range r : ranges)
    {
      if (r.overlapsInterval(query))
      {
        result.add(r);
      }
    }
    return result;
  }
}
