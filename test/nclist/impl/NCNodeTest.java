package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import features.api.SequenceFeatureI;
import features.impl.SequenceFeature;
import junit.extensions.PA;

public class NCNodeTest
{
  @Test(groups = "Functional")
  public void testAdd()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<Range>(r1);
    assertEquals(node.getBegin(), 10);
    Range r2 = new Range(10, 15);
    node.add(r2, false);

    List<Range> contents = new ArrayList<Range>();
    node.getEntries(contents);
    assertEquals(contents.size(), 2);
    assertTrue(contents.contains(r1));
    assertTrue(contents.contains(r2));
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAdd_invalidRangeStart()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<Range>(r1);
    assertEquals(node.getBegin(), 10);
    Range r2 = new Range(9, 15);
    node.add(r2, false);
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAdd_invalidRangeEnd()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<Range>(r1);
    assertEquals(node.getBegin(), 10);
    Range r2 = new Range(12, 21);
    node.add(r2, false);
  }

  @Test(groups = "Functional")
  public void testGetEntries()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<Range>(r1);
    List<Range> entries = new ArrayList<Range>();

    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    // clearing the returned list does not affect the NCNode
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    Range r2 = new Range(15, 18);
    node.add(r2, false);
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 2);
    assertTrue(entries.contains(r1));
    assertTrue(entries.contains(r2));
  }

  /**
   * Tests for the contains method (uses entry.equals() test)
   */
  @Test(groups = "Functional")
  public void testContains()
  {
    SequenceFeatureI sf1 = new SequenceFeature("type", "desc", 1, 10, 2f,
            "group");
    SequenceFeatureI sf2 = new SequenceFeature("type", "desc", 1, 10, 2f,
            "group");
    SequenceFeatureI sf3 = new SequenceFeature("type", "desc", 1, 10, 2f,
            "anothergroup");
    NCNode<SequenceFeatureI> node = new NCNode<>(sf1);

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
    NCNode<Range> node = new NCNode<Range>(r1);
    node.add(r2, false);
    node.add(r3, false);

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
