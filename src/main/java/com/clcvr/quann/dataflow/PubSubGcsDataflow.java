package com.clcvr.quann.dataflow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.repackaged.org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;

import org.apache.beam.sdk.transforms.windowing.FixedWindows;	
import org.apache.beam.sdk.transforms.windowing.Window;



/**
 * @author cloudcover-vaibhav
 *
 */
public class PubSubGcsDataflow {
    
	static final int WINDOW_SIZE = 5;
	private static final Logger LOG = LoggerFactory.getLogger(PubSubGcsDataflow.class);

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
		@Default.String("gs://log-agg/output/")
		String getOutput();
		void setOutput(String value);

		@Description("Fixed window duration, in minutes")
		@Default.Integer(WINDOW_SIZE)
		Integer getWindowSize();
		void setWindowSize(Integer value);

	}

	/**
	 * @param bytes
	 * @throws IOException 
	 */
	public static String decompress(byte[] bytes) throws IOException {
		/**
		 * This method is used to Decompress input PubSub Logs 
		 * to Human Readable format.
		 */
		LOG.info("Found UnCompressed PubSub Logs " + bytes);
		String outStr = "";
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			
			BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bais);
			InputStreamReader reader = new InputStreamReader(bzis);
			BufferedReader in = new BufferedReader(reader);
			String readed;		
			while ((readed = in.readLine()) != null) {
				LOG.info("Found Compressed Logs " + readed);
			    outStr += readed;
			    System.out.println("outStr"+outStr);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return outStr;
		
	}

	static class LogsFn extends DoFn<PubsubMessage, String> {
		/**
		 * This is DoFn function that process one element
		 * at a time from input PCollection
		 */
		private static final long serialVersionUID = 1L;

		@ProcessElement
		public void processElement(ProcessContext c) {
			LOG.info("Found context " + c);
			// Get the input element from ProcessContext.
			PubsubMessage message = c.element();
			// Use ProcessContext.output to emit the output element.
			String part = "";
			try {
				part = PubSubGcsDataflow.decompress(message.getPayload());
			} catch (IOException e) {
				e.printStackTrace();
			}
			c.output(part);
		}
	}


	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException , Exception{

		Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
		Pipeline pipeline = Pipeline.create(options);

		/**
		 * Read logs coming from Cloud Pub Sub 
		 */
		pipeline.apply("Read Logs Pubsub",PubsubIO.readMessages().fromTopic("projects/cloudcover-sandbox/topics/newTopic"))
		/**
		 * Decompress Logs  
		 */
		.apply("Decompressing Logs",ParDo.of(new LogsFn()))
		/**
		 * Window into fixed windows. The fixed window size is 5
		 * minute.
		 */
		.apply("Applying Window",Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowSize()))))

		/**
		 * Write log files to Google Cloud Storage bucket
		 */		
		.apply("Write Logs GCS",TextIO.write().to(options.getOutput()).
		     withWindowedWrites().withNumShards(1));

		pipeline.run();	


	}


}
