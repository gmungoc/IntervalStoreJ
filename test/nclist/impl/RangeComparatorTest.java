package nclist.impl;

import static org.testng.Assert.assertEquals;

import java.util.Comparator;

import org.testng.annotations.Test;

import nclist.api.IntervalI;
import nclist.impl.Range;
import nclist.impl.RangeComparator;

public class RangeComparatorTest
{

  @Test(groups = "Functional")
  public void testCompare()
  {
    RangeComparator comp = new RangeComparator(true);

    // same position, same length
    assertEquals(comp.compare(10, 10, 20, 20), 0);
    // same position, len1 > len2
    assertEquals(comp.compare(10, 10, 20, 19), -1);
    // same position, len1 < len2
    assertEquals(comp.compare(10, 10, 20, 21), 1);
    // pos1 > pos2
    assertEquals(comp.compare(11, 10, 20, 20), 1);
    // pos1 < pos2
    assertEquals(comp.compare(10, 11, 20, 10), -1);
  }

  @Test(groups = "Functional")
  public void testCompare_byStart()
  {
    Comparator<IntervalI> comp = RangeComparator.BY_START_POSITION;

    // same start position, same length
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 20)), 0);
    // same start position, len1 > len2
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 19)), -1);
    // same start position, len1 < len2
    assertEquals(comp.compare(new Range(10, 18), new Range(10, 20)), 1);
    // pos1 > pos2
    assertEquals(comp.compare(new Range(11, 20), new Range(10, 20)), 1);
    // pos1 < pos2
    assertEquals(comp.compare(new Range(10, 20), new Range(11, 20)), -1);
  }

  @Test(groups = "Functional")
  public void testCompare_byEnd()
  {
    Comparator<IntervalI> comp = RangeComparator.BY_END_POSITION;

    // same end position, same length
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 20)), 0);
    // same end position, len1 > len2
    assertEquals(comp.compare(new Range(10, 20), new Range(11, 20)), -1);
    // same end position, len1 < len2
    assertEquals(comp.compare(new Range(11, 20), new Range(10, 20)), 1);
    // end1 > end2
    assertEquals(comp.compare(new Range(10, 21), new Range(10, 20)), 1);
    // end1 < end2
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 21)), -1);
  }
}
