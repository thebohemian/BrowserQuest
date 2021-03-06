package de.rs01.bq.walletserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class ServerConfiguration {
	
	private JsonObject map;
	
	private ServerConfiguration(JsonObject map) {
		this.map = map;
	}
	
	static ServerConfiguration loadFromFile(File f) throws IOException {
		Gson g = new Gson();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(f));

			return new ServerConfiguration((JsonObject) g.fromJson(reader, JsonObject.class));
		} finally {
			IOUtils.closeQuietly(reader);
		}
		
	}
	
	static ServerConfiguration createEmptyConfiguration() {
		return new ServerConfiguration(new JsonObject());
	}
	
	void saveToFile(File f) throws IOException {
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		
		JsonWriter writer = null;
		try {
			writer = g.newJsonWriter(new FileWriter(f));
			
			g.toJson(map, writer);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	void setWalletSeedWords(String seedWords) {
		map.addProperty("walletSeedWords", seedWords);
	}
	
	String getWalletSeedWords() {
		return map.get("walletSeedWords").getAsString();
	}
	
	void setWalletCreationTimeSeconds(long creationTimeSecs) {
		map.addProperty("walletCreationTimeSeconds", creationTimeSecs);
	}
	
	long getWalletCreationTimeSeconds() {
		return map.get("walletCreationTimeSeconds").getAsLong();
	}
	
	void setWalletNetworkId(String networkId) {
		map.addProperty("walletNetworkId", networkId);
	}
	
	String getWalletNetworkId() {
		return map.get("walletNetworkId").getAsString();
	}
	
	void setWalletDirectory(String dir) {
		map.addProperty("walletDirectory", dir);
	}
	
	String getWalletDirectory() {
		return map.get("walletDirectory").getAsString();
	}
	
	void setWalletPrefix(String prefix) {
		map.addProperty("walletPrefix", prefix);
	}
	
	String getWalletPrefix() {
		return map.get("walletPrefix").getAsString();
	}
	
	void setServerHostname(String prefix) {
		map.addProperty("serverHostname", prefix);
	}
	
	String getServerHostname() {
		return map.get("serverHostname").getAsString();
	}

	void setServerPort(int port) {
		map.addProperty("serverPort", port);
	}
	
	int getServerPort() {
		return map.get("serverPort").getAsInt();
	}

}
