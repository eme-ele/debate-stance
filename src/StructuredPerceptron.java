import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import LBJ2.infer.GurobiHook;
import LBJ2.infer.ILPSolver;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class StructuredPerceptron {

    public int maxIter;
    public double learnRate;
    public List<List<Float> > weights;
    public List<Float> bias;

     // ILP solver for inference
    public ILPSolver ILPsolver;


    public StructuredPerceptron(int maxIter, double learnRate) {
        this.maxIter = maxIter;
        this.learnRate = learnRate;
        // library for inference
        this.ILPsolver = new GurobiHook();
    }

    public void initWeights(int numFeats, int numInferenceLabels) {
        this.weights = new ArrayList<>();
        this.bias = new ArrayList<>();
        for (int i = 0; i < numInferenceLabels; i++) {
            ArrayList<Float> inferenceWeights = new ArrayList<>();
            for (int j = 0; j < numFeats; j++) {
                inferenceWeights.add((float)0.0);
            }
            this.weights.add(inferenceWeights);
            this.bias.add((float)0.0);
        }
    }

    public float dotProduct(HashMap<Integer, Float> sample, int infereceLabel) {
        float dotProduct = (float)0.0;
        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            dotProduct += entry.getValue() *
                          this.weights.get(infereceLabel).get(entry.getKey());
        }
        dotProduct += this.bias.get(infereceLabel);
        return dotProduct;
    }

    public void ILPinference() {
        this.ILPsolver.setMaximize(true);
    }

}

class InferenceVariable {

    int id;
    double score;
    String descriptor;
    int varIndex;
    String edgeType;

    // edge related
    String topic;
    String headId;
    String tailId;

    public InferenceVariable(int id, double score,
                             int varIndex, String edgeType,
                             String desc, String topic,
                             String headId, String tailId) {
        this.id = id;
        this.score = score;
        this.varIndex = varIndex;
        this.edgeType = edgeType;
        this.descriptor = desc;

        // edge related
        this.topic = topic;
        this.headId = headId;
        this.tailId = tailId;
    }

    @Override
    public String toString() {
        return descriptor + "\n";
    }

}


class Solution {

    List<InferenceVariable> solution;

    public Solution(List<InferenceVariable> solutionVars) {
        this.solution=solutionVars;
    }

    @Override
    public String toString() {
        return solution.toString();
    }

}
