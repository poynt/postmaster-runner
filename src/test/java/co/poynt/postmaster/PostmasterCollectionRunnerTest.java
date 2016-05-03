package co.poynt.postmaster;

import org.testng.Assert;
import org.testng.annotations.Test;

import co.poynt.postman.PostmanRunResult;

public class PostmasterCollectionRunnerTest {

	// example to run the postmaster
	@Test(enabled =  false)
	public void runCollection() throws Exception {
		PostmasterCollectionRunner br = new PostmasterCollectionRunner();
		PostmanRunResult runResult = br.runCollection(
				"classpath:sample_postmaster_collection.json",
				"classpath:sample_postmaster_environment.json",
				null,
				false);
		Assert.assertTrue(runResult.isSuccessful());
	}
}
