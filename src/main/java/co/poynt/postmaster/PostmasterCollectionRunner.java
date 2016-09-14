package co.poynt.postmaster;

import co.poynt.postmaster.listener.ApiListener;
import co.poynt.postmaster.listener.KafkaListener;
import co.poynt.postmaster.model.PostmasterCollection;
import co.poynt.postmaster.model.PostmasterRequest;
import co.poynt.postmaster.request.PostmasterRequestRunner;
import co.poynt.postman.PostmanRunResult;
import co.poynt.postman.model.PostmanEnvironment;
import co.poynt.postman.model.PostmanFolder;
import co.poynt.postman.model.PostmanVariables;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostmasterCollectionRunner {

	private static final String ARG_COLLECTION = "c";
	private static final String ARG_ENVIRONMENT = "e";
	private static final String ARG_FOLDER = "f";
	private static final String ARG_HALTONERROR = "haltonerror";

	/**
	 * Main entry point for the Postmaster runner to run the collection file
	 * with environment settings
	 *
	 * @param colFilename the collection filename
	 * @param envFilename the environment filename
	 * @param folderName optional folder name to run
	 * @param haltOnError set to true to halt on error
	 * @return the result object
	 * @throws Exception
	 */
	public PostmanRunResult runCollection(String colFilename, String envFilename, String folderName,
	                                      boolean haltOnError) throws Exception {

		// read collection and environment files
		PostmasterReader reader = new PostmasterReader();
		PostmasterCollection c = reader.readCollectionFile(colFilename);
		c.init();
		PostmanEnvironment e = reader.readEnvironmentFile(envFilename);
		e.init();

		// check for folder
		PostmanFolder folder = null;
		if (folderName != null && !folderName.isEmpty()) {
			folder = c.folderLookup.get(folderName);
		}

		// execute all folders
		PostmanVariables var = new PostmanVariables(e);
		PostmasterRequestRunner runner = new PostmasterRequestRunner(var, haltOnError);
		PostmanRunResult runResult = new PostmanRunResult();
		if (folder != null) {
			executeFolder(haltOnError, runner, var, c, folder, runResult);
		} else {
			// execute all folder all requests
			boolean isSuccessful = true;
			for (PostmanFolder pf : c.folders) {
				isSuccessful = executeFolder(haltOnError, runner, var, c, pf, runResult) && isSuccessful;
				if (haltOnError && !isSuccessful) {
					return runResult;
				}
			}
		}

		System.out.println("@@@@@ Yay! All Done!");
		System.out.println(runResult);
		return runResult;
	}

	private boolean executeFolder(boolean haltOnError, PostmasterRequestRunner runner, PostmanVariables var,
	                              PostmasterCollection c, PostmanFolder folder, PostmanRunResult runResult) {
		System.out.println("==> POSTMASTER Folder: " + folder.name);
		prepareListeners(runner, var, c, folder);
		boolean isSuccessful = runFolder(haltOnError, runner, c, folder, runResult);
		runner.cleanListeners();
		return isSuccessful;
	}

	private void prepareListeners(PostmasterRequestRunner runner, PostmanVariables var, PostmasterCollection c,
	                              PostmanFolder folder) {
		Set<String> topics = new HashSet<>();
		List<String> apiUrls = new ArrayList<>();

		// scan for listeners
		for (String reqId : folder.order) {
			PostmasterRequest r = c.requestLookup.get(reqId);
			switch (r.type) {
				case KAFKA_LISTEN:
					topics.add(r.topic);
					break;
				case API_LISTEN:
					apiUrls.add(r.url);
					break;
				default:
					// do nothing
			}
		}

		// add kafka listener
		if (!topics.isEmpty()) {
			String kafka = var.getEnv().lookup.get("kafka").value;
			runner.addKafkaListener(new KafkaListener(kafka, topics));
		}

		// add api listener
		if (!apiUrls.isEmpty()) {
			runner.addApiListener(new ApiListener(apiUrls));
		}
	}

	private boolean runFolder(boolean haltOnError, PostmasterRequestRunner runner, PostmasterCollection c,
	                          PostmanFolder folder, PostmanRunResult runResult) {
		boolean isSuccessful = true;
		for (String reqId : folder.order) {
			runResult.totalRequest++;
			PostmasterRequest r = c.requestLookup.get(reqId);
			System.out.println("======> POSTMASTER request: " + r.name);
			try {
				boolean runSuccess = runner.run(r, runResult);
				if (!runSuccess) {
					runResult.failedRequest++;
					runResult.failedRequestName.add(folder.name + "." + r.name);
				}
				isSuccessful = runSuccess && isSuccessful;
				if (haltOnError && !isSuccessful) {
					return isSuccessful;
				}
			} catch (Throwable e) {
				e.printStackTrace();
				runResult.failedRequest++;
				runResult.failedRequestName.add(folder.name + "." + r.name);
				return false;
			}
		}
		return isSuccessful;
	}

	/**
	 * Running Postmaster runner from main
	 */
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(ARG_COLLECTION, true, "File name of the Postmaster collection.");
		options.addOption(ARG_ENVIRONMENT, true, "File name of the Postmaster environment variables.");
		options.addOption(ARG_FOLDER, true, "(Optional) Postmaster collection folder (group) to execute i.e. \"My Use Cases\"");
		options.addOption(ARG_HALTONERROR, false, "(Optional) Stop on first error in Postmaster folder.");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		String colFilename = cmd.getOptionValue(ARG_COLLECTION);
		String envFilename = cmd.getOptionValue(ARG_ENVIRONMENT);
		String folderName = cmd.getOptionValue(ARG_FOLDER);
		boolean haltOnError = cmd.hasOption(ARG_HALTONERROR);

		if (colFilename == null || colFilename.isEmpty() || envFilename == null || envFilename.isEmpty()) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("postmaster-runner", options);
			return;
		}

		PostmasterCollectionRunner pcr = new PostmasterCollectionRunner();
		PostmanRunResult result = pcr.runCollection(colFilename, envFilename, folderName, haltOnError);

		// check for failed request
		if (result.failedRequest > 0) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
}
