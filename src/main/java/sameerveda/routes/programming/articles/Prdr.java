package sameerveda.routes.programming.articles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;

import programming.articles.app.providers.DefaultProviders;
import sam.collection2.NoopMap;
import sam.full.access.dynamodb.DynamoConnection;
import sam.myutils.System2;

public class Prdr extends DefaultProviders {
	
	@Override
	protected DynamoConnection openConnection() throws FileNotFoundException, IllegalArgumentException, IOException {
		String f = System2.lookup("dynamo-creds");
		if(f != null) {
			System.out.println("dynamo-creds: "+f);
			return new DynamoConnection(new AWSStaticCredentialsProvider(new PropertiesCredentials(new File(f))));
		} else 
			return new DynamoConnection();
	}

	public Prdr() throws Exception {
		super();
	}

	@Override
	protected <E extends Comparable<?>, F> Map<E, F> cacheFor(String s) {
		return s.equals(CACHE_NAME_ICONS) ? null : new NoopMap<>();
	}
	

	@Override
	protected void cacheInit() throws IOException { }

	@Override
	protected Path loadedMetasPath() {
		return null;
	}

}
