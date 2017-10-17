# quannDataflow

Run the project in Eclipse :
  1.  Right Click the project -> Run As -> Run Configurations.
  2.  A pop window will open
  3.  In the Pipeline Arguments specify Runner as DataflowRunner.
  4.  Uncheck the default configuration and pass Account of your GCP(ex : <id>@cloudcover.in) , Project Id (get this from GCP consile)and full path of the staging location (ex : gs://<bucket-name>/staging). 
  4.  Apply the configuration.
  5.  Run the Configuration.

Run the project in command line :

  1.  cd to the project directory.
  2.  mvn compile exec:java -Dexec.mainClass=<path to the main java file location> \
     -Dexec.args="--runner=DataflowRunner --project=<your-gcp-project> \
                  --gcpTempLocation=gs://<your-gcs-bucket>/tmp \
                  --output=gs://<your-gcs-bucket>/output" \
     -Pdataflow-runner
  
  

