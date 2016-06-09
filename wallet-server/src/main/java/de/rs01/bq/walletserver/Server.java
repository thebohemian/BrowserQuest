package de.rs01.bq.walletserver;

import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

public class Server {

	Configuration configuration;
	
	Wallet wallet;
	
	Server(Configuration configuration) {
		this.configuration = configuration;

		try {
			wallet = Utils.recreateWallet(configuration);
		} catch (UnreadableWalletException e) {
			throw new RuntimeException("Unable to set up wallet.");
		}
	}
		
}
