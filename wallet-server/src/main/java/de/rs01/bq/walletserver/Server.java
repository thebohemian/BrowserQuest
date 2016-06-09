package de.rs01.bq.walletserver;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;

public class Server {
	
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	Configuration configuration;

	Wallet wallet;

	WalletAppKit kit;

	Server(Configuration configuration) {
		this.configuration = configuration;

		try {
			wallet = Utils.recreateWallet(configuration);
		} catch (UnreadableWalletException e) {
			throw new RuntimeException("Unable to set up wallet.");
		}

		kit = new WalletAppKit(wallet.getNetworkParameters(), new File(configuration.getWalletDirectory()),
				configuration.getWalletPrefix()) {
			
			@Override
			protected Wallet createWallet() {
				return wallet;
			}

			@Override
			protected PeerGroup createPeerGroup() throws TimeoutException {
				PeerGroup pg = super.createPeerGroup();
				pg.setPeerDiscoveryTimeoutMillis(10 * 1000);
				pg.setConnectTimeoutMillis(10 * 1000);
				
				return pg; 
			}
			
			
		};
		
		wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
			
			@Override
			public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
				
		        for (TransactionOutput o : tx.getOutputs()) {
		            if (o.isMine(wallet)) {
		            	Address receivingAddress = o.getAddressFromP2PKHScript(wallet.getNetworkParameters());
		            	Coin coin = o.getValue();
		            	
		            	if (receivingAddress != null) {
		            		notifyCoinsReceived(receivingAddress, coin);
		            	} else {
		            		logger.warn("Receiving address was not a public key hash: " + o);
		            	}
		            }
		        }

			}
			
		});
		
		kit.startAsync();
		
		kit.awaitRunning();
	}
	
	private void notifyCoinsReceived(Address address, Coin coin) {
		logger.info("[%s] - received coins: %s", address, coin);
	}

	Address createRegistrationInvoice() {
		Address address = wallet.freshReceiveAddress();
		
		logger.info("[%s] - created new receiving address", address);
		
		return address;
	}
}
