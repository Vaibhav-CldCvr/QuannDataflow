package com.clcvr.quann.dataflow;


import java.io.IOException;


import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;

import org.apache.beam.sdk.values.PCollection;



/**
 * @author cloudcover-vaibhav
 *
 */
public class ReadGCSDataflow {
    
	static final int WINDOW_SIZE = 5;
	private static final Logger LOG = LoggerFactory.getLogger(ReadGCSDataflow.class);

	/**
	 * Options supported by {@link ReadGCSDataflow}.
	 *
	 * <p>Defining your own configuration options. Here, you can add your own arguments
	 * to be processed by the command-line parser, and specify default values for them. You can then
	 * access the options values in your pipeline code.
	 *
	 * <p>Inherits standard configuration options.
	 */
	public interface Options extends PipelineOptions{

		/**
		 * Set this required option to specify where to write the output.
		 */
		
		@Description("Write PubSub Topic")
		@Required
		@Default.String("projects/project-lms-182110/topics/gcs-input")
		String getWriteTopicName();
		void setWriteTopicName(String value);
		

	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException , Exception{

		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
		Pipeline pipeline = Pipeline.create(options);	
		
		/**
		 * Read logs coming from Bucket 
		 */
		PCollection<String> inputCollection = pipeline
				.apply("Read CSV from GCS",TextIO.read().from("gs://quann-logs/upload-palo alto/PAN Logs/*.csv"));
		inputCollection.apply("Write PaloAlto Logs to Elk",PubsubIO.writeStrings().to(options.getWriteTopicName()));		
	
		
		pipeline.run();	


	}


}
