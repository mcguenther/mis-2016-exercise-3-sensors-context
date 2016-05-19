package de.volzo.sensors;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Johannes on 19.05.2016.
 */
public class SensorFusion {

    private float speed = 0f;
    public CircularFifoQueue<Double> frequencies;
    private final List<ActivityThreshold> activityThresholdList = new ArrayList<>();
    //private static int meanFDriving = 3;

    public SensorFusion() {
        this(512);
    }

    public SensorFusion(int queueSize) {
        frequencies = new CircularFifoQueue<Double>(queueSize);
        activityThresholdList.add(new ActivityThreshold("walking", 0.6, 0.2, 1.7, 0.5));
        activityThresholdList.add(new ActivityThreshold("biking", 1.8, 0.6, 7, 2));

        // activity "driving" could not be tested since no car was available; however, the
        // rotational speed should be less then 1800: https://de.wikipedia.org/wiki/Drehzahl
        activityThresholdList.add(new ActivityThreshold("driving", 18000, 500, 80, 7));
        activityThresholdList.add(new ActivityThreshold("lying", 0.2, 0, 0.5, 0));
    }

    public void updateGSSpeed(float speed) {
        this.speed = speed;
    }

    public String getActivity() {
        // machine learning e.g. decision trees or neural networks would be
        // appropriate to decide for a activity; instead, we use manually chosen thresholds
        // and let each frequency in the past vote for an activity
        Map<String, Integer> votes = new HashMap<>();
        for (double freq : frequencies) {

            Iterator it = activityThresholdList.iterator();
            while (it.hasNext()) {
                ActivityThreshold act = (ActivityThreshold) it.next();
                int singleVote = act.getVote(freq, this.speed);
                String actName = act.getName();
                if (!votes.containsKey(actName)) {
                    votes.put(actName, singleVote);
                } else {
                    votes.put(actName, votes.get(actName) + singleVote);
                }
            }
        }

        Integer max = null;
        String maxAct = "unknown";
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            int voteNum = entry.getValue();
            String actName = entry.getKey();

            if (max == null || voteNum > max) {
                max = voteNum;
                maxAct = actName;
            }
        }
        return maxAct;
    }

    public void updateFrequency(double freq) {
        frequencies.add(freq);
    }
}
