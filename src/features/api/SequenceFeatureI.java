package features.api;

public interface SequenceFeatureI extends FeatureLocationI
{

  /**
   * DOCUMENT ME!
   * 
   * @return DOCUMENT ME!
   */
  @Override
  int getBegin();

  /**
   * DOCUMENT ME!
   * 
   * @return DOCUMENT ME!
   */
  @Override
  int getEnd();

  /**
   * DOCUMENT ME!
   * 
   * @return DOCUMENT ME!
   */
  String getType();

  /**
   * DOCUMENT ME!
   * 
   * @return DOCUMENT ME!
   */
  String getDescription();

  void setDescription(String desc);

  String getFeatureGroup();

  float getScore();

  /**
   * Answers true if the feature's start/end values represent two related
   * positions, rather than ends of a range. Such features may be visualised or
   * reported differently to features on a range.
   */
  @Override
  boolean isContactFeature();

  /**
   * Answers true if the sequence has zero start and end position
   * 
   * @return
   */
  boolean isNonPositional();

}