package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import junit.extensions.PA;

public class NCNodeTest
{
  @Test(groups = "Functional")
  public void testAddNode()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(10, 15));
    node.addNode(n2);

    List<Range> contents = new ArrayList<>();
    node.getEntries(contents);
    assertEquals(contents.size(), 2);
    assertTrue(contents.contains(r1));
    assertTrue(contents.contains(n2.getRegion()));
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAddNode_invalidRangeStart()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(9, 15));
    node.addNode(n2);
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAddNode_invalidRangeEnd()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(12, 21));
    node.addNode(n2);
  }

  @Test(groups = "Functional")
  public void testGetEntries()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    List<Range> entries = new ArrayList<>();

    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    // clearing the returned list does not affect the NCNode
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    NCNode<Range> n2 = new NCNode<>(new Range(15, 18));
    node.addNode(n2);
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 2);
    assertTrue(entries.contains(r1));
    assertTrue(entries.contains(n2.getRegion()));
  }

  /**
   * Tests for the contains method (uses entry.equals() test)
   */
  @Test(groups = "Functional")
  public void testContainsInterval()
  {
    SimpleFeature sf1 = new SimpleFeature(1, 10, "desc");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "desc");
    SimpleFeature sf3 = new SimpleFeature(1, 10, "description");
    NCNode<SimpleFeature> node = new NCNode<>(sf1);

    assertFalse(node.contains(null));
    assertTrue(node.contains(sf1));
    assertTrue(node.contains(sf2)); // sf1.equals(sf2)
    assertFalse(node.contains(sf3)); // !sf1.equals(sf3)
  }

  /**
   * Test method that checks for valid structure. Valid means that all
   * subregions (if any) lie within the root range, and that all subregions have
   * valid structure.
   */
  @Test(groups = "Functional")
  public void testIsValid()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(14, 15);
    Range r3 = new Range(16, 17);

    NCNode<Range> node = new NCNode<>(r1);
    assertTrue(node.isValid());
    node.addNode(new NCNode<>(r2));
    assertTrue(node.isValid());
    node.addNode(new NCNode<>(r3));
    assertTrue(node.isValid());

    /*
     * node has root range [10-20] and contains an
     * NCList of [14-15, 16-17]
     */
    assertTrue(node.isValid());
    PA.setValue(r1, "start", 15);
    assertFalse(node.isValid()); // r2 not within r1
    PA.setValue(r1, "start", 10);
    assertTrue(node.isValid());
    PA.setValue(r1, "end", 16);
    assertFalse(node.isValid()); // r3 not within r1
    PA.setValue(r1, "end", 20);
    assertTrue(node.isValid());
    PA.setValue(r3, "start", 12);
    assertFalse(node.isValid()); // r3 should precede r2
  }
}
