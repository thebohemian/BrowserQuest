package de.rs01.bq.walletserver;

import java.io.File;
import java.io.IOException;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import com.google.common.base.Joiner;

public final class Utils {

	private Utils() {
		
	}
	
	static void createWalletAndConfiguration(String fileName, String prefix) throws IOException {
		File f = new File(fileName);
		// TODO: Get params from somewhere
		Wallet w = new Wallet(TestNet3Params.get());
		DeterministicSeed seed = w.getKeyChainSeed();
		
		Configuration c = Configuration.createEmptyConfiguration();
		
		c.setWalletSeedWords(Joiner.on(" ").join(seed.getMnemonicCode()));
		c.setWalletCreationTimeSeconds(seed.getCreationTimeSeconds());
		c.setWalletNetworkId(w.getNetworkParameters().getId());
		c.setWalletDirectory(f.getParent());
		c.setWalletPrefix(prefix);
		
		c.saveToFile(f);
	}
	
	static Wallet recreateWallet(Configuration configuration) throws UnreadableWalletException {
		DeterministicSeed seed = new DeterministicSeed(configuration.getWalletSeedWords(), null, "", configuration.getWalletCreationTimeSeconds());
		
		NetworkParameters params = NetworkParameters.fromID(configuration.getWalletNetworkId());
		Wallet restoredWallet = Wallet.fromSeed(params, seed);
		
		return restoredWallet;

	}
}
