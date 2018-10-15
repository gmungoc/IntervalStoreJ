package nclist.impl;

import nclist.api.IntervalI;

/**
 * A simplified feature instance sufficient for unit test purposes.
 */
public class SimpleFeature implements IntervalI
{
  final private int begin;

  final private int end;

  private String description;

  SimpleFeature(int from, int to, String desc)
  {
    begin = from;
    end = to;
    description = desc;
  }

  public SimpleFeature(SimpleFeature sf1)
  {
    this(sf1.begin, sf1.end, sf1.description);
  }

  @Override
  public int getBegin()
  {
    return begin;
  }

  @Override
  public int getEnd()
  {
    return end;
  }

  public String getDescription()
  {
    return description;
  }

  @Override
  public int hashCode()
  {
    return begin + 37 * end
            + (description == null ? 0 : description.hashCode());
  }

  /**
   * Equals method that requires two instances to have the same description, as
   * well as start and end position.
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj != null && obj instanceof SimpleFeature)
    {
      SimpleFeature o = (SimpleFeature) obj;
      if (this.begin == o.begin && this.end == o.end)
      {
        if (this.description == null)
        {
          return o.description == null;
        }
        return this.description.equals(o.description);
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return begin + ":" + end + ":" + description;
  }

}
