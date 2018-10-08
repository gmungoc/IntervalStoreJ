package features.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import features.api.SequenceFeatureI;

public class FeatureStoreTest
{

  @Test(groups = "Functional")
  public void testFindFeatures_nonNested()
  {
    FeatureStore fs = new FeatureStore();
    fs.addFeature(new SequenceFeature("", "", 10, 20, Float.NaN, null));
    // same range different description
    fs.addFeature(new SequenceFeature("", "desc", 10, 20, Float.NaN, null));
    fs.addFeature(new SequenceFeature("", "", 15, 25, Float.NaN, null));
    fs.addFeature(new SequenceFeature("", "", 20, 35, Float.NaN, null));

    List<SequenceFeatureI> overlaps = fs.findOverlappingFeatures(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = fs.findOverlappingFeatures(8, 10);
    assertEquals(overlaps.size(), 2);
    assertEquals(overlaps.get(0).getEnd(), 20);
    assertEquals(overlaps.get(1).getEnd(), 20);

    overlaps = fs.findOverlappingFeatures(12, 16);
    assertEquals(overlaps.size(), 3);
    assertEquals(overlaps.get(0).getEnd(), 20);
    assertEquals(overlaps.get(1).getEnd(), 20);
    assertEquals(overlaps.get(2).getEnd(), 25);

    overlaps = fs.findOverlappingFeatures(33, 33);
    assertEquals(overlaps.size(), 1);
    assertEquals(overlaps.get(0).getEnd(), 35);
  }

  @Test(groups = "Functional")
  public void testFindFeatures_nested()
  {
    FeatureStore fs = new FeatureStore();
    SequenceFeatureI sf1 = addFeature(fs, 10, 50);
    SequenceFeatureI sf2 = addFeature(fs, 10, 40);
    SequenceFeatureI sf3 = addFeature(fs, 20, 30);
    // fudge feature at same location but different group (so is added)
    SequenceFeature sf4 = new SequenceFeature("", "", 20, 30, Float.NaN,
            "different group");
    fs.addFeature(sf4);
    SequenceFeatureI sf5 = addFeature(fs, 35, 36);

    List<SequenceFeatureI> overlaps = fs.findOverlappingFeatures(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = fs.findOverlappingFeatures(10, 15);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = fs.findOverlappingFeatures(45, 60);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf1));

    overlaps = fs.findOverlappingFeatures(32, 38);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf5));

    overlaps = fs.findOverlappingFeatures(15, 25);
    assertEquals(overlaps.size(), 4);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
  }

  @Test(groups = "Functional")
  public void testFindFeatures_mixed()
  {
    FeatureStore fs = new FeatureStore();
    SequenceFeatureI sf1 = addFeature(fs, 10, 50);
    SequenceFeatureI sf2 = addFeature(fs, 1, 15);
    SequenceFeatureI sf3 = addFeature(fs, 20, 30);
    SequenceFeatureI sf4 = addFeature(fs, 40, 100);
    SequenceFeatureI sf5 = addFeature(fs, 60, 100);
    SequenceFeatureI sf6 = addFeature(fs, 70, 70);

    List<SequenceFeatureI> overlaps = fs.findOverlappingFeatures(200, 200);
    assertTrue(overlaps.isEmpty());

    overlaps = fs.findOverlappingFeatures(1, 9);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf2));

    overlaps = fs.findOverlappingFeatures(5, 18);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = fs.findOverlappingFeatures(30, 40);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));

    overlaps = fs.findOverlappingFeatures(80, 90);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));

    overlaps = fs.findOverlappingFeatures(68, 70);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));
  }

  /**
   * Helper method to add a feature of no particular type
   * 
   * @param fs
   * @param from
   * @param to
   * @return
   */
  SequenceFeature addFeature(FeatureStore fs, int from, int to)
  {
    SequenceFeature sf1 = new SequenceFeature("", "", from, to, Float.NaN,
            null);
    fs.addFeature(sf1);
    return sf1;
  }

  @Test(groups = "Functional")
  public void testFindFeatures_contactFeatures()
  {
    FeatureStore fs = new FeatureStore();

    SequenceFeature sf = new SequenceFeature("disulphide bond", "bond", 10,
            20, Float.NaN, null);
    fs.addFeature(sf);

    /*
     * neither contact point in range
     */
    List<SequenceFeatureI> overlaps = fs.findOverlappingFeatures(1, 9);
    assertTrue(overlaps.isEmpty());

    /*
     * neither contact point in range
     */
    overlaps = fs.findOverlappingFeatures(11, 19);
    assertTrue(overlaps.isEmpty());

    /*
     * first contact point in range
     */
    overlaps = fs.findOverlappingFeatures(5, 15);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf));

    /*
     * second contact point in range
     */
    overlaps = fs.findOverlappingFeatures(15, 25);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf));

    /*
     * both contact points in range
     */
    overlaps = fs.findOverlappingFeatures(5, 25);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf));
  }

  /**
   * Tests for the method that returns false for an attempt to add a feature
   * that would enclose, or be enclosed by, another feature
   */
  @Test(groups = "Functional")
  public void testAddNonNestedFeature()
  {
    FeatureStore fs = new FeatureStore();

    String type = "Domain";
    SequenceFeature sf1 = new SequenceFeature(type, type, 10, 20, Float.NaN,
            null);
    assertTrue(fs.addNonNestedFeature(sf1));

    // co-located feature is ok
    SequenceFeature sf2 = new SequenceFeature(type, type, 10, 20, Float.NaN,
            null);
    assertTrue(fs.addNonNestedFeature(sf2));

    // overlap left is ok
    SequenceFeature sf3 = new SequenceFeature(type, type, 5, 15, Float.NaN,
            null);
    assertTrue(fs.addNonNestedFeature(sf3));

    // overlap right is ok
    SequenceFeature sf4 = new SequenceFeature(type, type, 15, 25, Float.NaN,
            null);
    assertTrue(fs.addNonNestedFeature(sf4));

    // add enclosing feature is not ok
    SequenceFeature sf5 = new SequenceFeature(type, type, 10, 21, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf5));
    SequenceFeature sf6 = new SequenceFeature(type, type, 4, 15, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf6));
    SequenceFeature sf7 = new SequenceFeature(type, type, 1, 50, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf7));

    // add enclosed feature is not ok
    SequenceFeature sf8 = new SequenceFeature(type, type, 10, 19, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf8));
    SequenceFeature sf9 = new SequenceFeature(type, type, 16, 25, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf9));
    SequenceFeature sf10 = new SequenceFeature(type, type, 7, 7, Float.NaN,
            null);
    assertFalse(fs.addNonNestedFeature(sf10));
  }

  @Test(groups = "Functional")
  public void testGetPositionalFeatures()
  {
    FeatureStore store = new FeatureStore();
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.addFeature(sf1);
    // same range, different description
    SequenceFeature sf2 = new SequenceFeature("Metal", "desc2", 10, 20,
            Float.NaN, null);
    store.addFeature(sf2);
    // discontiguous range
    SequenceFeature sf3 = new SequenceFeature("Metal", "desc", 30, 40,
            Float.NaN, null);
    store.addFeature(sf3);
    // overlapping range
    SequenceFeature sf4 = new SequenceFeature("Metal", "desc", 15, 35,
            Float.NaN, null);
    store.addFeature(sf4);
    // enclosing range
    SequenceFeature sf5 = new SequenceFeature("Metal", "desc", 5, 50,
            Float.NaN, null);
    store.addFeature(sf5);
    // non-positional feature
    SequenceFeature sf6 = new SequenceFeature("Metal", "desc", 0, 0,
            Float.NaN, null);
    store.addFeature(sf6);
    // contact feature
    SequenceFeature sf7 = new SequenceFeature("Disulphide bond", "desc", 18,
            45, Float.NaN, null);
    store.addFeature(sf7);

    List<SequenceFeatureI> features = store.getPositionalFeatures();
    assertEquals(features.size(), 6);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf2));
    assertTrue(features.contains(sf3));
    assertTrue(features.contains(sf4));
    assertTrue(features.contains(sf5));
    assertFalse(features.contains(sf6));
    assertTrue(features.contains(sf7));

    features = store.getNonPositionalFeatures();
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf6));
  }

  @Test(groups = "Functional")
  public void testDelete()
  {
    FeatureStore store = new FeatureStore();
    SequenceFeatureI sf1 = addFeature(store, 10, 20);
    assertTrue(store.getPositionalFeatures().contains(sf1));

    /*
     * simple deletion
     */
    assertTrue(store.delete(sf1));
    assertTrue(store.getPositionalFeatures().isEmpty());

    /*
     * non-positional feature deletion
     */
    SequenceFeatureI sf2 = addFeature(store, 0, 0);
    assertFalse(store.getPositionalFeatures().contains(sf2));
    assertTrue(store.getNonPositionalFeatures().contains(sf2));
    assertTrue(store.delete(sf2));
    assertTrue(store.getNonPositionalFeatures().isEmpty());

    /*
     * contact feature deletion
     */
    SequenceFeature sf3 = new SequenceFeature("", "Disulphide Bond", 11, 23,
            Float.NaN, null);
    store.addFeature(sf3);
    assertEquals(store.getPositionalFeatures().size(), 1);
    assertTrue(store.getPositionalFeatures().contains(sf3));
    assertTrue(store.delete(sf3));
    assertTrue(store.getPositionalFeatures().isEmpty());

    /*
     * nested feature deletion
     */
    SequenceFeatureI sf4 = addFeature(store, 20, 30);
    SequenceFeatureI sf5 = addFeature(store, 22, 26); // to NCList
    SequenceFeatureI sf6 = addFeature(store, 23, 24); // child of sf5
    SequenceFeatureI sf7 = addFeature(store, 25, 25); // sibling of sf6
    SequenceFeatureI sf8 = addFeature(store, 24, 24); // child of sf6
    SequenceFeatureI sf9 = addFeature(store, 23, 23); // child of sf6
    assertEquals(store.getPositionalFeatures().size(), 6);

    // delete a node with children - they take its place
    assertTrue(store.delete(sf6)); // sf8, sf9 should become children of sf5
    assertEquals(store.getPositionalFeatures().size(), 5);
    assertFalse(store.getPositionalFeatures().contains(sf6));

    // delete a node with no children
    assertTrue(store.delete(sf7));
    assertEquals(store.getPositionalFeatures().size(), 4);
    assertFalse(store.getPositionalFeatures().contains(sf7));

    // delete root of NCList
    assertTrue(store.delete(sf5));
    assertEquals(store.getPositionalFeatures().size(), 3);
    assertFalse(store.getPositionalFeatures().contains(sf5));

    // continue the killing fields
    assertTrue(store.delete(sf4));
    assertEquals(store.getPositionalFeatures().size(), 2);
    assertFalse(store.getPositionalFeatures().contains(sf4));

    assertTrue(store.delete(sf9));
    assertEquals(store.getPositionalFeatures().size(), 1);
    assertFalse(store.getPositionalFeatures().contains(sf9));

    assertTrue(store.delete(sf8));
    assertTrue(store.getPositionalFeatures().isEmpty());
  }

  @Test(groups = "Functional")
  public void testAddFeature()
  {
    FeatureStore fs = new FeatureStore();

    SequenceFeature sf1 = new SequenceFeature("Cath", "", 10, 20, Float.NaN,
            null);
    SequenceFeature sf2 = new SequenceFeature("Cath", "", 10, 20, Float.NaN,
            null);

    assertTrue(fs.addFeature(sf1));
    assertEquals(fs.getFeatureCount(true), 1); // positional
    assertEquals(fs.getFeatureCount(false), 0); // non-positional

    /*
     * re-adding the same or an identical feature should fail
     */
    assertFalse(fs.addFeature(sf1));
    assertEquals(fs.getFeatureCount(true), 1);
    assertFalse(fs.addFeature(sf2));
    assertEquals(fs.getFeatureCount(true), 1);

    /*
     * add non-positional
     */
    SequenceFeature sf3 = new SequenceFeature("Cath", "", 0, 0, Float.NaN,
            null);
    assertTrue(fs.addFeature(sf3));
    assertEquals(fs.getFeatureCount(true), 1); // positional
    assertEquals(fs.getFeatureCount(false), 1); // non-positional
    SequenceFeature sf4 = new SequenceFeature("Cath", "", 0, 0, Float.NaN,
            null);
    assertFalse(fs.addFeature(sf4)); // already stored
    assertEquals(fs.getFeatureCount(true), 1); // positional
    assertEquals(fs.getFeatureCount(false), 1); // non-positional

    /*
     * add contact
     */
    SequenceFeature sf5 = new SequenceFeature("Disulfide bond", "", 10, 20,
            Float.NaN, null);
    assertTrue(fs.addFeature(sf5));
    assertEquals(fs.getFeatureCount(true), 2); // positional - add 1 for contact
    assertEquals(fs.getFeatureCount(false), 1); // non-positional
    SequenceFeature sf6 = new SequenceFeature("Disulfide bond", "", 10, 20,
            Float.NaN, null);
    assertFalse(fs.addFeature(sf6)); // already stored
    assertEquals(fs.getFeatureCount(true), 2); // no change
    assertEquals(fs.getFeatureCount(false), 1); // no change
  }

  @Test(groups = "Functional")
  public void testIsEmpty()
  {
    FeatureStore fs = new FeatureStore();
    assertTrue(fs.isEmpty());
    assertEquals(fs.getFeatureCount(true), 0);

    /*
     * non-nested feature
     */
    SequenceFeature sf1 = new SequenceFeature("Cath", "", 10, 20, Float.NaN,
            null);
    fs.addFeature(sf1);
    assertFalse(fs.isEmpty());
    assertEquals(fs.getFeatureCount(true), 1);
    fs.delete(sf1);
    assertTrue(fs.isEmpty());
    assertEquals(fs.getFeatureCount(true), 0);

    /*
     * non-positional feature
     */
    sf1 = new SequenceFeature("Cath", "", 0, 0, Float.NaN, null);
    fs.addFeature(sf1);
    assertFalse(fs.isEmpty());
    assertEquals(fs.getFeatureCount(false), 1); // non-positional
    assertEquals(fs.getFeatureCount(true), 0); // positional
    fs.delete(sf1);
    assertTrue(fs.isEmpty());
    assertEquals(fs.getFeatureCount(false), 0);

    /*
     * contact feature
     */
    sf1 = new SequenceFeature("Disulfide bond", "", 19, 49, Float.NaN,
            null);
    fs.addFeature(sf1);
    assertFalse(fs.isEmpty());
    assertEquals(fs.getFeatureCount(true), 1);
    fs.delete(sf1);
    assertTrue(fs.isEmpty());
    assertEquals(fs.getFeatureCount(true), 0);

    /*
     * sf2, sf3 added as nested features
     */
    sf1 = new SequenceFeature("Cath", "", 19, 49, Float.NaN, null);
    SequenceFeature sf2 = new SequenceFeature("Cath", "", 20, 40, Float.NaN,
            null);
    SequenceFeature sf3 = new SequenceFeature("Cath", "", 25, 35, Float.NaN,
            null);
    fs.addFeature(sf1);
    fs.addFeature(sf2);
    fs.addFeature(sf3);
    assertEquals(fs.getFeatureCount(true), 3);
    assertTrue(fs.delete(sf1));
    assertEquals(fs.getFeatureCount(true), 2);
    // FeatureStore should now only contain features in the NCList
    assertTrue(fs.nonNestedFeatures.isEmpty());
    assertEquals(fs.nestedFeatures.size(), 2);
    assertFalse(fs.isEmpty());
    assertTrue(fs.delete(sf2));
    assertEquals(fs.getFeatureCount(true), 1);
    assertFalse(fs.isEmpty());
    assertTrue(fs.delete(sf3));
    assertEquals(fs.getFeatureCount(true), 0);
    assertTrue(fs.isEmpty()); // all gone
  }

  @Test(groups = "Functional")
  public void testGetFeatureGroups()
  {
    FeatureStore fs = new FeatureStore();
    assertTrue(fs.getFeatureGroups(true).isEmpty());
    assertTrue(fs.getFeatureGroups(false).isEmpty());

    SequenceFeature sf1 = new SequenceFeature("Cath", "desc", 10, 20, 1f,
            "group1");
    fs.addFeature(sf1);
    Set<String> groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("group1"));

    /*
     * add another feature of the same group, delete one, delete both
     */
    SequenceFeature sf2 = new SequenceFeature("Cath", "desc", 20, 30, 1f,
            "group1");
    fs.addFeature(sf2);
    groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("group1"));
    fs.delete(sf2);
    groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("group1"));
    fs.delete(sf1);
    groups = fs.getFeatureGroups(true);
    assertTrue(fs.getFeatureGroups(true).isEmpty());

    SequenceFeature sf3 = new SequenceFeature("Cath", "desc", 20, 30, 1f,
            "group2");
    fs.addFeature(sf3);
    SequenceFeature sf4 = new SequenceFeature("Cath", "desc", 20, 30, 1f,
            "Group2");
    fs.addFeature(sf4);
    SequenceFeature sf5 = new SequenceFeature("Cath", "desc", 20, 30, 1f,
            null);
    fs.addFeature(sf5);
    groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 3);
    assertTrue(groups.contains("group2"));
    assertTrue(groups.contains("Group2")); // case sensitive
    assertTrue(groups.contains(null)); // null allowed
    assertTrue(fs.getFeatureGroups(false).isEmpty()); // non-positional

    fs.delete(sf3);
    groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 2);
    assertFalse(groups.contains("group2"));
    fs.delete(sf4);
    groups = fs.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertFalse(groups.contains("Group2"));
    fs.delete(sf5);
    groups = fs.getFeatureGroups(true);
    assertTrue(groups.isEmpty());

    /*
     * add non-positional feature
     */
    SequenceFeature sf6 = new SequenceFeature("Cath", "desc", 0, 0, 1f,
            "CathGroup");
    fs.addFeature(sf6);
    groups = fs.getFeatureGroups(false);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("CathGroup"));
    assertTrue(fs.delete(sf6));
    assertTrue(fs.getFeatureGroups(false).isEmpty());
  }

  @Test(groups = "Functional")
  public void testGetTotalFeatureLength()
  {
    FeatureStore fs = new FeatureStore();
    assertEquals(fs.getTotalFeatureLength(), 0);

    addFeature(fs, 10, 20); // 11
    assertEquals(fs.getTotalFeatureLength(), 11);
    addFeature(fs, 17, 37); // 21
    SequenceFeatureI sf1 = addFeature(fs, 14, 74); // 61
    assertEquals(fs.getTotalFeatureLength(), 93);

    // non-positional features don't count
    SequenceFeature sf2 = new SequenceFeature("Cath", "desc", 0, 0, 1f,
            "group1");
    fs.addFeature(sf2);
    assertEquals(fs.getTotalFeatureLength(), 93);

    // contact features count 1
    SequenceFeature sf3 = new SequenceFeature("disulphide bond", "desc", 15,
            35, 1f, "group1");
    fs.addFeature(sf3);
    assertEquals(fs.getTotalFeatureLength(), 94);

    assertTrue(fs.delete(sf1));
    assertEquals(fs.getTotalFeatureLength(), 33);
    assertFalse(fs.delete(sf1));
    assertEquals(fs.getTotalFeatureLength(), 33);
    assertTrue(fs.delete(sf2));
    assertEquals(fs.getTotalFeatureLength(), 33);
    assertTrue(fs.delete(sf3));
    assertEquals(fs.getTotalFeatureLength(), 32);
  }

  @Test(groups = "Functional")
  public void testGetFeatureLength()
  {
    /*
     * positional feature
     */
    SequenceFeature sf1 = new SequenceFeature("Cath", "desc", 10, 20, 1f,
            "group1");
    assertEquals(FeatureStore.getFeatureLength(sf1), 11);

    /*
     * non-positional feature
     */
    SequenceFeature sf2 = new SequenceFeature("Cath", "desc", 0, 0, 1f,
            "CathGroup");
    assertEquals(FeatureStore.getFeatureLength(sf2), 0);

    /*
     * contact feature counts 1
     */
    SequenceFeature sf3 = new SequenceFeature("Disulphide Bond", "desc", 14,
            28, 1f, "AGroup");
    assertEquals(FeatureStore.getFeatureLength(sf3), 1);
  }

  @Test(groups = "Functional")
  public void testMin()
  {
    assertEquals(FeatureStore.min(Float.NaN, Float.NaN), Float.NaN);
    assertEquals(FeatureStore.min(Float.NaN, 2f), 2f);
    assertEquals(FeatureStore.min(-2f, Float.NaN), -2f);
    assertEquals(FeatureStore.min(2f, -3f), -3f);
  }

  @Test(groups = "Functional")
  public void testMax()
  {
    assertEquals(FeatureStore.max(Float.NaN, Float.NaN), Float.NaN);
    assertEquals(FeatureStore.max(Float.NaN, 2f), 2f);
    assertEquals(FeatureStore.max(-2f, Float.NaN), -2f);
    assertEquals(FeatureStore.max(2f, -3f), 2f);
  }

  @Test(groups = "Functional")
  public void testGetMinimumScore_getMaximumScore()
  {
    FeatureStore fs = new FeatureStore();
    assertEquals(fs.getMinimumScore(true), Float.NaN); // positional
    assertEquals(fs.getMaximumScore(true), Float.NaN);
    assertEquals(fs.getMinimumScore(false), Float.NaN); // non-positional
    assertEquals(fs.getMaximumScore(false), Float.NaN);

    // add features with no score
    SequenceFeature sf1 = new SequenceFeature("type", "desc", 0, 0,
            Float.NaN, "group");
    fs.addFeature(sf1);
    SequenceFeature sf2 = new SequenceFeature("type", "desc", 10, 20,
            Float.NaN, "group");
    fs.addFeature(sf2);
    assertEquals(fs.getMinimumScore(true), Float.NaN);
    assertEquals(fs.getMaximumScore(true), Float.NaN);
    assertEquals(fs.getMinimumScore(false), Float.NaN);
    assertEquals(fs.getMaximumScore(false), Float.NaN);

    // add positional features with score
    SequenceFeature sf3 = new SequenceFeature("type", "desc", 10, 20, 1f,
            "group");
    fs.addFeature(sf3);
    SequenceFeature sf4 = new SequenceFeature("type", "desc", 12, 16, 4f,
            "group");
    fs.addFeature(sf4);
    assertEquals(fs.getMinimumScore(true), 1f);
    assertEquals(fs.getMaximumScore(true), 4f);
    assertEquals(fs.getMinimumScore(false), Float.NaN);
    assertEquals(fs.getMaximumScore(false), Float.NaN);

    // add non-positional features with score
    SequenceFeature sf5 = new SequenceFeature("type", "desc", 0, 0, 11f,
            "group");
    fs.addFeature(sf5);
    SequenceFeature sf6 = new SequenceFeature("type", "desc", 0, 0, -7f,
            "group");
    fs.addFeature(sf6);
    assertEquals(fs.getMinimumScore(true), 1f);
    assertEquals(fs.getMaximumScore(true), 4f);
    assertEquals(fs.getMinimumScore(false), -7f);
    assertEquals(fs.getMaximumScore(false), 11f);

    // delete one positional and one non-positional
    // min-max should be recomputed
    assertTrue(fs.delete(sf6));
    assertTrue(fs.delete(sf3));
    assertEquals(fs.getMinimumScore(true), 4f);
    assertEquals(fs.getMaximumScore(true), 4f);
    assertEquals(fs.getMinimumScore(false), 11f);
    assertEquals(fs.getMaximumScore(false), 11f);

    // delete remaining features with score
    assertTrue(fs.delete(sf4));
    assertTrue(fs.delete(sf5));
    assertEquals(fs.getMinimumScore(true), Float.NaN);
    assertEquals(fs.getMaximumScore(true), Float.NaN);
    assertEquals(fs.getMinimumScore(false), Float.NaN);
    assertEquals(fs.getMaximumScore(false), Float.NaN);

    // delete all features
    assertTrue(fs.delete(sf1));
    assertTrue(fs.delete(sf2));
    assertTrue(fs.isEmpty());
    assertEquals(fs.getMinimumScore(true), Float.NaN);
    assertEquals(fs.getMaximumScore(true), Float.NaN);
    assertEquals(fs.getMinimumScore(false), Float.NaN);
    assertEquals(fs.getMaximumScore(false), Float.NaN);
  }

  @Test(groups = "Functional")
  public void testListContains()
  {
    assertFalse(FeatureStore.listContains(null, null));
    List<SequenceFeatureI> features = new ArrayList<SequenceFeatureI>();
    assertFalse(FeatureStore.listContains(features, null));

    SequenceFeature sf1 = new SequenceFeature("type1", "desc1", 20, 30, 3f,
            "group1");
    assertFalse(FeatureStore.listContains(null, sf1));
    assertFalse(FeatureStore.listContains(features, sf1));

    features.add(sf1);
    SequenceFeature sf2 = new SequenceFeature("type1", "desc1", 20, 30, 3f,
            "group1");
    SequenceFeature sf3 = new SequenceFeature("type1", "desc1", 20, 40, 3f,
            "group1");

    // sf2.equals(sf1) so contains should return true
    assertTrue(FeatureStore.listContains(features, sf2));
    assertFalse(FeatureStore.listContains(features, sf3));
  }

  @Test(groups = "Functional")
  public void testGetFeaturesForGroup()
  {
    FeatureStore fs = new FeatureStore();

    /*
     * with no features
     */
    assertTrue(fs.getFeaturesForGroup(true, null).isEmpty());
    assertTrue(fs.getFeaturesForGroup(false, null).isEmpty());
    assertTrue(fs.getFeaturesForGroup(true, "uniprot").isEmpty());
    assertTrue(fs.getFeaturesForGroup(false, "uniprot").isEmpty());

    /*
     * sf1: positional feature in the null group
     */
    SequenceFeature sf1 = new SequenceFeature("Pfam", "desc", 4, 10, 0f,
            null);
    fs.addFeature(sf1);
    assertTrue(fs.getFeaturesForGroup(true, "uniprot").isEmpty());
    assertTrue(fs.getFeaturesForGroup(false, "uniprot").isEmpty());
    assertTrue(fs.getFeaturesForGroup(false, null).isEmpty());
    List<SequenceFeatureI> features = fs.getFeaturesForGroup(true, null);
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf1));

    /*
     * sf2: non-positional feature in the null group
     * sf3: positional feature in a non-null group
     * sf4: non-positional feature in a non-null group
     */
    SequenceFeature sf2 = new SequenceFeature("Pfam", "desc", 0, 0, 0f,
            null);
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 4, 10, 0f,
            "Uniprot");
    SequenceFeature sf4 = new SequenceFeature("Pfam", "desc", 0, 0, 0f,
            "Rfam");
    fs.addFeature(sf2);
    fs.addFeature(sf3);
    fs.addFeature(sf4);

    features = fs.getFeaturesForGroup(true, null);
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf1));

    features = fs.getFeaturesForGroup(false, null);
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf2));

    features = fs.getFeaturesForGroup(true, "Uniprot");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf3));

    features = fs.getFeaturesForGroup(false, "Rfam");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf4));
  }

  @Test(groups = "Functional")
  public void testDelete_readd()
  {
    /*
     * add a feature and a nested feature
     */
    FeatureStore store = new FeatureStore();
    SequenceFeatureI sf1 = addFeature(store, 10, 20);
    // sf2 is nested in sf1 so will be stored in nestedFeatures
    SequenceFeature sf2 = addFeature(store, 12, 14);
    List<SequenceFeatureI> features = store.getPositionalFeatures();
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf2));
    assertTrue(store.nonNestedFeatures.contains(sf1));
    assertTrue(store.nestedFeatures.contains(sf2));

    /*
     * delete the first feature
     */
    assertTrue(store.delete(sf1));
    features = store.getPositionalFeatures();
    assertFalse(features.contains(sf1));
    assertTrue(features.contains(sf2));

    /*
     * re-add the 'nested' feature; is it now duplicated?
     */
    store.addFeature(sf2);
    features = store.getPositionalFeatures();
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf2));
  }

  @Test(groups = "Functional")
  public void testContains()
  {
    FeatureStore fs = new FeatureStore();
    SequenceFeature sf1 = new SequenceFeature("Cath", "", 10, 20, Float.NaN,
            "group1");
    SequenceFeature sf2 = new SequenceFeature("Cath", "", 10, 20, Float.NaN,
            "group2");
    SequenceFeature sf3 = new SequenceFeature("Cath", "", 0, 0, Float.NaN,
            "group1");
    SequenceFeature sf4 = new SequenceFeature("Cath", "", 0, 0, 0f,
            "group1");
    SequenceFeature sf5 = new SequenceFeature("Disulphide Bond", "", 5, 15,
            Float.NaN, "group1");
    SequenceFeature sf6 = new SequenceFeature("Disulphide Bond", "", 5, 15,
            Float.NaN, "group2");

    fs.addFeature(sf1);
    fs.addFeature(sf3);
    fs.addFeature(sf5);
    assertTrue(fs.contains(sf1)); // positional feature
    assertTrue(fs.contains(new SequenceFeature(sf1))); // identical feature
    assertFalse(fs.contains(sf2)); // different group
    assertTrue(fs.contains(sf3)); // non-positional
    assertTrue(fs.contains(new SequenceFeature(sf3)));
    assertFalse(fs.contains(sf4)); // different score
    assertTrue(fs.contains(sf5)); // contact feature
    assertTrue(fs.contains(new SequenceFeature(sf5)));
    assertFalse(fs.contains(sf6)); // different group

    /*
     * add a nested feature
     */
    SequenceFeature sf7 = new SequenceFeature("Cath", "", 12, 16, Float.NaN,
            "group1");
    fs.addFeature(sf7);
    assertTrue(fs.contains(sf7));
    assertTrue(fs.contains(new SequenceFeature(sf7)));

    /*
     * delete the outer (enclosing, non-nested) feature
     */
    fs.delete(sf1);
    assertFalse(fs.contains(sf1));
    assertTrue(fs.contains(sf7));
  }
}
