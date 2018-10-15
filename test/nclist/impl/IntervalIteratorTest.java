package nclist.impl;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;

import org.testng.annotations.Test;

public class IntervalIteratorTest
{
  @Test
  public void testNext()
  {
    IntervalStore<Range> store = new IntervalStore<>();

    Iterator<Range> it = store.iterator();
    assertFalse(it.hasNext());

    Range range1 = new Range(11, 20);
    store.add(range1);
    it = store.iterator();
    assertTrue(it.hasNext());
    assertSame(range1, it.next());
    assertFalse(it.hasNext());

    Range range2 = new Range(4, 8);
    store.add(range2);
    Range range3 = new Range(40, 60);
    store.add(range3);
    it = store.iterator();
    assertSame(range2, it.next());
    assertSame(range1, it.next());
    assertSame(range3, it.next());
    assertFalse(it.hasNext());

    /*
     * add nested intervals
     */
    store.clear();
    store.add(range1);
    Range range4 = new Range(15, 18);
    store.add(range4);
    it = store.iterator();
    assertTrue(it.hasNext());
    assertSame(range1, it.next());
    assertSame(range4, it.next());
    assertFalse(it.hasNext());
  }
}
