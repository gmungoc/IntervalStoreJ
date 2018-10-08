package features.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import features.api.SequenceFeatureI;
import features.api.SequenceFeaturesI;
import junit.extensions.PA;

public class SequenceFeaturesTest
{
  @Test(groups = "Functional")
  public void testConstructor()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    assertFalse(store.hasFeatures());

    store = new SequenceFeatures((List<SequenceFeatureI>) null);
    assertFalse(store.hasFeatures());

    List<SequenceFeatureI> features = new ArrayList<>();
    store = new SequenceFeatures(features);
    assertFalse(store.hasFeatures());

    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    features.add(sf1);
    SequenceFeature sf2 = new SequenceFeature("Metal", "desc", 15, 18,
            Float.NaN, null);
    features.add(sf2); // nested
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc2", 0, 0,
            Float.NaN, null); // non-positional
    features.add(sf3);
    store = new SequenceFeatures(features);
    assertTrue(store.hasFeatures());
    assertEquals(2, store.getFeatureCount(true)); // positional
    assertEquals(1, store.getFeatureCount(false)); // non-positional
    assertFalse(store.add(sf1)); // already contained
    assertFalse(store.add(sf2)); // already contained
    assertFalse(store.add(sf3)); // already contained
  }

  @Test(groups = "Functional")
  public void testGetPositionalFeatures()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    // same range, different description
    SequenceFeature sf2 = new SequenceFeature("Metal", "desc2", 10, 20,
            Float.NaN, null);
    store.add(sf2);
    // discontiguous range
    SequenceFeature sf3 = new SequenceFeature("Metal", "desc", 30, 40,
            Float.NaN, null);
    store.add(sf3);
    // overlapping range
    SequenceFeature sf4 = new SequenceFeature("Metal", "desc", 15, 35,
            Float.NaN, null);
    store.add(sf4);
    // enclosing range
    SequenceFeature sf5 = new SequenceFeature("Metal", "desc", 5, 50,
            Float.NaN, null);
    store.add(sf5);
    // non-positional feature
    SequenceFeature sf6 = new SequenceFeature("Metal", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf6);
    // contact feature
    SequenceFeature sf7 = new SequenceFeature("Disulphide bond", "desc", 18,
            45, Float.NaN, null);
    store.add(sf7);
    // different feature type
    SequenceFeature sf8 = new SequenceFeature("Pfam", "desc", 30, 40,
            Float.NaN, null);
    store.add(sf8);
    SequenceFeature sf9 = new SequenceFeature("Pfam", "desc", 15, 35,
            Float.NaN, null);
    store.add(sf9);

    /*
     * get all positional features
     */
    List<SequenceFeatureI> features = store.getPositionalFeatures();
    assertEquals(features.size(), 8);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf2));
    assertTrue(features.contains(sf3));
    assertTrue(features.contains(sf4));
    assertTrue(features.contains(sf5));
    assertFalse(features.contains(sf6)); // non-positional
    assertTrue(features.contains(sf7));
    assertTrue(features.contains(sf8));
    assertTrue(features.contains(sf9));

    /*
     * get features by type
     */
    assertTrue(store.getPositionalFeatures((String) null).isEmpty());
    assertTrue(store.getPositionalFeatures("Cath").isEmpty());
    assertTrue(store.getPositionalFeatures("METAL").isEmpty());

    features = store.getPositionalFeatures("Metal");
    assertEquals(features.size(), 5);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf2));
    assertTrue(features.contains(sf3));
    assertTrue(features.contains(sf4));
    assertTrue(features.contains(sf5));
    assertFalse(features.contains(sf6));

    features = store.getPositionalFeatures("Disulphide bond");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf7));

    features = store.getPositionalFeatures("Pfam");
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf8));
    assertTrue(features.contains(sf9));
  }

  @Test(groups = "Functional")
  public void testGetContactFeatures()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    // non-contact
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    // non-positional
    SequenceFeature sf2 = new SequenceFeature("Metal", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf2);
    // contact feature
    SequenceFeature sf3 = new SequenceFeature("Disulphide bond", "desc", 18,
            45, Float.NaN, null);
    store.add(sf3);
    // repeat for different feature type
    SequenceFeature sf4 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf4);
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf5);
    SequenceFeature sf6 = new SequenceFeature("Disulfide bond", "desc", 18,
            45, Float.NaN, null);
    store.add(sf6);

    /*
     * get all contact features
     */
    List<SequenceFeatureI> features = store.getContactFeatures();
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf3));
    assertTrue(features.contains(sf6));

    /*
     * get contact features by type
     */
    assertTrue(store.getContactFeatures((String) null).isEmpty());
    assertTrue(store.getContactFeatures("Cath").isEmpty());
    assertTrue(store.getContactFeatures("Pfam").isEmpty());
    assertTrue(store.getContactFeatures("DISULPHIDE BOND").isEmpty());

    features = store.getContactFeatures("Disulphide bond");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf3));

    features = store.getContactFeatures("Disulfide bond");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf6));
  }

  @Test(groups = "Functional")
  public void testGetNonPositionalFeatures()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    // positional
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    // non-positional
    SequenceFeature sf2 = new SequenceFeature("Metal", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf2);
    // contact feature
    SequenceFeature sf3 = new SequenceFeature("Disulphide bond", "desc", 18,
            45, Float.NaN, null);
    store.add(sf3);
    // repeat for different feature type
    SequenceFeature sf4 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf4);
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf5);
    SequenceFeature sf6 = new SequenceFeature("Disulfide bond", "desc", 18,
            45, Float.NaN, null);
    store.add(sf6);
    // one more non-positional, different description
    SequenceFeature sf7 = new SequenceFeature("Pfam", "desc2", 0, 0,
            Float.NaN, null);
    store.add(sf7);

    /*
     * get all non-positional features
     */
    List<SequenceFeatureI> features = store.getNonPositionalFeatures();
    assertEquals(features.size(), 3);
    assertTrue(features.contains(sf2));
    assertTrue(features.contains(sf5));
    assertTrue(features.contains(sf7));

    /*
     * get non-positional features by type
     */
    assertTrue(store.getNonPositionalFeatures((String) null).isEmpty());
    assertTrue(store.getNonPositionalFeatures("Cath").isEmpty());
    assertTrue(store.getNonPositionalFeatures("PFAM").isEmpty());

    features = store.getNonPositionalFeatures("Metal");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf2));

    features = store.getNonPositionalFeatures("Pfam");
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf5));
    assertTrue(features.contains(sf7));
  }

  /**
   * Helper method to add a feature of no particular type
   * 
   * @param sf
   * @param type
   * @param from
   * @param to
   * @return
   */
  SequenceFeatureI addFeature(SequenceFeaturesI sf, String type, int from,
          int to)
  {
    SequenceFeature sf1 = new SequenceFeature(type, "", from, to, Float.NaN,
            null);
    sf.add(sf1);
    return sf1;
  }

  @Test(groups = "Functional")
  public void testFindFeatures()
  {
    SequenceFeaturesI sf = new SequenceFeatures();
    SequenceFeatureI sf1 = addFeature(sf, "Pfam", 10, 50);
    SequenceFeatureI sf2 = addFeature(sf, "Pfam", 1, 15);
    SequenceFeatureI sf3 = addFeature(sf, "Pfam", 20, 30);
    SequenceFeatureI sf4 = addFeature(sf, "Pfam", 40, 100);
    SequenceFeatureI sf5 = addFeature(sf, "Pfam", 60, 100);
    SequenceFeatureI sf6 = addFeature(sf, "Pfam", 70, 70);
    SequenceFeatureI sf7 = addFeature(sf, "Cath", 10, 50);
    SequenceFeatureI sf8 = addFeature(sf, "Cath", 1, 15);
    SequenceFeatureI sf9 = addFeature(sf, "Cath", 20, 30);
    SequenceFeatureI sf10 = addFeature(sf, "Cath", 40, 100);
    SequenceFeatureI sf11 = addFeature(sf, "Cath", 60, 100);
    SequenceFeatureI sf12 = addFeature(sf, "Cath", 70, 70);

    List<SequenceFeatureI> overlaps = sf.findFeatures(200, 200, "Pfam");
    assertTrue(overlaps.isEmpty());

    overlaps = sf.findFeatures(1, 9, "Pfam");
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf2));

    overlaps = sf.findFeatures(5, 18, "Pfam");
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = sf.findFeatures(30, 40, "Pfam");
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));

    overlaps = sf.findFeatures(80, 90, "Pfam");
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));

    overlaps = sf.findFeatures(68, 70, "Pfam");
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));

    overlaps = sf.findFeatures(16, 69, "Cath");
    assertEquals(overlaps.size(), 4);
    assertTrue(overlaps.contains(sf7));
    assertFalse(overlaps.contains(sf8));
    assertTrue(overlaps.contains(sf9));
    assertTrue(overlaps.contains(sf10));
    assertTrue(overlaps.contains(sf11));
    assertFalse(overlaps.contains(sf12));

    assertTrue(sf.findFeatures(0, 1000, "Metal").isEmpty());

    overlaps = sf.findFeatures(7, 7, (String) null);
    assertTrue(overlaps.isEmpty());
  }

  @Test(groups = "Functional")
  public void testDelete()
  {
    SequenceFeaturesI sf = new SequenceFeatures();
    SequenceFeatureI sf1 = addFeature(sf, "Pfam", 10, 50);
    assertTrue(sf.getPositionalFeatures().contains(sf1));

    assertFalse(sf.delete(null));
    SequenceFeatureI sf2 = new SequenceFeature("Cath", "", 10, 15, 0f, null);
    assertFalse(sf.delete(sf2)); // not added, can't delete it
    assertTrue(sf.delete(sf1));
    assertTrue(sf.getPositionalFeatures().isEmpty());
  }

  @Test(groups = "Functional")
  public void testHasFeatures()
  {
    SequenceFeaturesI sf = new SequenceFeatures();
    assertFalse(sf.hasFeatures());

    SequenceFeatureI sf1 = addFeature(sf, "Pfam", 10, 50);
    assertTrue(sf.hasFeatures());

    sf.delete(sf1);
    assertFalse(sf.hasFeatures());
  }

  /**
   * Tests for the method that gets feature groups for positional or
   * non-positional features
   */
  @Test(groups = "Functional")
  public void testGetFeatureGroups()
  {
    SequenceFeaturesI sf = new SequenceFeatures();
    assertTrue(sf.getFeatureGroups(true).isEmpty());
    assertTrue(sf.getFeatureGroups(false).isEmpty());

    /*
     * add a non-positional feature (begin/end = 0/0)
     */
    SequenceFeature sfx = new SequenceFeature("AType", "Desc", 0, 0, 0f,
            "AGroup");
    sf.add(sfx);
    Set<String> groups = sf.getFeatureGroups(true); // for positional
    assertTrue(groups.isEmpty());
    groups = sf.getFeatureGroups(false); // for non-positional
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("AGroup"));
    groups = sf.getFeatureGroups(false, "AType");
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("AGroup"));
    groups = sf.getFeatureGroups(true, "AnotherType");
    assertTrue(groups.isEmpty());

    /*
     * add, then delete, more non-positional features of different types
     */
    SequenceFeature sfy = new SequenceFeature("AnotherType", "Desc", 0, 0,
            0f, "AnotherGroup");
    sf.add(sfy);
    SequenceFeature sfz = new SequenceFeature("AThirdType", "Desc", 0, 0,
            0f, null);
    sf.add(sfz);
    groups = sf.getFeatureGroups(false);
    assertEquals(groups.size(), 3);
    assertTrue(groups.contains("AGroup"));
    assertTrue(groups.contains("AnotherGroup"));
    assertTrue(groups.contains(null)); // null is a possible group
    sf.delete(sfz);
    sf.delete(sfy);
    groups = sf.getFeatureGroups(false);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("AGroup"));

    /*
     * add positional features
     */
    SequenceFeature sf1 = new SequenceFeature("Pfam", "Desc", 10, 50, 0f,
            "PfamGroup");
    sf.add(sf1);
    groups = sf.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("PfamGroup"));
    groups = sf.getFeatureGroups(false); // non-positional unchanged
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("AGroup"));

    SequenceFeature sf2 = new SequenceFeature("Cath", "Desc", 10, 50, 0f,
            null);
    sf.add(sf2);
    groups = sf.getFeatureGroups(true);
    assertEquals(groups.size(), 2);
    assertTrue(groups.contains("PfamGroup"));
    assertTrue(groups.contains(null));

    sf.delete(sf1);
    sf.delete(sf2);
    assertTrue(sf.getFeatureGroups(true).isEmpty());

    SequenceFeature sf3 = new SequenceFeature("CDS", "", 10, 50, 0f,
            "Ensembl");
    sf.add(sf3);
    SequenceFeature sf4 = new SequenceFeature("exon", "", 10, 50, 0f,
            "Ensembl");
    sf.add(sf4);
    groups = sf.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("Ensembl"));

    /*
     * delete last Ensembl group feature from CDS features
     * but still have one in exon features
     */
    sf.delete(sf3);
    groups = sf.getFeatureGroups(true);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("Ensembl"));

    /*
     * delete the last non-positional feature
     */
    sf.delete(sfx);
    groups = sf.getFeatureGroups(false);
    assertTrue(groups.isEmpty());
  }

  @Test(groups = "Functional")
  public void testGetFeatureTypesForGroups()
  {
    SequenceFeaturesI sf = new SequenceFeatures();
    assertTrue(sf.getFeatureTypesForGroups(true, (String) null).isEmpty());

    /*
     * add feature with group = "Uniprot", type = "helix"
     */
    String groupUniprot = "Uniprot";
    SequenceFeature sf1 = new SequenceFeature("helix", "Desc", 10, 50, 0f,
            groupUniprot);
    sf.add(sf1);
    Set<String> groups = sf.getFeatureTypesForGroups(true, groupUniprot);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("helix"));
    assertTrue(sf.getFeatureTypesForGroups(true, (String) null).isEmpty());

    /*
     * add feature with group = "Uniprot", type = "strand"
     */
    SequenceFeature sf2 = new SequenceFeature("strand", "Desc", 10, 50, 0f,
            groupUniprot);
    sf.add(sf2);
    groups = sf.getFeatureTypesForGroups(true, groupUniprot);
    assertEquals(groups.size(), 2);
    assertTrue(groups.contains("helix"));
    assertTrue(groups.contains("strand"));

    /*
     * delete the "strand" Uniprot feature - still have "helix"
     */
    sf.delete(sf2);
    groups = sf.getFeatureTypesForGroups(true, groupUniprot);
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("helix"));

    /*
     * delete the "helix" Uniprot feature - none left
     */
    sf.delete(sf1);
    assertTrue(sf.getFeatureTypesForGroups(true, groupUniprot).isEmpty());

    /*
     * add some null group features
     */
    SequenceFeature sf3 = new SequenceFeature("strand", "Desc", 10, 50, 0f,
            null);
    sf.add(sf3);
    SequenceFeature sf4 = new SequenceFeature("turn", "Desc", 10, 50, 0f,
            null);
    sf.add(sf4);
    groups = sf.getFeatureTypesForGroups(true, (String) null);
    assertEquals(groups.size(), 2);
    assertTrue(groups.contains("strand"));
    assertTrue(groups.contains("turn"));

    /*
     * add strand/Cath  and turn/Scop and query for one or both groups
     * (find feature types for groups selected in Feature Settings)
     */
    SequenceFeature sf5 = new SequenceFeature("strand", "Desc", 10, 50, 0f,
            "Cath");
    sf.add(sf5);
    SequenceFeature sf6 = new SequenceFeature("turn", "Desc", 10, 50, 0f,
            "Scop");
    sf.add(sf6);
    groups = sf.getFeatureTypesForGroups(true, "Cath");
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("strand"));
    groups = sf.getFeatureTypesForGroups(true, "Scop");
    assertEquals(groups.size(), 1);
    assertTrue(groups.contains("turn"));
    groups = sf.getFeatureTypesForGroups(true, "Cath", "Scop");
    assertEquals(groups.size(), 2);
    assertTrue(groups.contains("turn"));
    assertTrue(groups.contains("strand"));
    // alternative vararg syntax
    groups = sf.getFeatureTypesForGroups(true,
            new String[]
            { "Cath", "Scop" });
    assertEquals(groups.size(), 2);
    assertTrue(groups.contains("turn"));
    assertTrue(groups.contains("strand"));
  }

  @Test(groups = "Functional")
  public void testGetFeatureTypes()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    Set<String> types = store.getFeatureTypes();
    assertTrue(types.isEmpty());

    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    types = store.getFeatureTypes();
    assertEquals(types.size(), 1);
    assertTrue(types.contains("Metal"));

    // null type is rejected...
    SequenceFeature sf2 = new SequenceFeature(null, "desc", 10, 20,
            Float.NaN, null);
    assertFalse(store.add(sf2));
    types = store.getFeatureTypes();
    assertEquals(types.size(), 1);
    assertFalse(types.contains(null));
    assertTrue(types.contains("Metal"));

    /*
     * add non-positional feature
     */
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf3);
    types = store.getFeatureTypes();
    assertEquals(types.size(), 2);
    assertTrue(types.contains("Pfam"));

    /*
     * add contact feature
     */
    SequenceFeature sf4 = new SequenceFeature("Disulphide Bond", "desc", 10,
            20, Float.NaN, null);
    store.add(sf4);
    types = store.getFeatureTypes();
    assertEquals(types.size(), 3);
    assertTrue(types.contains("Disulphide Bond"));

    /*
     * add another Pfam
     */
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf5);
    types = store.getFeatureTypes();
    assertEquals(types.size(), 3); // unchanged

    /*
     * delete first Pfam - still have one
     */
    assertTrue(store.delete(sf3));
    types = store.getFeatureTypes();
    assertEquals(types.size(), 3);
    assertTrue(types.contains("Pfam"));

    /*
     * delete second Pfam - no longer have one
     */
    assertTrue(store.delete(sf5));
    types = store.getFeatureTypes();
    assertEquals(types.size(), 2);
    assertFalse(types.contains("Pfam"));
  }

  @Test(groups = "Functional")
  public void testGetFeatureCount()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    assertEquals(store.getFeatureCount(true), 0);
    assertEquals(store.getFeatureCount(false), 0);

    /*
     * add positional
     */
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    assertEquals(store.getFeatureCount(true), 1);
    assertEquals(store.getFeatureCount(false), 0);

    /*
     * null feature type is rejected
     */
    SequenceFeature sf2 = new SequenceFeature(null, "desc", 10, 20,
            Float.NaN, null);
    assertFalse(store.add(sf2));
    assertEquals(store.getFeatureCount(true), 1);
    assertEquals(store.getFeatureCount(false), 0);

    /*
     * add non-positional feature
     */
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf3);
    assertEquals(store.getFeatureCount(true), 1);
    assertEquals(store.getFeatureCount(false), 1);

    /*
     * add contact feature (counts as 1)
     */
    SequenceFeature sf4 = new SequenceFeature("Disulphide Bond", "desc", 10,
            20, Float.NaN, null);
    store.add(sf4);
    assertEquals(store.getFeatureCount(true), 2);
    assertEquals(store.getFeatureCount(false), 1);

    /*
     * add another Pfam but this time as a positional feature
     */
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf5);
    assertEquals(store.getFeatureCount(true), 3); // sf1, sf4, sf5
    assertEquals(store.getFeatureCount(false), 1); // sf3
    assertEquals(store.getFeatureCount(true, "Pfam"), 1); // positional
    assertEquals(store.getFeatureCount(false, "Pfam"), 1); // non-positional
    // search for type==null
    assertEquals(store.getFeatureCount(true, (String) null), 0);
    // search with no type specified
    assertEquals(store.getFeatureCount(true, (String[]) null), 3);
    assertEquals(store.getFeatureCount(true, "Metal", "Cath"), 1);
    assertEquals(store.getFeatureCount(true, "Disulphide Bond"), 1);
    assertEquals(store.getFeatureCount(true, "Metal", "Pfam", null), 2);

    /*
     * delete first Pfam (non-positional)
     */
    assertTrue(store.delete(sf3));
    assertEquals(store.getFeatureCount(true), 3);
    assertEquals(store.getFeatureCount(false), 0);

    /*
     * delete second Pfam (positional)
     */
    assertTrue(store.delete(sf5));
    assertEquals(store.getFeatureCount(true), 2);
    assertEquals(store.getFeatureCount(false), 0);
  }

  @Test(groups = "Functional")
  public void testGetAllFeatures()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    List<SequenceFeatureI> features = store.getAllFeatures();
    assertTrue(features.isEmpty());

    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    features = store.getAllFeatures();
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf1));

    SequenceFeature sf2 = new SequenceFeature("Metallic", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf2);
    features = store.getAllFeatures();
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf2));

    /*
     * add non-positional feature
     */
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf3);
    features = store.getAllFeatures();
    assertEquals(features.size(), 3);
    assertTrue(features.contains(sf3));

    /*
     * add contact feature
     */
    SequenceFeature sf4 = new SequenceFeature("Disulphide Bond", "desc", 10,
            20, Float.NaN, null);
    store.add(sf4);
    features = store.getAllFeatures();
    assertEquals(features.size(), 4);
    assertTrue(features.contains(sf4));

    /*
     * add another Pfam
     */
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf5);
    features = store.getAllFeatures();
    assertEquals(features.size(), 5);
    assertTrue(features.contains(sf5));

    /*
     * select by type does not apply to non-positional features
     */
    features = store.getAllFeatures("Cath");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf3));

    features = store.getAllFeatures("Pfam", "Cath", "Metal");
    assertEquals(features.size(), 3);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf3));
    assertTrue(features.contains(sf5));

    /*
     * delete first Pfam
     */
    assertTrue(store.delete(sf3));
    features = store.getAllFeatures();
    assertEquals(features.size(), 4);
    assertFalse(features.contains(sf3));

    /*
     * delete second Pfam
     */
    assertTrue(store.delete(sf5));
    features = store.getAllFeatures();
    assertEquals(features.size(), 3);
    assertFalse(features.contains(sf3));
  }

  @Test(groups = "Functional")
  public void testGetTotalFeatureLength()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    assertEquals(store.getTotalFeatureLength(), 0);

    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 10, 20,
            Float.NaN, null);
    assertTrue(store.add(sf1));
    assertEquals(store.getTotalFeatureLength(), 11);
    assertEquals(store.getTotalFeatureLength("Metal"), 11);
    assertEquals(store.getTotalFeatureLength("Plastic"), 0);

    // re-add does nothing!
    assertFalse(store.add(sf1));
    assertEquals(store.getTotalFeatureLength(), 11);

    /*
     * add non-positional feature
     */
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf3);
    assertEquals(store.getTotalFeatureLength(), 11);

    /*
     * add contact feature - counts 1 to feature length
     */
    SequenceFeature sf4 = new SequenceFeature("Disulphide Bond", "desc", 10,
            20, Float.NaN, null);
    store.add(sf4);
    assertEquals(store.getTotalFeatureLength(), 12);

    /*
     * add another Pfam
     */
    SequenceFeature sf5 = new SequenceFeature("Pfam", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf5);
    assertEquals(store.getTotalFeatureLength(), 23);

    /*
     * delete features
     */
    assertTrue(store.delete(sf3)); // non-positional
    assertEquals(store.getTotalFeatureLength(), 23); // no change

    assertTrue(store.delete(sf5));
    assertEquals(store.getTotalFeatureLength(), 12);

    assertTrue(store.delete(sf4)); // contact
    assertEquals(store.getTotalFeatureLength(), 11);

    assertTrue(store.delete(sf1));
    assertEquals(store.getTotalFeatureLength(), 0);
  }

  @Test(groups = "Functional")
  public void testGetMinimumScore_getMaximumScore()
  {
    SequenceFeatures sf = new SequenceFeatures();
    SequenceFeature sf1 = new SequenceFeature("Metal", "desc", 0, 0,
            Float.NaN, "group"); // non-positional, no score
    sf.add(sf1);
    SequenceFeature sf2 = new SequenceFeature("Cath", "desc", 10, 20,
            Float.NaN, "group"); // positional, no score
    sf.add(sf2);
    SequenceFeature sf3 = new SequenceFeature("Metal", "desc", 10, 20, 1f,
            "group");
    sf.add(sf3);
    SequenceFeature sf4 = new SequenceFeature("Metal", "desc", 12, 16, 4f,
            "group");
    sf.add(sf4);
    SequenceFeature sf5 = new SequenceFeature("Cath", "desc", 0, 0, 11f,
            "group");
    sf.add(sf5);
    SequenceFeature sf6 = new SequenceFeature("Cath", "desc", 0, 0, -7f,
            "group");
    sf.add(sf6);

    assertEquals(sf.getMinimumScore("nosuchtype", true), Float.NaN);
    assertEquals(sf.getMinimumScore("nosuchtype", false), Float.NaN);
    assertEquals(sf.getMaximumScore("nosuchtype", true), Float.NaN);
    assertEquals(sf.getMaximumScore("nosuchtype", false), Float.NaN);

    // positional features min-max:
    assertEquals(sf.getMinimumScore("Metal", true), 1f);
    assertEquals(sf.getMaximumScore("Metal", true), 4f);
    assertEquals(sf.getMinimumScore("Cath", true), Float.NaN);
    assertEquals(sf.getMaximumScore("Cath", true), Float.NaN);

    // non-positional features min-max:
    assertEquals(sf.getMinimumScore("Cath", false), -7f);
    assertEquals(sf.getMaximumScore("Cath", false), 11f);
    assertEquals(sf.getMinimumScore("Metal", false), Float.NaN);
    assertEquals(sf.getMaximumScore("Metal", false), Float.NaN);

    // delete features; min-max should get recomputed
    sf.delete(sf6);
    assertEquals(sf.getMinimumScore("Cath", false), 11f);
    assertEquals(sf.getMaximumScore("Cath", false), 11f);
    sf.delete(sf4);
    assertEquals(sf.getMinimumScore("Metal", true), 1f);
    assertEquals(sf.getMaximumScore("Metal", true), 1f);
    sf.delete(sf5);
    assertEquals(sf.getMinimumScore("Cath", false), Float.NaN);
    assertEquals(sf.getMaximumScore("Cath", false), Float.NaN);
    sf.delete(sf3);
    assertEquals(sf.getMinimumScore("Metal", true), Float.NaN);
    assertEquals(sf.getMaximumScore("Metal", true), Float.NaN);
    sf.delete(sf1);
    sf.delete(sf2);
    assertFalse(sf.hasFeatures());
    assertEquals(sf.getMinimumScore("Cath", false), Float.NaN);
    assertEquals(sf.getMaximumScore("Cath", false), Float.NaN);
    assertEquals(sf.getMinimumScore("Metal", true), Float.NaN);
    assertEquals(sf.getMaximumScore("Metal", true), Float.NaN);
  }

  @Test(groups = "Functional")
  public void testVarargsToTypes()
  {
    SequenceFeatures sf = new SequenceFeatures();
    sf.add(new SequenceFeature("Metal", "desc", 0, 0, Float.NaN, "group"));
    sf.add(new SequenceFeature("Cath", "desc", 10, 20, Float.NaN, "group"));

    /*
     * no type specified - get all types stored
     * they are returned in keyset (alphabetical) order
     */
    Map<String, FeatureStore> featureStores = (Map<String, FeatureStore>) PA
            .getValue(sf, "featureStore");

    Iterable<FeatureStore> types = sf.varargToTypes();
    Iterator<FeatureStore> iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Cath"));
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());

    /*
     * empty array is the same as no vararg parameter supplied
     * so treated as all stored types
     */
    types = sf.varargToTypes(new String[] {});
    iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Cath"));
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());

    /*
     * null type specified; this is passed as vararg
     * String[1] {null}
     */
    types = sf.varargToTypes((String) null);
    assertFalse(types.iterator().hasNext());

    /*
     * null types array specified; this is passed as vararg null
     */
    types = sf.varargToTypes((String[]) null);
    iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Cath"));
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());

    /*
     * one type specified
     */
    types = sf.varargToTypes("Metal");
    iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());

    /*
     * two types specified - get sorted alphabetically
     */
    types = sf.varargToTypes("Metal", "Cath");
    iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Cath"));
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());

    /*
     * null type included - should be ignored
     */
    types = sf.varargToTypes("Metal", null, "Helix");
    iterator = types.iterator();
    assertTrue(iterator.hasNext());
    assertSame(iterator.next(), featureStores.get("Metal"));
    assertFalse(iterator.hasNext());
  }

  @Test(groups = "Functional")
  public void testGetFeatureTypes_byOntology()
  {
    SequenceFeaturesI store = new SequenceFeatures();

    SequenceFeature sf1 = new SequenceFeature("transcript", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf1);
    // mRNA isA mature_transcript isA transcript
    SequenceFeature sf2 = new SequenceFeature("mRNA", "desc", 10, 20,
            Float.NaN, null);
    store.add(sf2);
    // just to prove non-positional feature types are included
    SequenceFeature sf3 = new SequenceFeature("mRNA", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf3);
    SequenceFeature sf4 = new SequenceFeature("CDS", "desc", 0, 0,
            Float.NaN, null);
    store.add(sf4);

    Set<String> types = store.getFeatureTypes("transcript");
    assertEquals(types.size(), 2);
    assertTrue(types.contains("transcript"));
    assertTrue(types.contains("mRNA"));

    // matches include arguments whether SO terms or not
    types = store.getFeatureTypes("transcript", "CDS");
    assertEquals(types.size(), 3);
    assertTrue(types.contains("transcript"));
    assertTrue(types.contains("mRNA"));
    assertTrue(types.contains("CDS"));

    types = store.getFeatureTypes("exon");
    assertTrue(types.isEmpty());
  }

  @Test(groups = "Functional")
  public void testGetFeaturesByOntology()
  {
    SequenceFeaturesI store = new SequenceFeatures();
    List<SequenceFeatureI> features = store.getFeaturesByOntology();
    assertTrue(features.isEmpty());
    assertTrue(store.getFeaturesByOntology(new String[] {}).isEmpty());
    assertTrue(store.getFeaturesByOntology((String[]) null).isEmpty());

    SequenceFeature transcriptFeature = new SequenceFeature("transcript",
            "desc", 10, 20, Float.NaN, null);
    store.add(transcriptFeature);

    /*
     * mRNA is a sub-type of transcript; added here 'as if' non-positional
     * just to show that non-positional features are included in results
     */
    SequenceFeature mrnaFeature = new SequenceFeature("mRNA", "desc", 0, 0,
            Float.NaN, null);
    store.add(mrnaFeature);

    SequenceFeature pfamFeature = new SequenceFeature("Pfam", "desc", 30,
            40, Float.NaN, null);
    store.add(pfamFeature);

    /*
     * "transcript" matches both itself and the sub-term "mRNA"
     */
    features = store.getFeaturesByOntology("transcript");
    assertEquals(features.size(), 2);
    assertTrue(features.contains(transcriptFeature));
    assertTrue(features.contains(mrnaFeature));

    /*
     * "mRNA" matches itself but not parent term "transcript"
     */
    features = store.getFeaturesByOntology("mRNA");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(mrnaFeature));

    /*
     * "pfam" is not an SO term but is included as an exact match
     */
    features = store.getFeaturesByOntology("mRNA", "Pfam");
    assertEquals(features.size(), 2);
    assertTrue(features.contains(mrnaFeature));
    assertTrue(features.contains(pfamFeature));

    features = store.getFeaturesByOntology("sequence_variant");
    assertTrue(features.isEmpty());
  }

  @Test(groups = "Functional")
  public void testSortFeatures()
  {
    List<SequenceFeatureI> sfs = new ArrayList<>();
    SequenceFeature sf1 = new SequenceFeature("Pfam", "desc", 30, 80,
            Float.NaN, null);
    sfs.add(sf1);
    SequenceFeature sf2 = new SequenceFeature("Rfam", "desc", 40, 50,
            Float.NaN, null);
    sfs.add(sf2);
    SequenceFeature sf3 = new SequenceFeature("Rfam", "desc", 50, 60,
            Float.NaN, null);
    sfs.add(sf3);

    // sort by end position descending
    SequenceFeatures.sortFeatures(sfs, false);
    assertSame(sfs.get(0), sf1);
    assertSame(sfs.get(1), sf3);
    assertSame(sfs.get(2), sf2);

    // sort by start position ascending
    SequenceFeatures.sortFeatures(sfs, true);
    assertSame(sfs.get(0), sf1);
    assertSame(sfs.get(1), sf2);
    assertSame(sfs.get(2), sf3);
  }

  @Test(groups = "Functional")
  public void testGetFeaturesForGroup()
  {
    SequenceFeaturesI store = new SequenceFeatures();

    List<SequenceFeatureI> features = store.getFeaturesForGroup(true, null);
    assertTrue(features.isEmpty());
    assertTrue(store.getFeaturesForGroup(false, null).isEmpty());
    assertTrue(store.getFeaturesForGroup(true, "Uniprot").isEmpty());
    assertTrue(store.getFeaturesForGroup(false, "Uniprot").isEmpty());

    SequenceFeature sf1 = new SequenceFeature("Pfam", "desc", 4, 10, 0f,
            null);
    SequenceFeature sf2 = new SequenceFeature("Pfam", "desc", 0, 0, 0f,
            null);
    SequenceFeature sf3 = new SequenceFeature("Pfam", "desc", 4, 10, 0f,
            "Uniprot");
    SequenceFeature sf4 = new SequenceFeature("Metal", "desc", 0, 0, 0f,
            "Rfam");
    SequenceFeature sf5 = new SequenceFeature("Cath", "desc", 5, 15, 0f,
            null);
    store.add(sf1);
    store.add(sf2);
    store.add(sf3);
    store.add(sf4);
    store.add(sf5);

    // positional features for null group, any type
    features = store.getFeaturesForGroup(true, null);
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf5));

    // positional features for null group, specified type
    features = store.getFeaturesForGroup(true, null,
            new String[]
            { "Pfam", "Xfam" });
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf1));
    features = store.getFeaturesForGroup(true, null,
            new String[]
            { "Pfam", "Xfam", "Cath" });
    assertEquals(features.size(), 2);
    assertTrue(features.contains(sf1));
    assertTrue(features.contains(sf5));

    // positional features for non-null group, any type
    features = store.getFeaturesForGroup(true, "Uniprot");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf3));
    assertTrue(store.getFeaturesForGroup(true, "Rfam").isEmpty());

    // positional features for non-null group, specified type
    features = store.getFeaturesForGroup(true, "Uniprot", "Pfam", "Xfam",
            "Rfam");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf3));
    assertTrue(
            store.getFeaturesForGroup(true, "Uniprot", "Cath").isEmpty());

    // non-positional features for null group, any type
    features = store.getFeaturesForGroup(false, null);
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf2));

    // non-positional features for null group, specified type
    features = store.getFeaturesForGroup(false, null, "Pfam", "Xfam");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf2));
    assertTrue(store.getFeaturesForGroup(false, null, "Cath").isEmpty());

    // non-positional features for non-null group, any type
    features = store.getFeaturesForGroup(false, "Rfam");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf4));
    assertTrue(store.getFeaturesForGroup(false, "Uniprot").isEmpty());

    // non-positional features for non-null group, specified type
    features = store.getFeaturesForGroup(false, "Rfam", "Pfam", "Metal");
    assertEquals(features.size(), 1);
    assertTrue(features.contains(sf4));
    assertTrue(store.getFeaturesForGroup(false, "Rfam", "Cath", "Pfam")
            .isEmpty());
  }

  @Test(groups = "Functional")
  public void testIsOntologyTerm()
  {
    SequenceFeatures store = new SequenceFeatures();
    assertTrue(store.isOntologyTerm("gobbledygook"));
    assertTrue(store.isOntologyTerm("transcript", "transcript"));
    assertTrue(store.isOntologyTerm("mRNA", "transcript"));
    assertFalse(store.isOntologyTerm("transcript", "mRNA"));
    assertTrue(store.isOntologyTerm("junk", "transcript", "junk"));
    assertTrue(store.isOntologyTerm("junk", new String[] {}));
    assertTrue(store.isOntologyTerm("junk", (String[]) null));
  }
}
