package de.rs01.bqw;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.Wallet;

public class Test {
	
	Test() {
		System.out.println("Test!");

		NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
		Wallet w = new Wallet(params);

		System.out.println("w: " + w);
	}

	public static void main(String[] args) {
		new Test();
	}

}
