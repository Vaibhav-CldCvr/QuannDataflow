package com.clcvr.quann.dataflow;

import java.io.IOException;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.joda.time.Duration;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;	
import org.apache.beam.sdk.transforms.windowing.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cloudcover-vaibhav
 *
 */
public class PubSubGcsDataflow {
    
	private static final Logger LOG = LoggerFactory.getLogger(PubSubGcsDataflow.class);
	static final int WINDOW_SIZE = 1;

	/**
	 * Options supported by {@link PubSubGcsDataflow}.
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
		@Description("Path of the file to write to")
		@Required
		@Default.String("gs://log-agg/output")
		String getOutput();
		void setOutput(String value);

		@Description("Fixed window duration, in minutes")
		@Default.Integer(WINDOW_SIZE)
		Integer getWindowSize();
		void setWindowSize(Integer value);

	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
		Pipeline pipeline = Pipeline.create(options);


		/**
		 * Read logs coming from Cloud Pub Sub 
		 */
		pipeline.apply("Read From Pubsub",PubsubIO.readStrings().fromTopic("projects/cloudcover-sandbox/topics/inputTopic"))
		
		/**
		 * Window into fixed windows. The fixed window size for this example defaults to 1
		 * minute.
		 */
		.apply(Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))
		
		/**
		 * Write log files to Google Cloud Storage bucket
		 */		
		.apply("Write to Bucket",TextIO.write().to(options.getOutput()).
				withWindowedWrites().withNumShards(10));
		
		
		pipeline.run();	


	}

}
