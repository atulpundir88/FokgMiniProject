# Classifying data using CELOE Algorithm (DLLearner)

# Description

Our project uses the CELOE algorithm which generates the class expressions and these expressions are used for classifying individuals based on the knowledge base. From each learning problem, we provide positive individuals to the algorithm, and it sees what it fits for the class expressions. Since the reasoner is based on a closed world assumption, the negative examples need not be specified explicitly. The Project is executed to generate positive and negative individuals for both training and grading data, which are then stored in different files. Furthermore, these stored files are given as input and the class expression then generated classifies individuals accordingly. Finally, we store these results in an output file.


# Process

Step 1-**Getting data from a given TTL file and generating lp files for positive and negative examples** using class : **ReadTTLDataUsingOWLAPI**
1. Parsing the given training & grading files.
2. Getting individuals using includesResource and excludesResource properties from each learning problem.
3. Save the generated individuals in a directory. 
<br/>Example : 
<br/>training files - lpfiles/train/lp_1_p.txt, lpfiles/train/lp_1_n.txt, etc. 
<br/>grade files - lpfiles/grade/lp_26_p.txt, lpfiles/grade/lp_26_n.txt, etc.

Step 2: **Classify data using CELOE and generate results** using class : **DLLearnerCELOE**
1. Initializing the CELOE algorithm.
2. Getting the individuals from lp files generated in step 1.
3. Classify data based on class expression generated by the algorithm.
4. Generated results are stored in a directory for both training and grading data.

# Execution:

Step 1: Configuring parameters in the config.properties file. 
<br/>(Note:-These parameters are already updated. They can be changed for different datasets or for different learning problems.)
<br/>Following parameters are specified in the config.properties file:
|Parameter name| Parameter Value| Description of the parameter|
|--------------|----------------|-----------------------------|
|fokg.train.filename|kg-mini-project-train_v2.ttl.txt|file name of the training data|
|fokg.train.filepath|lpfiles/train/|file path for LP files generated for training data |
|fokg.train.startlpnumber|1|first lp number for training data|
|fokg.train.totallps|25|total number of LPs for training data|
|fokg.train.execute|true or false|flag to execute algorithm for training data|
|fokg.train.output.filedetail|output/trainresult.ttl|output file for training data|
|fokg.grade.filename|kg-mini-project-grading.ttl.txt|file name of grading data|
|fokg.grade.filepath|lpfiles/grade/|file path for LP files generated for grading data|
|fokg.grade.startlpnumber|1|first lp number for grading data|
|fokg.grade.totallps|25|total number of LPs for grading data|
|fokg.grade.output.filedetail|output/trainresult.ttl|output file for grading data|


Step 2: Execute ReadTTLDataUsingOWLAPI.java class. This class will generate the LP files for training data under lpfiles/train path and for grading data under lpfiles/grade path (The lpfiles are already existing in the project directory).

Step 3: Execute DLLearnerCELOE.java class. This class will invoke the CELOE algorithm for each lp of training (if fokg.train.execute flag is true) and grading data. After the execution is completed, an output file will be created under the output directory.
