package co.poynt.postmaster.model;

import co.poynt.postman.model.PostmanFolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostmasterCollection {
	public String id;
	public String name;
	public String description;
	public List<PostmanFolder> folders;  //ordered
	public Long timestamp;
	public Boolean synced;
	public List<PostmasterRequest> requests; //ordered

	public Map<String, PostmasterRequest> requestLookup = new HashMap<>();
	public Map<String, PostmanFolder> folderLookup = new HashMap<>();

	public void init() {
		for (PostmasterRequest r : requests) {
			requestLookup.put(r.id, r);
		}
		for (PostmanFolder f : folders) {
			folderLookup.put(f.name, f);
		}
	}
}
