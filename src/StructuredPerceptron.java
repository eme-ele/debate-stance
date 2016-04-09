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
    public List<List<Float> > edgeWeights;
    public List<List<Float> > authorWeights;
    public List<Float> edgeBias;
    public List<Float> authorBias;


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

    public void initWeights(int numFeats, int numInferenceLabels,
                            List<List<Float>> weights, List<Float> bias) {
        for (int i = 0; i < numInferenceLabels; i++) {
            ArrayList<Float> inferenceWeights = new ArrayList<>();
            for (int j = 0; j < numFeats; j++) {
                Random r = new Random();
                double randomValue = 0.0001 + (0.001 - 0.0001) * r.nextDouble();
                inferenceWeights.add((float)0.0);
            }
            weights.add(inferenceWeights);
            Random r = new Random();
            double randomValue = 0.0001 + (0.001 - 0.0001) * r.nextDouble();
            bias.add((float)randomValue);
        }
    }

    /*public void pushWeights(int weakEdge,
                            List<List<Float>> weights, List<Float> bias) {
        for (int i = 0; i < weights.get(weakEdge).size(); i++) {
            float prevWeight = weights.get(weakEdge).get(i);
            Random r = new Random();
            double randomValue = 0.01 + (0.1 - 0.01) * r.nextDouble();
            weights.get(weakEdge).set(i, prevWeight);
        }
    }*/


    public float dotProduct(HashMap<Integer, Float> sample, int infereceLabel,
                            List<List<Float> > weights, List<Float> bias) {
        float dotProduct = (float)0.0;
        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            dotProduct += entry.getValue() *
                          weights.get(infereceLabel).get(entry.getKey());
        }
        dotProduct += bias.get(infereceLabel);
        return dotProduct;
    }

    public void updateWeights(int predEdge, int goldenEdge, HashMap<Integer, Float> sample,
                              List<List<Float> > weights, List<Float> bias) {
        // careful with -1 as an index in the stance case
        // this is a hack !
        if (predEdge == -1)
            predEdge = 0;
        if (goldenEdge == -1)
            goldenEdge = 0;

        for (Map.Entry<Integer, Float> entry: sample.entrySet()) {
            float w_prev;
            int index = entry.getKey();
            float value = entry.getValue();
            // promote goldens
            w_prev = weights.get(goldenEdge).get(index);
            weights.get(goldenEdge).set(index, w_prev + this.learnRate * value);
            // demote predicted
            w_prev = weights.get(predEdge).get(index);
            weights.get(predEdge).set(index, w_prev - this.learnRate * value);
        }
        float b_prev;
        b_prev = bias.get(goldenEdge);
        bias.set(goldenEdge, b_prev + this.learnRate);
        b_prev = bias.get(predEdge);
        bias.set(predEdge, b_prev + this.learnRate);
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

    public ILPSolver addAuthorVariables(int goldenStance,
                                        String authorId,
                                        List<Instance> authorPosts,
                                        HashMap<String, List<InferenceVariable> > varsCache,
                                        ILPSolver solver) {
        if (!varsCache.containsKey(authorId)) {
            varsCache.put(authorId, new ArrayList<>());
            //double score1 = dotProduct(feats, 0, this.authorWeights, this.authorBias);
            //double score2 = dotProduct(feats, 1, this.authorWeights, this.authorBias);
            int id1 = solver.addBooleanVariable(1);
            int id2 = solver.addBooleanVariable(1);
            InferenceVariable v1 = new AuthorVariable(id1, 1, authorId, -1,
                                                      authorId+"="+-1,
                                                      goldenStance);
            InferenceVariable v2 = new AuthorVariable(id2, 1, authorId, 1,
                                                      authorId+"="+1,
                                                      goldenStance);
            int[] constraintVars = {v1.ilpId, v2.ilpId};
            double[] constraintCoef = {1, 1};
            solver.addEqualityConstraint(constraintVars, constraintCoef, 1);
            varsCache.get(authorId).add(v1);
            varsCache.get(authorId).add(v2);
        }
        return solver;
    }

    public ILPSolver addEdgeVariables(Tree tree,
                                      Instance headOpinion, Instance tailOpinion,
                                      String head, String tail, String topic,
                                      HashMap<String, List<InferenceVariable> > varsCache,
                                      ILPSolver solver,
                                      HashMap<String,List<Instance>> byAuthor,
                                      boolean authorLevel) {

        String edgeId = topic+head+tail;
        String headAuthor = ((Opinion) headOpinion).author;
        String tailAuthor = ((Opinion) tailOpinion).author;

        HashMap<Integer, Float> edgeFeats;
        if (authorLevel)
            edgeFeats = fe.getAuthorLevelEdgeFeatures(tree, headOpinion, tailOpinion,
                                                      byAuthor.get(headAuthor),
                                                      byAuthor.get(tailAuthor));
        else
            edgeFeats = fe.getOpinionLevelEdgeFeatures(tree, headOpinion, tailOpinion);

        varsCache.put(edgeId, new ArrayList<>());
        // iterate over possible classes and add ilp variables
        for (int j = 0; j < 4; j++) {
            int weakEdge = getEdge(((Opinion)headOpinion).weakStance, ((Opinion)tailOpinion).weakStance);
            //pushWeights(weakEdge, this.edgeWeights, this.edgeBias);
            double score = dotProduct(edgeFeats, j, this.edgeWeights, this.edgeBias);
            int id = solver.addBooleanVariable(score);
            String edge = edgeVariables[j];
            InferenceVariable v = new EdgeVariable(id, score, edgeId, j,
                                                   head+"--"+edge+"-->"+tail,
                                                   headOpinion, tailOpinion,
                                                   edgeFeats);
            varsCache.get(edgeId).add(v);
        }
        return solver;
    }

    public ILPSolver addCardinalityEdgeConstraints(String head, String tail, String topic,
                                                   HashMap<String, List<InferenceVariable> > varsCache,
                                                   ILPSolver solver) {
        // add cardinality constraints
        // only one edge type is allowed per edge
        String edgeId = topic+head+tail;
        int[] constraintVars = new int[varsCache.get(edgeId).size()];
        double[] constraintCoef = new double[varsCache.get(edgeId).size()];
        for (int index = 0; index < varsCache.get(edgeId).size(); index++) {
            InferenceVariable iv = varsCache.get(edgeId).get(index);
            constraintVars[index] = iv.ilpId;
            constraintCoef[index] = 1;
        }
        solver.addEqualityConstraint(constraintVars, constraintCoef, 1);
        return solver;
    }

    public ILPSolver addPreviousEdgeConstraints(Tree tree,
                                                String head, String tail,
                                                HashMap<String, List<InferenceVariable> > varsCache,
                                                ILPSolver solver) {

        String edgeId = tree.topic+head+tail;
        if (tree.parents.containsKey(head)) {
            String parent = tree.parents.get(head);
            String prevEdgeId = tree.topic+parent+head;

            //System.out.println("PREV: "+ prevEdgeId);
            //System.out.println("CURR: " + edgeId);
            //  current && aa => prev && (aa || da)
            int[] constraintVars01 = {varsCache.get(edgeId).get(0).ilpId,
                                      varsCache.get(prevEdgeId).get(0).ilpId,
                                      varsCache.get(prevEdgeId).get(2).ilpId};
            double[] constraintCoef01 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars01, constraintCoef01, 0);
            // current & ad => prev && (aa || da)
            int[] constraintVars02 = {varsCache.get(edgeId).get(1).ilpId,
                                      varsCache.get(prevEdgeId).get(0).ilpId,
                                      varsCache.get(prevEdgeId).get(2).ilpId};
            double[] constraintCoef02 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars02, constraintCoef02, 0);
            // current & da => prev & (ad || dd)
            int[] constraintVars03 = {varsCache.get(edgeId).get(2).ilpId,
                                      varsCache.get(prevEdgeId).get(1).ilpId,
                                      varsCache.get(prevEdgeId).get(3).ilpId};
            double[] constraintCoef03 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars03, constraintCoef03, 0);
            // current & dd => prev & (ad || dd)
            int[] constraintVars04 = {varsCache.get(edgeId).get(3).ilpId,
                                      varsCache.get(prevEdgeId).get(1).ilpId,
                                      varsCache.get(prevEdgeId).get(3).ilpId};
            double[] constraintCoef04 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars04, constraintCoef04, 0);

        }
        return solver;
    }

    public ILPSolver addSubsequentEdgeConstraints(Tree tree,
                                                  String head, String tail,
                                                  HashMap<String, List<InferenceVariable> > varsCache,
                                                  ILPSolver solver) {
        String edgeId = tree.topic+head+tail;
        for (String subseq: tree.adjacencyList.get(tail)) {
            String subseqEdgeId = tree.topic+tail+subseq;

            // current && aa => next && (aa || ad)
            int[] constraintVars1 = {varsCache.get(edgeId).get(0).ilpId,
                                    varsCache.get(subseqEdgeId).get(0).ilpId,
                                    varsCache.get(subseqEdgeId).get(1).ilpId};
            double[] constraintCoef1 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars1, constraintCoef1, 0);

            // current && ad => next && (dd || da)
            int[] constraintVars2 = {varsCache.get(edgeId).get(1).ilpId,
                                     varsCache.get(subseqEdgeId).get(2).ilpId,
                                     varsCache.get(subseqEdgeId).get(3).ilpId};
            double[] constraintCoef2 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars2, constraintCoef2, 0);

            // current && da => next && (aa || ad)
            int[] constraintVars3 = {varsCache.get(edgeId).get(2).ilpId,
                                     varsCache.get(subseqEdgeId).get(0).ilpId,
                                     varsCache.get(subseqEdgeId).get(1).ilpId};
            double[] constraintCoef3 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars3, constraintCoef3, 0);

            // current && dd => next && (dd || da)
            int[] constraintVars4 = {varsCache.get(edgeId).get(3).ilpId,
                                     varsCache.get(subseqEdgeId).get(2).ilpId,
                                     varsCache.get(subseqEdgeId).get(3).ilpId};
            double[] constraintCoef4 = {-1, 1, 1};
            solver.addGreaterThanConstraint(constraintVars4, constraintCoef4, 0);


        }

        return solver;
    }

    public ILPSolver addConsistencyEdgeConstraints(Tree tree,
                                                   HashMap<String, List<InferenceVariable> > varsCache,
                                                   ILPSolver solver) {
        // Add consistency constraints
        for (String head: tree.adjacencyList.keySet()) {
            Instance headOpinion = tree.nodes.get(head);
            for(String tail: tree.adjacencyList.get(head)) {
                // edges must be consistent with previous adjacent edge
                // only one because it is a tree
                solver = addPreviousEdgeConstraints(tree, head, tail, varsCache, solver);
                // edges must be consistent with subsequent adjacent edges
                solver = addSubsequentEdgeConstraints(tree, head, tail, varsCache, solver);
            }
        }
        return solver;

    }

    public ILPSolver addConsistencyAuthorConstraints(Instance headOpinion, Instance tailOpinion,
                                                     String head, String tail, String topic,
                                                     HashMap<String, List<InferenceVariable> > varsCache,
                                                     ILPSolver solver)
    {
        String edgeId = topic+head+tail;
        String headAuthor = ((Opinion) headOpinion).author;
        String tailAuthor = ((Opinion) tailOpinion).author;

        // aa => head = 1 & tail = 1
        int[] constraintVars1 = {varsCache.get(edgeId).get(0).ilpId,
                                 varsCache.get(headAuthor).get(1).ilpId};
        double[] constraintCoef1 = {-1, 1};
        int[] constraintVars2 = {varsCache.get(edgeId).get(0).ilpId,
                                 varsCache.get(tailAuthor).get(1).ilpId};
        double[] constraintCoef2 = {-1, 1};
        solver.addGreaterThanConstraint(constraintVars1, constraintCoef1, 0);
        solver.addGreaterThanConstraint(constraintVars2, constraintCoef2, 0);

        // ad => head = 1 && tail -1
        int[] constraintVars3 = {varsCache.get(edgeId).get(1).ilpId,
                                 varsCache.get(headAuthor).get(1).ilpId};
        double[] constraintCoef3 = {-1, 1};
        int[] constraintVars4 = {varsCache.get(edgeId).get(1).ilpId,
                                 varsCache.get(tailAuthor).get(0).ilpId};
        double[] constraintCoef4 = {-1, 1};
        solver.addGreaterThanConstraint(constraintVars3, constraintCoef3, 0);
        solver.addGreaterThanConstraint(constraintVars4, constraintCoef4, 0);

        // da => head -1 && tail = 1
        int[] constraintVars5 = {varsCache.get(edgeId).get(2).ilpId,
                                 varsCache.get(headAuthor).get(0).ilpId};
        double[] constraintCoef5 = {-1, 1};
        int[] constraintVars6 = {varsCache.get(edgeId).get(2).ilpId,
                                 varsCache.get(tailAuthor).get(1).ilpId};
        double[] constraintCoef6 = {-1, 1};
        solver.addGreaterThanConstraint(constraintVars5, constraintCoef5, 0);
        solver.addGreaterThanConstraint(constraintVars6, constraintCoef6, 0);

        // dd => head = 1 && tail -1
        int[] constraintVars7 = {varsCache.get(edgeId).get(3).ilpId,
                                 varsCache.get(headAuthor).get(0).ilpId};
        double[] constraintCoef7 = {-1, 1};
        int[] constraintVars8 = {varsCache.get(edgeId).get(3).ilpId,
                                 varsCache.get(tailAuthor).get(0).ilpId};
        double[] constraintCoef8 = {-1, 1};
        solver.addGreaterThanConstraint(constraintVars7, constraintCoef7, 0);
        solver.addGreaterThanConstraint(constraintVars8, constraintCoef8, 0);

        // head  1 => aa or ad
        // aa + ad -head1 >= 0
        int[] constraintVars9 = {varsCache.get(headAuthor).get(1).ilpId,
                                 varsCache.get(edgeId).get(0).ilpId,
                                 varsCache.get(edgeId).get(1).ilpId};
        double [] constraintCoef9 = {-1, 1, 1};
        solver.addGreaterThanConstraint(constraintVars9, constraintCoef9, 0);
        // head  -1 => da or dd
        int[] constraintVars10 = {varsCache.get(headAuthor).get(0).ilpId,
                                 varsCache.get(edgeId).get(2).ilpId,
                                 varsCache.get(edgeId).get(3).ilpId};
        double [] constraintCoef10 = {-1, 1, 1};
        solver.addGreaterThanConstraint(constraintVars10, constraintCoef10, 0);
        // tail 1 => aa or da
        int[] constraintVars11 = {varsCache.get(tailAuthor).get(1).ilpId,
                                 varsCache.get(edgeId).get(0).ilpId,
                                 varsCache.get(edgeId).get(2).ilpId};
        double [] constraintCoef11 = {-1, 1, 1};
        solver.addGreaterThanConstraint(constraintVars11, constraintCoef11, 0);
        // tail -1 => ad or dd
        int[] constraintVars12 = {varsCache.get(tailAuthor).get(0).ilpId,
                                 varsCache.get(edgeId).get(1).ilpId,
                                 varsCache.get(edgeId).get(3).ilpId};
        double [] constraintCoef12 = {-1, 1, 1};
        solver.addGreaterThanConstraint(constraintVars12, constraintCoef12, 0);

        return solver;
    }

    public void printInferenceVariables(HashMap<String, List<InferenceVariable> > varsCache) {
        for (List<InferenceVariable> varsAtId: varsCache.values()) {
            for (InferenceVariable v: varsAtId) {
                System.out.println(v);
            }
        }
    }

    public Solution ILPinference(Tree tree, HashMap<String,List<Instance>> byAuthor,
                                 boolean authorLevel)
    {
        //System.out.println(tree);
        Solution s = null;
        ILPSolver solver = new GurobiHook();
        solver.setMaximize(true);

        try {
            // Map of edge id and list of variables for each edge, one corresponding to each edge type
            HashMap<String, List<InferenceVariable> > varsCache = new HashMap<>();

            // Add all variables and simple cardinality constraints
            // for each edge and author
            for (String head: tree.adjacencyList.keySet()) {
                Instance headOpinion = tree.nodes.get(head);
                // variables
                String headAuthor = ((Opinion)headOpinion).author;
                int headStance = ((Opinion)headOpinion).stance;
                solver = addAuthorVariables(headStance, headAuthor,
                                            byAuthor.get(headAuthor), varsCache, solver);

                for (String tail: tree.adjacencyList.get(head)) {
                    Instance tailOpinion = tree.nodes.get(tail);
                    // variables
                    String tailAuthor = ((Opinion)tailOpinion).author;
                    int tailStance = ((Opinion)tailOpinion).stance;
                    solver = addAuthorVariables(tailStance, tailAuthor,
                                                byAuthor.get(tailAuthor), varsCache, solver);

                    solver = addEdgeVariables(tree, headOpinion, tailOpinion, head, tail, tree.topic,
                                              varsCache, solver, byAuthor, authorLevel);
                    // constraints
                    solver = addCardinalityEdgeConstraints(head, tail, tree.topic,
                                                           varsCache, solver);
                    solver = addConsistencyAuthorConstraints(headOpinion, tailOpinion,
                                                             head, tail, tree.topic,
                                                             varsCache, solver);
                }

            }

            // printing for debugging
            // printInferenceVariables(varsCache);
            // System.exit(1);

            // more constraints
            solver = addConsistencyEdgeConstraints(tree, varsCache, solver);

            if (solver.solve()) {
                List<InferenceVariable> solutionVars = new ArrayList<>();
                for (List<InferenceVariable> varsAtId: varsCache.values()) {
                    for (InferenceVariable v: varsAtId) {
                        if (solver.getBooleanValue(v.ilpId)) {
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


    public void train(List<Tree> dataset, HashMap<String, List<Instance>> byAuthor,
                      boolean authorLevel) {
        this.edgeWeights = new ArrayList<>();
        this.authorWeights = new ArrayList<>();
        this.edgeBias = new ArrayList<>();
        this.authorBias = new ArrayList<>();

        // ugly hack, remember !
        if (authorLevel)
            initWeights(fe.numAuthorLevelEdgeFeatures, 4, this.edgeWeights, this.edgeBias);
        else
            initWeights(fe.numOpinionLevelEdgeFeatures, 4, this.edgeWeights, this.edgeBias);
        System.out.println(this.edgeWeights.get(0).size());
        //System.exit(1);

        for (int i = 0; i < maxIter; i++) {

            int numMistakes = 0;
            int total = 0;
            for (Tree t: dataset) {
                Solution s = ILPinference(t, byAuthor, authorLevel);

                // update rule
                for (InferenceVariable v: s.solution) {

                    if (v instanceof EdgeVariable) {
                        EdgeVariable ev = (EdgeVariable) v;
                        Opinion head = (Opinion) ev.headOpinion;
                        Opinion tail = (Opinion) ev.tailOpinion;
                        int goldenEdge = getEdge(head.stance, tail.stance);
                        if (ev.edgeTypeIndex != goldenEdge) {
                            numMistakes += 1;
                            total += 1;
                            updateWeights(ev.edgeTypeIndex, goldenEdge, ev.features,
                                          this.edgeWeights, this.edgeBias);
                        } else {
                            total += 1;
                        }
                    } /*else if (v instanceof AuthorVariable) {
                        AuthorVariable av = (AuthorVariable) v;
                        if (av.goldenStance != av.stance) {
                            updateWeights(av.stance, av.goldenStance, av.features,
                                          this.authorWeights, this.authorBias);
                        }
                    }*/
                }
            }

            double trainAccuracy = 1 - ((1.0*numMistakes)/total);
            System.out.println("Training accuracy: " + trainAccuracy);
            if (numMistakes == 0)
                return;

        }
    }

    public void test(List<Tree> dataset, HashMap<String, List<Instance>> byAuthor, boolean authorLevel) {
        int numMistakes = 0;
        HashMap<String, Pair> predictions = new HashMap<>();
        for (Tree t: dataset) {
            Solution s = ILPinference(t, byAuthor, authorLevel);

            for (InferenceVariable v: s.solution) {

                if (v instanceof EdgeVariable) {
                    EdgeVariable ev = (EdgeVariable) v;
                    Opinion head = (Opinion) ev.headOpinion;
                    Opinion tail = (Opinion) ev.tailOpinion;
                    int headStance = getHeadStance(ev.edgeTypeIndex);
                    int tailStance = getTailStance(ev.edgeTypeIndex);

                    Pair pHead = new Pair(head.stance, headStance);
                    Pair pTail = new Pair(tail.stance, tailStance);

                    String headId = head.topic+head.debate+head.id;
                    String tailId = tail.topic+tail.debate+tail.id;
                    predictions.put(headId, pHead);
                    predictions.put(tailId, pTail);
                }

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

abstract class InferenceVariable {
    int ilpId;
    double score;
    String customId;
    String descriptor;
    //HashMap<Integer, Float> features;

    public InferenceVariable(int ilpId, double score, String customId,
                             String desc) {
        this.ilpId = ilpId;
        this.score = score;
        this.customId = customId;
        this.descriptor = desc;
       // this.features = feats;
    }

    @Override
    public String toString() {
        return descriptor;
    }
}

class AuthorVariable extends InferenceVariable {

    int stance;
    int goldenStance;

    public AuthorVariable(int id, double score,
                          String authorId, int stance,
                          String desc,
                          int goldenStance) {

        super(id, score, authorId, desc);
        this.stance = stance;
        this.goldenStance = goldenStance;
    }

}

class EdgeVariable extends InferenceVariable {

    int edgeTypeIndex;
    Instance headOpinion;
    Instance tailOpinion;
    HashMap<Integer, Float> features;


    public EdgeVariable(int id, double score, String edgeId,
                        int edgeTypeIndex, String desc,
                        Instance headOpinion,
                        Instance tailOpinion,
                        HashMap<Integer, Float> edgeFeats) {

        super(id, score, edgeId, desc);

        this.edgeTypeIndex = edgeTypeIndex;
        this.headOpinion = headOpinion;
        this.tailOpinion = tailOpinion;
        this.features = edgeFeats;
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
