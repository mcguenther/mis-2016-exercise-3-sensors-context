package de.volzo.sensors;

/**
 * Created by Johannes on 19.05.2016.
 */
public class ActivityThreshold {

    private static final int WEIGHT_FREQUENCY = 2;
    private static final int WEIGHT_SPEED = 1;

    private final String name;
    private final double upperLimitFreq;
    private final double lowerLimitFreq;
    private final double upperLimitSpeed;
    private final double lowerLimitSpeed;

    @Override
    public String toString() {
        return this.name;
    }

    public String getName() {
        return name;
    }

    public double getUpperLimitFreq() {
        return upperLimitFreq;
    }

    public double getLowerLimitFreq() {
        return lowerLimitFreq;
    }

    public double getUpperLimitSpeed() {
        return upperLimitSpeed;
    }

    public double getLowerLimitSpeed() {
        return lowerLimitSpeed;
    }

    public ActivityThreshold(String name, double upperLimitFreq, double lowerLimitFreq, double upperLimitSpeed, double lowerLimitSpeed) {
        this.name = name;
        this.upperLimitFreq = upperLimitFreq;
        this.lowerLimitFreq = lowerLimitFreq;
        this.upperLimitSpeed = upperLimitSpeed;
        this.lowerLimitSpeed = lowerLimitSpeed;
    }

    public int getVote(double freq, double speed) {
        int score = 0;
        if(freq < upperLimitFreq && freq > lowerLimitFreq) {
            score += WEIGHT_FREQUENCY;
        } else {
            score -= WEIGHT_FREQUENCY;
        }
        if(speed < upperLimitSpeed && speed > lowerLimitSpeed) {
            score += WEIGHT_SPEED;
        } else {
            score -= WEIGHT_SPEED;
        }
        return score;
    }
}
