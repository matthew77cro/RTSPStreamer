package hr.matija.rtpStreamer.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class H264RtspResourceCollection {
	
	private Path resourceDescriptor;
	private Path resourceRoot;
	
	private Set<Resource> resources;
	private Map<Integer, Resource> idResourceMapper;
	private Map<String, Resource> uriResourceMapper;
	
	public H264RtspResourceCollection(Path resourceDescriptor, Path resourceRoot) throws IOException {
		setResourceDescriptor(resourceDescriptor, resourceRoot);
	}
	
	public void loadResources() throws IOException {
		Set<Resource> resources = new HashSet<>();
		Map<Integer, Resource> idResourceMapper = new HashMap<>();
		Map<String, Resource> uriResourceMapper = new HashMap<>();
		
		List<String> lines = Files.readAllLines(resourceDescriptor);
		
		for(String l : lines) {
			if(l.trim().startsWith("#")) continue;
			
			String[] resource = l.trim().split("\t");
			int id = Integer.parseInt(resource[0]);
			String name = resource[1];
			String uri = resource[2];
			int fps = Integer.parseInt(resource[3]);
			
			Path p = null;
			if(!name.startsWith("live://")) p = Paths.get(resourceRoot.toString() + "/" + name);
			Resource res = new Resource(id, name, p, uri, fps);
			resources.add(res);
			if(idResourceMapper.put(id, res) != null) throw new RuntimeException("There exist two resources with same id! This is illegal!");
			if(uriResourceMapper.put(uri, res) != null) throw new RuntimeException("There exist two resources mapped to the same uri! This is illegal!");
		}
		
		this.resources = resources;
		this.idResourceMapper = idResourceMapper;
		this.uriResourceMapper = uriResourceMapper;
	}
	
	public Path getResourceDescriptor() {
		return resourceDescriptor;
	}
	
	public void setResourceDescriptor(Path resourceDescriptor, Path resourceRoot) throws IOException {
		if(!Files.isRegularFile(resourceDescriptor) || !Files.isReadable(resourceDescriptor))
			throw new RuntimeException("resourceDescriptor error!");
		if(!Files.isDirectory(resourceRoot)) throw new RuntimeException("resourceRoot error!");
		
		this.resourceDescriptor = Objects.requireNonNull(resourceDescriptor);
		this.resourceRoot = resourceRoot;
		loadResources();
	}
	
	public Resource getResourceForId(int id) {
		return idResourceMapper.get(id);
	}
	
	public Resource getResourceForUri(String uri) {
		if(uri==null) return null;
		return uriResourceMapper.get(uri);
	}
	
	public Set<Resource> getResources() {
		return resources;
	}
	
	/**
	 * Resource available for streaming
	 * @author Matija
	 *
	 */
	public static class Resource {
		private int id;
		private String name;
		private Path path;
		private String uriMapping;
		private int fps;
		
		public Resource(int id, String name, Path path, String uriMapping, int fps) {
			this.id = id;
			this.name = name;
			this.path = path;
			this.uriMapping = uriMapping;
			this.fps = fps;
		}

		public int getId() {
			return id;
		}

		public Path getPath() {
			return path;
		}
		
		public String getName() {
			return name;
		}
		
		public String getUriMapping() {
			return uriMapping;
		}

		public int getFps() {
			return fps;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fps, id, path, uriMapping);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Resource))
				return false;
			Resource other = (Resource) obj;
			return fps == other.fps && id == other.id && Objects.equals(path, other.path) && Objects.equals(uriMapping, other.uriMapping);
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %s, %s, %d)", id, name, uriMapping, fps);
		}
	}

}
