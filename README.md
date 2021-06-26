# FokgMiniProject

# Step 1 (Done)
class : ReadTTLDataUsingOWLAPI
1. Parses training file
2. Gets include and exclude resources for each learning problem.
3. Stores this resources in directory lpfiles/<learning_problem>_<positive_negative>.txt

# Step 2 (Need to start)
Have tried to train the model for first 20 LPs, getting following F1-score:
CELOE Algorithm => 49%
OCEL Algorithm => 44%
Note: This is F1-score for first 20 LPs.

We need to work on following task.
1) Cross Validation. (Create a logic to divide training data and use this different combination of data to train the model)
2) Trying different parameters for CELOE & OCEL. (Like changing Noise Percentage, negative weight, etc. )
3) Validate Test data against the class expression(Note we will get this class expression from training data).
4) Using different learning problem classes like PosNegUndLP, etc. (This classes are present under https://github.com/SmartDataAnalytics/DL-Learner/blob/89e6380cd14921ba86205a1437857744fe896c4d/components-core/src/main/java/org/dllearner/learningproblems/)

Note : 
I have tried below changes :
1) Using Refinement operators like PSI-up, PSI-down, ELDown. Getting error when Refine method is invoked. It is not defiend for this operators.
2) Used PCELOE instead of CELOE, getting ConcurrentModification exception message. It is trying to execute the flow in parallel.
3) NaiveALLearner algorithm => with expression length of 4 (Taking a lot of time)
