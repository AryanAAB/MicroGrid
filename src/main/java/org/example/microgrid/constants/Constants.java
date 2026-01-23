package org.example.microgrid.constants;

import java.util.concurrent.TimeUnit;

public final class Constants
{
    private Constants() {}

    public static final double SEC_IN_MIN  = 60.0;
    public static final double MIN_IN_HOUR = 60.0;
    public static final double SEC_IN_HOUR = SEC_IN_MIN * MIN_IN_HOUR;
    public static final double HOUR_IN_DAY = 24.0;
    public static final double SEC_IN_DAY  = HOUR_IN_DAY * SEC_IN_HOUR;

    //how many second is 1 step in simulation
    public static final double STEP_TO_SECONDS = 60;
}
