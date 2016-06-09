package de.rs01.bq.walletserver;

import java.io.File;

public class Main {
	
	private static final String RUN = "run";
	private static final String CREATE_WALLET = "create-wallet";

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Missing arguments");
			System.exit(-1);
		}
		
		Server server;
		
		switch(args[0]) {
			case RUN:
				server = new Server(Configuration.loadFromFile(new File(args[1])));
				System.exit(0);
				break;
			case CREATE_WALLET:
				Utils.createWalletAndConfiguration(args[1]);
				System.exit(0);
				break;
			default:
				System.out.println("Unknown command: " + args[0] + ". Exiting.");
				System.exit(-1);
				break;
		}
	}
	
}
