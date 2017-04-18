package com.roamingroths.cmcc.data;

import android.provider.BaseColumns;

public class ObservationContract {

  public static final class ObservationEntry implements BaseColumns {

    public static final String TABLE_NAME = "observation";

    /*
     * The date column will store the UTC date that correlates to the local date for which
     * each particular weather row represents. For example, if you live in the Eastern
     * Standard Time (EST) time zone and you load weather data at 9:00 PM on September 23, 2016,
     * the UTC time stamp for that particular time would be 1474678800000 in milliseconds.
     * However, due to time zone offsets, it would already be September 24th, 2016 in the GMT
     * time zone when it is 9:00 PM on the 23rd in the EST time zone. In this example, the date
     * column would hold the date representing September 23rd at midnight in GMT time.
     * (1474588800000)
     *
     * The reason we store GMT time and not local time is because it is best practice to have a
     * "normalized", or standard when storing the date and adjust as necessary when
     * displaying the date. Normalizing the date also allows us an easy way to convert to
     * local time at midnight, as all we have to do is add a particular time zone's GMT
     * offset to this date to get local time at midnight on the appropriate date.
     */
    public static final String COLUMN_CREATION_DATE = "creation_date";
    public static final String COLUMN_FLOW = "flow";
    public static final String COLUMN_OCCURENCE = "occurrence";
  }
}
