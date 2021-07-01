# Classifying data using CELOE

# The project consists of following steps:
Step 1: Read data from TTL file and generate lp files for positive and negative examples using class : ReadTTLDataUsingOWLAPI
1. Parses training file & grading file.
2. Gets include and exclude resources for each learning problem.
3. Stores this resources in directory lpfiles/train/<learning_problem>_<positive_negative>.txt(for train data) or lpfiles/grade/<learning_problem>_<positive_negative>.txt(for grading data)

Step 2: Classify data using CELOE and generate output file using class : DLLearnerCELOE
1. Initializes CELOE algorithm.
2. Gets data for individual LPs from lp files generated in step 1.
3. Classify data using CELOE.
4. Generate output file.

# Execution:

Step 1: Check parameters in properties file. (These parameters are already specified for training and grading data)
Following parameters are specified in the config.properties file:
1.  "fokg.train.filename" - file name containing training data.
2.  "fokg.train.filepath" - file path for generating LP files.
3.  "fokg.train.startlpnumber" - first lp number for training data.
4.  "fokg.train.totallps" - total number of LPs for training data.
5.  "fokg.train.execute" - flag to execute algorithm for training data.
6.  "fokg.train.output.filedetail" - output file path for training data.

Similar parameters exists for grading data : fokg.grade.filename, fokg.grade.filepath, fokg.grade.startlpnumber, fokg.grade.totallps, fokg.grade.output.filedetail

Step 2: Execute ReadTTLDataUsingOWLAPI.java class. This class will generate the LP files for training data under lpfiles/train path and for grading data under lpfiles/grade path.

Step 3: Execute DLLearnerCELOE.java class. This class will invoke CELOE algorithm for each lp of training(if fokg.train.execute flag is true) and grading data. After the execution is completed, output file will be created under output directory.
