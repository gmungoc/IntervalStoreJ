package nclist.impl;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import nclist.api.IntervalI;

public class IntervalTest
{
  @Test(groups="Functional")
  public void testContainsInterval()
  {
    IntervalI i1 = new Range(10, 20);

    assertTrue(i1.containsInterval(i1));
    assertTrue(i1.containsInterval(new Range(10, 19)));
    assertTrue(i1.containsInterval(new Range(11, 20)));
    assertTrue(i1.containsInterval(new Range(15, 16)));

    assertFalse(i1.containsInterval(null));
    assertFalse(i1.containsInterval(new Range(9, 10)));
    assertFalse(i1.containsInterval(new Range(8, 9)));
    assertFalse(i1.containsInterval(new Range(20, 21)));
    assertFalse(i1.containsInterval(new Range(23, 24)));
    assertFalse(i1.containsInterval(new Range(1, 100)));
  }

  @Test(groups = "Functional")
  public void testProperlyContainsInterval()
  {
    IntervalI i1 = new Range(10, 20);

    assertTrue(i1.properlyContainsInterval(new Range(10, 19)));
    assertTrue(i1.properlyContainsInterval(new Range(11, 20)));
    assertTrue(i1.properlyContainsInterval(new Range(15, 16)));

    assertFalse(i1.properlyContainsInterval(null));
    assertFalse(i1.properlyContainsInterval(i1));
    assertFalse(i1.properlyContainsInterval(new Range(9, 10)));
    assertFalse(i1.properlyContainsInterval(new Range(8, 9)));
    assertFalse(i1.properlyContainsInterval(new Range(20, 21)));
    assertFalse(i1.properlyContainsInterval(new Range(23, 24)));
    assertFalse(i1.properlyContainsInterval(new Range(1, 100)));
  }

  @Test(groups = "Functional")
  public void testEqualsInterval()
  {
    IntervalI i1 = new Range(10, 20);
    assertTrue(i1.equalsInterval(i1));
    assertTrue(i1.equalsInterval(new Range(10, 20)));

    assertFalse(i1.equalsInterval(new Range(10, 21)));
    assertFalse(i1.equalsInterval(null));
  }

  @Test(groups = "Functional")
  public void testOverlapsInterval()
  {
    IntervalI i1 = new Range(10, 20);
    assertTrue(i1.overlapsInterval(i1));
    assertTrue(i1.overlapsInterval(new Range(5, 10)));
    assertTrue(i1.overlapsInterval(new Range(5, 15)));
    assertTrue(i1.overlapsInterval(new Range(12, 18)));
    assertTrue(i1.overlapsInterval(new Range(15, 30)));
    assertTrue(i1.overlapsInterval(new Range(20, 30)));
    assertTrue(i1.overlapsInterval(new Range(1, 100)));

    assertFalse(i1.overlapsInterval(null));
    assertFalse(i1.overlapsInterval(new Range(1, 9)));
    assertFalse(i1.overlapsInterval(new Range(21, 21)));
  }
}
