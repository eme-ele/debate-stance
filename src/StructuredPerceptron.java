import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import LBJ2.infer.GurobiHook;
import LBJ2.infer.ILPSolver;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class StructuredPerceptron {

    public int maxIter;
    public float learnRate;
    public List<List<Float> > weights;
    public List<Float> bias;

    // feature extractor
    EdgeFeatureExtractor fe;

    // edge variables
    String[] edgeVariables = {"a-a", "a-d",
                              "d-a", "d-d"};


    public StructuredPerceptron(int maxIter, float learnRate,
                                EdgeFeatureExtractor fe) {
        this.maxIter = maxIter;
        this.learnRate = learnRate;
        // feature extractor
        this.fe = fe;

    }

    public void initWeights(int numFeats, int numInferenceLabels) {
        this.weights = new ArrayList<>();
        this.bias = new ArrayList<>();
        for (int i = 0; i < numInferenceLabels; i++) {
            ArrayList<Float> inferenceWeights = new ArrayList<>();
            for (int j = 0; j < numFeats; j++) {
                Random r = new Random();
                double randomValue = 0.0001 + (0.001 - 0.0001) * r.nextDouble();
                inferenceWeights.add((float)0.0);
            }
            this.weights.add(inferenceWeights);
            Random r = new Random();
            double randomValue = 0.0001 + (0.001 - 0.0001) * r.nextDouble();
            this.bias.add((float)randomValue);
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

    public void updateWeights(int predEdge, int goldenEdge, HashMap<Integer, Float> sample) {
        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            float w_prev;
            int index = entry.getKey();
            float value = entry.getValue();
            // promote golden
            w_prev = this.weights.get(goldenEdge).get(index);
            this.weights.get(goldenEdge).set(index, w_prev + this.learnRate * value);
            // demote predicted
            w_prev = this.weights.get(predEdge).get(index);
            this.weights.get(predEdge).set(index, w_prev - this.learnRate * value);
        }
    }

    public int getEdge(int headStance, int tailStance) {
        if (headStance == 1 && tailStance == 1) {
            return 0;
        } else if (headStance == 1 && tailStance == -1) {
            return 1;
        } else if (headStance == -1 && tailStance == 1) {
            return 2;
        } else {
            return 3;
        }
    }

    public int getHeadStance(int edge) {
        if (edge == 0) {
            return 1;
        } else if (edge == 1) {
            return 1;
        } else if (edge == 2) {
            return -1;
        } else {
            return -1;
        }
    }

    public int getTailStance(int edge) {
        if (edge == 0) {
            return 1;
        } else if (edge == 1) {
            return -1;
        } else if (edge == 2) {
            return 1;
        } else {
            return -1;
        }

    }

    public Solution ILPinference(Tree tree) {
        //System.out.println(tree);
        Solution s = null;
        ILPSolver solver = new GurobiHook();
        solver.setMaximize(true);

        try {
            // Map of edge id and list of variables for each edge, one corresponding to each edge type
            HashMap<String, List<InferenceVariable> > varsCache = new HashMap<>();

            // Add all variables and simple cardinality constraints
            for (String head: tree.adjacencyList.keySet()) {
                Instance headOpinion = tree.nodes.get(head);
                for (String tail: tree.adjacencyList.get(head)) {
                    Instance tailOpinion = tree.nodes.get(tail);
                    HashMap<Integer, Float> edgeFeats = fe.getEdgeFeatures(headOpinion, tailOpinion);
                    /*Opinion ho = (Opinion) headOpinion;
                    Opinion to = (Opinion) tailOpinion;*/
                    String edgeId = tree.topic+head+tail;

                    varsCache.put(edgeId, new ArrayList<>());

                    // iterate over possible classes and add ilp variables
                    for (int j = 0; j < 4; j++) {
                        double score = dotProduct(edgeFeats, j);
                        int id = solver.addBooleanVariable(score);
                        String edge = edgeVariables[j];
                        //int goldenEdge = getEdge(ho.stance, to.stance);
                        InferenceVariable v = new InferenceVariable(id, score, edgeId, j,
                                                                    head+"--"+edge+"-->"+tail,
                                                                    headOpinion, tailOpinion,
                                                                    edgeFeats);
                        varsCache.get(edgeId).add(v);
                    }

                    // add cardinality constraints
                    // only one edge type is allowed per edge
                    int[] constraintVars = new int[varsCache.get(edgeId).size()];
                    double[] constraintCoef = new double[varsCache.get(edgeId).size()];
                    for (int index = 0; index < varsCache.get(edgeId).size(); index++) {
                        InferenceVariable iv = varsCache.get(edgeId).get(index);
                        constraintVars[index] = iv.id;
                        constraintCoef[index] = 1;
                    }
                    solver.addEqualityConstraint(constraintVars, constraintCoef, 1);
                }
            }

            /*
            // printing for debugging
            for (List<InferenceVariable> varsAtId: varsCache.values()) {
                for (InferenceVariable v: varsAtId) {
                    System.out.println(v);
                }
            }*/


            // Add consistency constraints
            for (String head: tree.adjacencyList.keySet()) {
                Instance headOpinion = tree.nodes.get(head);
                for(String tail: tree.adjacencyList.get(head)) {
                    Instance tailOpinion = tree.nodes.get(tail);
                    String edgeId = tree.topic+head+tail;

                    // edges must be consistent with previous adjacent edge
                    // only one because it is a tree
                    if (tree.parents.containsKey(head)) {
                        String parent = tree.parents.get(head);
                        String prevEdgeId = tree.topic+parent+head;

                        //System.out.println("PREV: "+ prevEdgeId);
                        //System.out.println("CURR: " + edgeId);
                        //  current && aa => prev && (aa || da)
                        int[] constraintVars01 = {varsCache.get(edgeId).get(0).id,
                                                  varsCache.get(prevEdgeId).get(0).id,
                                                  varsCache.get(prevEdgeId).get(2).id};
                        double[] constraintCoef01 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars01, constraintCoef01, 0);
                        // current & ad => prev && (aa || da)
                        int[] constraintVars02 = {varsCache.get(edgeId).get(1).id,
                                                  varsCache.get(prevEdgeId).get(0).id,
                                                  varsCache.get(prevEdgeId).get(2).id};
                        double[] constraintCoef02 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars02, constraintCoef02, 0);
                        // current & da => prev & (ad || dd)
                        int[] constraintVars03 = {varsCache.get(edgeId).get(2).id,
                                                  varsCache.get(prevEdgeId).get(1).id,
                                                  varsCache.get(prevEdgeId).get(3).id};
                        double[] constraintCoef03 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars03, constraintCoef03, 0);
                        // current & dd => prev & (ad || dd)
                        int[] constraintVars04 = {varsCache.get(edgeId).get(3).id,
                                                  varsCache.get(prevEdgeId).get(1).id,
                                                  varsCache.get(prevEdgeId).get(3).id};
                        double[] constraintCoef04 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars04, constraintCoef04, 0);

                    }

                    // edges must be consistent with subsequent adjacent edges
                    for (String subseq: tree.adjacencyList.get(tail)) {
                        String subseqEdgeId = tree.topic+tail+subseq;

                        // current && aa => next && (aa || ad)
                        int[] constraintVars1 = {varsCache.get(edgeId).get(0).id,
                                                varsCache.get(subseqEdgeId).get(0).id,
                                                varsCache.get(subseqEdgeId).get(1).id};
                        double[] constraintCoef1 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars1, constraintCoef1, 0);

                        // current && ad => next && (dd || da)
                        int[] constraintVars2 = {varsCache.get(edgeId).get(1).id,
                                                 varsCache.get(subseqEdgeId).get(2).id,
                                                 varsCache.get(subseqEdgeId).get(3).id};
                        double[] constraintCoef2 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars2, constraintCoef2, 0);

                        // current && da => next && (aa || ad)
                        int[] constraintVars3 = {varsCache.get(edgeId).get(2).id,
                                                 varsCache.get(subseqEdgeId).get(0).id,
                                                 varsCache.get(subseqEdgeId).get(1).id};
                        double[] constraintCoef3 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars3, constraintCoef3, 0);

                        // current && dd => next && (dd || da)
                        int[] constraintVars4 = {varsCache.get(edgeId).get(3).id,
                                                 varsCache.get(subseqEdgeId).get(2).id,
                                                 varsCache.get(subseqEdgeId).get(3).id};
                        double[] constraintCoef4 = {-1, 1, 1};
                        solver.addGreaterThanConstraint(constraintVars4, constraintCoef4, 0);


                    }
                }
            }

            if (solver.solve()) {
                List<InferenceVariable> solutionVars = new ArrayList<>();
                for (List<InferenceVariable> varsAtId: varsCache.values()) {
                    for (InferenceVariable v: varsAtId) {
                        if (solver.getBooleanValue(v.id)) {
                            solutionVars.add(v);
                        }
                    }
                }
                s = new Solution(solutionVars);
            } else {
                System.out.println("solve failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return s;
    }


    public void train(List<Tree> dataset) {
        initWeights(fe.numFeatures * 2, 4);
        for (int i = 0; i < maxIter; i++) {

            int numMistakes = 0;
            int total = 0;
            for (Tree t: dataset) {
                Solution s = ILPinference(t);

                // update rule
                for (InferenceVariable v: s.solution) {
                    Opinion head = (Opinion) v.headOpinion;
                    Opinion tail = (Opinion) v.tailOpinion;
                    int goldenEdge = getEdge(head.stance, tail.stance);
                    if (v.edgeTypeIndex != goldenEdge) {
                        numMistakes += 1;
                        total += 1;
                        updateWeights(v.edgeTypeIndex, goldenEdge, v.edgeFeats);
                    } else {
                        total += 1;
                    }
                }
            }

            double trainAccuracy = 1 - ((1.0*numMistakes)/total);
            //System.out.println("Training accuracy: " + trainAccuracy);
            if (numMistakes == 0)
                return;

        }
    }

    public void test(List<Tree> dataset) {
        int numMistakes = 0;
        HashMap<String, Pair> predictions = new HashMap<>();
        for (Tree t: dataset) {
            Solution s = ILPinference(t);
            for (InferenceVariable v: s.solution) {
                Opinion head = (Opinion) v.headOpinion;
                Opinion tail = (Opinion) v.tailOpinion;
                int headStance = getHeadStance(v.edgeTypeIndex);
                int tailStance = getTailStance(v.edgeTypeIndex);

                Pair pHead = new Pair(head.stance, headStance);
                Pair pTail = new Pair(tail.stance, tailStance);

                String headId = head.topic+head.debate+head.id;
                String tailId = tail.topic+tail.debate+tail.id;
                predictions.put(headId, pHead);
                predictions.put(tailId, pTail);
            }
        }

        int total = predictions.size();
        for (String opinion: predictions.keySet()) {
            Pair p = predictions.get(opinion);
            if (p.pred != p.gold) {
                numMistakes += 1;
            }
        }

        double testAccuracy = 1 - ((1.0*numMistakes)/total);
        System.out.println("Total examples: " + total);
        System.out.println("Accuracy: " + testAccuracy);

    }

}

class InferenceVariable {

    int id;
    double score;
    String edgeId;
    int edgeTypeIndex;
    String descriptor;
    Instance headOpinion;
    Instance tailOpinion;
    HashMap<Integer, Float> edgeFeats;

    public InferenceVariable(int id, double score, String edgeId,
                             int edgeTypeIndex, String desc,
                             Instance headOpinion,
                             Instance tailOpinion,
                             HashMap<Integer, Float> edgeFeats) {
        this.id = id;
        this.score = score;
        this.edgeId = edgeId;
        this.edgeTypeIndex = edgeTypeIndex;
        this.descriptor = desc;
        this.headOpinion = headOpinion;
        this.tailOpinion = tailOpinion;
        this.edgeFeats = edgeFeats;
    }

    @Override
    public String toString() {
        return descriptor;
    }

}


class Solution {

    public List<InferenceVariable> solution;

    public Solution(List<InferenceVariable> solutionVars) {
        this.solution = solutionVars;
    }

    @Override
    public String toString() {
        return solution.toString();
    }

}

class Pair {
    public int pred;
    public int gold;

    public Pair(int gold, int pred) {
        this.gold = gold;
        this.pred = pred;
    }
}
