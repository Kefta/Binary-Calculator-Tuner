package code_gs.binarycalculatortuner;

public class Tuner
{
    private static final double PEAK_HEIGHT = 0.7;

    static double getFrequency(int rate, int size, int count, short[] buffer)
    {
        int[] condensed = new int[size/2];

        // Each point takes up two shorts, combine them for straight comparison
        for (int i = 0; i < count; i += 2)
        {
            condensed[i / 2] = (buffer[i] & 0xFFFF) | ((buffer[i] & 0xFFFF) << 16);
        }

        double prevPeak = 0;
        double prevDx = 0;
        double maxDist = 0;

        int sample = 0;
        int len = condensed.length / 2;

            for (int i = 0; i < len; i++)
            {
                double dist = 0;

                for (int j = 0; j < len; j++)
                {
                    // Compare the base wave peak with one offset by horizontal translation i
                    dist += Math.abs(condensed[j] - condensed[i + j]);
                }

                double dx = prevPeak - dist;

                // Change of sign in dx, only check peaks
                if (dx < 0 && prevDx > 0 && dist < PEAK_HEIGHT * maxDist && sample <= 0)
                {
                    sample = i - 1;
                }

                prevDx = dx;
                prevPeak = dist;
                maxDist = Math.max(dist, maxDist);
            }

        // If we have a valid frequency, put it in a single octave range
        if (sample > 0)
        {
            double frequency = (rate/sample);
            //Log.d("Calculator", String.format("%.2fhz",frequency));

            // A4 - low end
            while (frequency < 440)
            {
                frequency *= 2;
            }

            // A5 - high end
            while (frequency > 880)
            {
                frequency /= 2;
            }

            return frequency;
        }

        // No clear peaks could be found; sound too varied or not varied enough
        return -1;
    }
}
