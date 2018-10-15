package nclist.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import junit.extensions.PA;

public class IntervalStoreTest
{

  @Test(groups = "Functional")
  public void testFindFeatures_nonNested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    store.add(new SimpleFeature(10, 20, ""));
    // same range different description
    store.add(new SimpleFeature(10, 20, "desc"));
    store.add(new SimpleFeature(15, 25, ""));
    store.add(new SimpleFeature(20, 35, ""));

    List<SimpleFeature> overlaps = store.findOverlaps(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(8, 10);
    assertEquals(overlaps.size(), 2);
    assertEquals(overlaps.get(0).getEnd(), 20);
    assertEquals(overlaps.get(1).getEnd(), 20);

    overlaps = store.findOverlaps(12, 16);
    assertEquals(overlaps.size(), 3);
    assertEquals(overlaps.get(0).getEnd(), 20);
    assertEquals(overlaps.get(1).getEnd(), 20);
    assertEquals(overlaps.get(2).getEnd(), 25);

    overlaps = store.findOverlaps(33, 33);
    assertEquals(overlaps.size(), 1);
    assertEquals(overlaps.get(0).getEnd(), 35);
  }

  @Test(groups = "Functional")
  public void testFindFeatures_nested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 10, 40);
    SimpleFeature sf3 = add(store, 20, 30);
    // fudge feature at same location but different description (so is added)
    SimpleFeature sf4 = new SimpleFeature(20, 30, "different desc");
    store.add(sf4);
    SimpleFeature sf5 = add(store, 35, 36);

    List<SimpleFeature> overlaps = store.findOverlaps(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(10, 15);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(45, 60);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf1));

    overlaps = store.findOverlaps(32, 38);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf5));

    overlaps = store.findOverlaps(15, 25);
    assertEquals(overlaps.size(), 4);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
  }

  @Test(groups = "Functional")
  public void testFindFeatures_mixed()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 1, 15);
    SimpleFeature sf3 = add(store, 20, 30);
    SimpleFeature sf4 = add(store, 40, 100);
    SimpleFeature sf5 = add(store, 60, 100);
    SimpleFeature sf6 = add(store, 70, 70);

    List<SimpleFeature> overlaps = store.findOverlaps(200, 200);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(1, 9);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(5, 18);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(30, 40);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));

    overlaps = store.findOverlaps(80, 90);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));

    overlaps = store.findOverlaps(68, 70);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));
  }

  /**
   * Helper method to add a feature of no particular type
   * 
   * @param store
   * @param from
   * @param to
   * @return
   */
  SimpleFeature add(IntervalStore<SimpleFeature> store, int from,
          int to)
  {
    SimpleFeature sf1 = new SimpleFeature(from, to, "desc");
    store.add(sf1);
    return sf1;
  }

  /**
   * Tests for the method that returns false for an attempt to add an interval
   * that would enclose, or be enclosed by, another interval
   */
  @Test(groups = "Functional")
  public void testAddNonNestedInterval()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();

    String type = "Domain";
    SimpleFeature sf1 = new SimpleFeature(10, 20, type);
    assertTrue(store.addNonNestedInterval(sf1));

    // co-located feature is ok
    SimpleFeature sf2 = new SimpleFeature(10, 20, type);
    assertTrue(store.addNonNestedInterval(sf2));

    // overlap left is ok
    SimpleFeature sf3 = new SimpleFeature(5, 15, type);
    assertTrue(store.addNonNestedInterval(sf3));

    // overlap right is ok
    SimpleFeature sf4 = new SimpleFeature(15, 25, type);
    assertTrue(store.addNonNestedInterval(sf4));

    // add enclosing feature is not ok
    SimpleFeature sf5 = new SimpleFeature(10, 21, type);
    assertFalse(store.addNonNestedInterval(sf5));
    SimpleFeature sf6 = new SimpleFeature(4, 15, type);
    assertFalse(store.addNonNestedInterval(sf6));
    SimpleFeature sf7 = new SimpleFeature(1, 50, type);
    assertFalse(store.addNonNestedInterval(sf7));

    // add enclosed feature is not ok
    SimpleFeature sf8 = new SimpleFeature(10, 19, type);
    assertFalse(store.addNonNestedInterval(sf8));
    SimpleFeature sf9 = new SimpleFeature(16, 25, type);
    assertFalse(store.addNonNestedInterval(sf9));
    SimpleFeature sf10 = new SimpleFeature(7, 7, type);
    assertFalse(store.addNonNestedInterval(sf10));

    store.remove(sf4);
    SimpleFeature sf11 = new SimpleFeature(30, 40, type);
    assertTrue(store.addNonNestedInterval(sf11));
    SimpleFeature sf12 = new SimpleFeature(10, 19, type);
    assertFalse(store.addNonNestedInterval(sf12));
  }

  @Test(groups = "Functional")
  public void testRemove()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    assertTrue(store.contains(sf1));

    /*
     * simple deletion
     */
    assertTrue(store.remove(sf1));
    assertTrue(store.isEmpty());

    SimpleFeature sf2 = add(store, 0, 0);
    assertTrue(store.contains(sf2));
    assertTrue(store.remove(sf2));
    assertTrue(store.isEmpty());

    /*
     * nested feature deletion
     */
    SimpleFeature sf4 = add(store, 20, 30);
    SimpleFeature sf5 = add(store, 22, 26); // to NCList
    SimpleFeature sf6 = add(store, 23, 24); // child of sf5
    SimpleFeature sf7 = add(store, 25, 25); // sibling of sf6
    SimpleFeature sf8 = add(store, 24, 24); // child of sf6
    SimpleFeature sf9 = add(store, 23, 23); // child of sf6
    assertEquals(store.size(), 6);

    // delete a node with children - they take its place
    assertTrue(store.remove(sf6)); // sf8, sf9 should become children of sf5
    assertEquals(store.size(), 5);
    assertFalse(store.contains(sf6));

    // delete a node with no children
    assertTrue(store.remove(sf7));
    assertEquals(store.size(), 4);
    assertFalse(store.contains(sf7));

    // delete root of NCList
    assertTrue(store.remove(sf5));
    assertEquals(store.size(), 3);
    assertFalse(store.contains(sf5));

    // continue the killing fields
    assertTrue(store.remove(sf4));
    assertEquals(store.size(), 2);
    assertFalse(store.contains(sf4));

    assertTrue(store.remove(sf9));
    assertEquals(store.size(), 1);
    assertFalse(store.contains(sf9));

    assertTrue(store.remove(sf8));
    assertTrue(store.isEmpty());
  }

  @Test(groups = "Functional")
  public void testAdd()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();

    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    SimpleFeature sf2 = new SimpleFeature(10, 20, "Cath");

    assertTrue(store.add(sf1));
    assertEquals(store.size(), 1);

    /*
     * contains should return true for the same or an identical feature
     */
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(sf2));

    SimpleFeature sf3 = new SimpleFeature(0, 0, "Cath");
    assertTrue(store.add(sf3));
    assertEquals(store.size(), 2);
  }

  @Test(groups = "Functional")
  public void testIsEmpty()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    /*
     * non-nested feature
     */
    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    store.add(sf1);
    assertFalse(store.isEmpty());
    assertEquals(store.size(), 1);
    store.remove(sf1);
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    sf1 = new SimpleFeature(0, 0, "Cath");
    store.add(sf1);
    assertFalse(store.isEmpty());
    assertEquals(store.size(), 1);
    store.remove(sf1);
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    /*
     * sf2, sf3 added as nested features
     */
    sf1 = new SimpleFeature(19, 49, "Cath");
    SimpleFeature sf2 = new SimpleFeature(20, 40, "Cath");
    SimpleFeature sf3 = new SimpleFeature(25, 35, "Cath");
    store.add(sf1);
    store.add(sf2);
    store.add(sf3);
    assertEquals(store.size(), 3);
    assertTrue(store.remove(sf1));
    assertEquals(store.size(), 2);
    // IntervalStore should now only contain features in the NCList
    List<SimpleFeature> nonNested = (List<SimpleFeature>) PA.getValue(store,
            "nonNested");
    NCList<SimpleFeature> nested = (NCList<SimpleFeature>) PA.getValue(store,
            "nested");
    assertTrue(nonNested.isEmpty());
    assertEquals(nested.size(), 2);
    assertFalse(store.isEmpty());
    assertTrue(store.remove(sf2));
    assertEquals(store.size(), 1);
    assertFalse(store.isEmpty());
    assertTrue(store.remove(sf3));
    assertEquals(store.size(), 0);
    assertTrue(store.isEmpty()); // all gone
  }

  @Test(groups = "Functional")
  public void testListContains()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertFalse(store.listContains(null, null));
    List<SimpleFeature> features = new ArrayList<>();
    assertFalse(store.listContains(features, null));

    SimpleFeature sf1 = new SimpleFeature(20, 30, "desc1");
    assertFalse(store.listContains(null, sf1));
    assertFalse(store.listContains(features, sf1));

    features.add(sf1);
    SimpleFeature sf2 = new SimpleFeature(20, 30, "desc1");
    SimpleFeature sf3 = new SimpleFeature(20, 40, "desc1");

    // sf2.equals(sf1) so contains should return true
    assertTrue(store.listContains(features, sf2));
    assertFalse(store.listContains(features, sf3));
  }

  @Test(groups = "Functional")
  public void testRemove_readd()
  {
    /*
     * add a feature and a nested feature
     */
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    // sf2 is nested in sf1 so will be stored in nestedFeatures
    SimpleFeature sf2 = add(store, 12, 14);
    assertEquals(store.size(), 2);
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(sf2));

    @SuppressWarnings("unchecked")
    List<SimpleFeature> nonNested = (List<SimpleFeature>) PA.getValue(store,
            "nonNested");
    @SuppressWarnings("unchecked")
    NCList<SimpleFeature> nested = (NCList<SimpleFeature>) PA
            .getValue(store, "nested");
    assertTrue(nonNested.contains(sf1));
    assertTrue(nested.contains(sf2));
  
    /*
     * delete the first feature
     */
    assertTrue(store.remove(sf1));
    assertFalse(store.contains(sf1));
    assertTrue(store.contains(sf2));

    /*
     * re-add the 'nested' feature; it is now duplicated
     */
    store.add(sf2);
    assertEquals(store.size(), 2);
    assertTrue(store.contains(sf2));
  }

  @Test(groups = "Functional")
  public void testContains()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    SimpleFeature sf2 = new SimpleFeature(10, 20, "Pfam");

    store.add(sf1);
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(new SimpleFeature(sf1))); // identical feature
    assertFalse(store.contains(sf2)); // different description

    /*
     * add a nested feature
     */
    SimpleFeature sf3 = new SimpleFeature(12, 16, "Cath");
    store.add(sf3);
    assertTrue(store.contains(sf3));
    assertTrue(store.contains(new SimpleFeature(sf3)));

    /*
     * delete the outer (enclosing, non-nested) feature
     */
    store.remove(sf1);
    assertFalse(store.contains(sf1));
    assertTrue(store.contains(sf3));
  }

  @Test(groups = "Functional")
  public void testToString()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertEquals(store.toString(), "[]");
    add(store, 20, 30);
    assertEquals(store.toString(), "[20:30:desc]");
    add(store, 25, 35);
    assertEquals(store.toString(), "[20:30:desc, 25:35:desc]");
    add(store, 22, 28);
    add(store, 22, 28);
    add(store, 24, 26);
    assertEquals(store.toString(),
            "[20:30:desc, 25:35:desc]\n[22:28:desc [22:28:desc [24:26:desc]]]");
  }
}
