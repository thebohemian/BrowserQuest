package de.rs01.bq.walletserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import bsh.EvalError;
import bsh.Interpreter;

public final class Utils {

	private Utils() {

	}

	static void createWalletAndConfiguration(String fileName, String networkId, String prefix) throws IOException {
		File f = new File(fileName);

		File parent = f.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}

		// TODO: Get params from somewhere
		NetworkParameters networkParameters = null;
		switch (networkId) {
		case "mainnet":
			networkParameters = MainNetParams.get();
			break;
		case "testnet":
			networkParameters = TestNet3Params.get();
			break;
		default:
			throw new IllegalArgumentException("Invalid network id: " + networkId + ". Must be 'testnet' or 'mainnet'.");
		}
		
		Wallet w = new Wallet(networkParameters);
		DeterministicSeed seed = w.getKeyChainSeed();

		ServerConfiguration c = ServerConfiguration.createEmptyConfiguration();

		c.setWalletSeedWords(Joiner.on(" ").join(seed.getMnemonicCode()));
		c.setWalletCreationTimeSeconds(seed.getCreationTimeSeconds());
		c.setWalletNetworkId(w.getNetworkParameters().getId());
		c.setWalletDirectory(f.getParent());
		c.setWalletPrefix(prefix);

		c.saveToFile(f);
	}

	static Wallet recreateWallet(ServerConfiguration configuration) throws UnreadableWalletException {
		DeterministicSeed seed = new DeterministicSeed(configuration.getWalletSeedWords(), null, "",
				configuration.getWalletCreationTimeSeconds());

		NetworkParameters params = NetworkParameters.fromID(configuration.getWalletNetworkId());
		Wallet restoredWallet = Wallet.fromSeed(params, seed);

		return restoredWallet;

	}

	static void runAsDaemon(String name, Runnable r) {
		Thread t = new Thread(r, name);
		t.setDaemon(true);
		t.start();
	}

	static void startBeanShell(Object frontend, Object backend) {

		Interpreter itp = new Interpreter(new InputStreamReader(System.in), System.out, System.err, true);
		// itp.setExitOnEOF(false);

		try {
			itp.set("LF", LoggerFactory.class);
			itp.set("frontend", frontend);
			itp.set("backend", backend);
			itp.eval("show();");
			itp.eval("setAccessibility(true);");
		} catch (EvalError e) {

		}

		itp.run();
	}
}
