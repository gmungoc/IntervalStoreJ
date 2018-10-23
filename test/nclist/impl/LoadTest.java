package nclist.impl;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import junit.extensions.PA;

/**
 * Tests that load and analyze real world datasets
 * 
 * @author gmcarstairs
 *
 */
public class LoadTest
{
  /*
   * Ensembl and gnomAD variants on human BRAF gene 
   */
  private static final String VARIANTS_FILENAME = "test/nclist/impl/brafVariants.csv";

  /*
   * Coding, non-coding and pseudo-gene start-end loci on each human chromosome 
   */
  private static final String GENES_FILENAME = "test/nclist/impl/humanGenes.csv";

  /**
   * This 'test' loads a file of variants interval data to an NCList then
   * queries the list to report its greatest depth, and number of single-locus
   * entries
   * 
   * @throws IOException
   */
  @Test(groups = "Functional")
  public void testNclistDepth_variants() throws IOException
  {
    System.out.println("\ntestNclistDepth_variants: start");
    List<SimpleFeature> intervals = new ArrayList<>();
    File f = new File(VARIANTS_FILENAME);
    if (!f.exists())
    {
      fail(VARIANTS_FILENAME + " not found - please unzip "
              + VARIANTS_FILENAME + ".zip");
    }
    try (BufferedReader br = new BufferedReader(new FileReader(f)))
    {
      int snvCount = 0;

      String line = br.readLine();
      while (line != null)
      {
        if (!line.startsWith("#"))
        {
          String[] tokens = line.split("\\,");
          int from = Integer.parseInt(tokens[0]);
          int to = Integer.parseInt(tokens[1]);
          String desc = tokens[2];
          intervals.add(new SimpleFeature(from, to, desc));
          if (from == to)
          {
            snvCount++;
          }
        }
        line = br.readLine();
      }

      System.out.println("Found " + intervals.size() + " variants of which "
              + snvCount + " SNVs");
      NCList<SimpleFeature> ncl = buildNclist(intervals, "Variants");
      // System.out.println(ncl.prettyPrint());
    }

    System.out.println("testNclistDepth_variants: end\n");
  }

  /**
   * This 'test' loads a file of all human gene locus data, builds an NCList per
   * chromosome, and report the size and depth of each NCList
   * 
   * @throws IOException
   */
  @Test(groups = "Functional")
  public void testNclistDepth_genes() throws IOException
  {
    System.out.println("\ntestNclistDepth_genes: start");
    List<SimpleFeature> intervals = new ArrayList<>();
    File f = new File(GENES_FILENAME);
    if (!f.exists())
    {
      fail(GENES_FILENAME + " not found - please unzip " + GENES_FILENAME
              + ".zip");
    }
    try (BufferedReader br = new BufferedReader(new FileReader(f)))
    {
      String lastChr = null;
      String line = br.readLine();
      while (line != null)
      {
        if (!line.startsWith("#"))
        {
          String[] tokens = line.split("\\,");
          int from = Integer.parseInt(tokens[1]);
          int to = Integer.parseInt(tokens[2]);
          String chr = tokens[3];
          String desc = tokens[4];
          if (lastChr == null)
          {
            lastChr = chr;
          }

          if (!lastChr.equals(chr))
          {
            /*
             * end of chromosome - construct NCList for last chromosome
             */
            NCList<SimpleFeature> ncl = buildNclist(intervals,
                    "chr" + lastChr);
            if ("2".equals(lastChr))
            {
              // System.out.println(ncl.prettyPrint());
            }

            intervals = new ArrayList<>();
            lastChr = chr;
          }
          intervals.add(new SimpleFeature(from, to, desc));
        }
        line = br.readLine();
      }
      NCList<SimpleFeature> ncl = buildNclist(intervals,
              "chr" + lastChr);
    }

    System.out.println("testNclistDepth_genes: end\n");
  }

  /**
   * Helper method that constructs an NCList from the given intervals, and
   * reports its size, width and depth
   * 
   * @param intervals
   * @param title
   */
  protected NCList<SimpleFeature> buildNclist(
          List<SimpleFeature> intervals,
          String title)
  {
    NCList<SimpleFeature> ncl = new NCList<>(
            intervals);
    assertTrue(ncl.isValid());
    @SuppressWarnings("unchecked")
    int w = ((List<SimpleFeature>) PA.getValue(ncl, "subranges")).size();
    String msg = String.format(
            "%s NCList size=%d, width=%d (%.1f%%), depth=%d", title,
            ncl.size(), w, w * 100 / (float) ncl.size(), ncl.getDepth());
    System.out.println(msg);

    return ncl;
  }

  /**
   * This 'test' loads a file of all human gene locus data, builds an
   * IntervalStore per chromosome, and report the size and depth of each
   * contained NCList
   * 
   * @throws IOException
   */
  @Test(groups = "Functional")
  public void testIntervalStoreDepth_genes() throws IOException
  {
    System.out.println("\ntestIntervalStoreDepth_genes: start");
    File f = new File(GENES_FILENAME);
    if (!f.exists())
    {
      fail(GENES_FILENAME + " not found - please unzip " + GENES_FILENAME
              + ".zip");
    }
    try (BufferedReader br = new BufferedReader(new FileReader(f)))
    {
      String lastChr = null;
      IntervalStore<SimpleFeature> fs = new IntervalStore<>();
      String line = br.readLine();

      while (line != null)
      {
        if (!line.startsWith("#"))
        {
          String[] tokens = line.split("\\,");
          int from = Integer.parseInt(tokens[1]);
          int to = Integer.parseInt(tokens[2]);
          String chr = tokens[3];
          String desc = tokens[4];
          if (lastChr == null)
          {
            lastChr = chr;
          }
  
          if (!lastChr.equals(chr))
          {
            /*
             * end of chromosome - construct NCList for last chromosome
             */
            @SuppressWarnings("unchecked")
            int w = ((List<SimpleFeature>) PA.getValue(fs, "nonNested"))
                    .size();
            @SuppressWarnings("unchecked")
            NCList<SimpleFeature> ncl = (NCList<SimpleFeature>) PA
                    .getValue(fs, "nested");
            if ("2".equals(lastChr) || "5".equals(lastChr))
            {
              // System.out.println(ncl.prettyPrint());
            }
            int size = fs.size();
            String msg = String.format(
                    "chr%s size=%d, width=%d (%.1f%%), nclSize = %d, ncldepth=%d",
                    lastChr, size, w, w * 100 / (float) size, ncl.size(),
                    ncl.getDepth());
            System.out.println(msg);

            lastChr = chr;

            fs = new IntervalStore<>();
          }
          String description = chr + ":" + desc;
          SimpleFeature sf = new SimpleFeature(from, to, description);
          fs.add(sf);
        }
        line = br.readLine();
      }

      @SuppressWarnings("unchecked")
      int w = ((List<SimpleFeature>) PA.getValue(fs, "nonNested")).size();
      @SuppressWarnings("unchecked")
      NCList<SimpleFeature> ncl = (NCList<SimpleFeature>) PA
              .getValue(fs, "nested");
      int size = fs.size();
      String msg = String.format(
              "chr%s size=%d, width=%d (%.1f%%), nclSize = %d, ncldepth=%d",
              lastChr, size, w, w * 100 / (float) size, ncl.size(),
              ncl.getDepth());
      System.out.println(msg);
    }
  
    System.out.println("testIntervalStoreDepth_genes: end\n");
  }
}
