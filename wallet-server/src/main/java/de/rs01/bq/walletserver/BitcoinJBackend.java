package de.rs01.bq.walletserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.Wallet.BalanceType;
import org.bitcoinj.wallet.Wallet.SendResult;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoinJBackend {

	private static final Logger logger = LoggerFactory.getLogger(BitcoinJBackend.class);

	ServerConfiguration configuration;

	WalletAppKit kit;
	
	Executor executor;

	List<Listener> listeners = new ArrayList<Listener>();

	BitcoinJBackend(ServerConfiguration configuration) {
		this.configuration = configuration;

		final Wallet loadedWallet;
		try {
			loadedWallet = Utils.recreateWallet(configuration);
			loadedWallet.allowSpendingUnconfirmedTransactions();
		} catch (UnreadableWalletException e) {
			throw new RuntimeException("Unable to set up wallet.");
		}

		kit = new WalletAppKit(loadedWallet.getNetworkParameters(), new File(configuration.getWalletDirectory()),
				configuration.getWalletPrefix()) {

			@Override
			protected Wallet createWallet() {
				return loadedWallet;
			}

			@Override
			protected void onSetupCompleted() {
				setupListeners();
			}
			
			@Override
			protected Executor executor() {
				return executor = super.executor();
			}

		};
	}

	private void setupListeners() {

		kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {

			@Override
			public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {

				logger.info("coins received: " + tx.getValueSentToMe(wallet));

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
	}

	void addListener(Listener l) {
		listeners.add(l);
	}

	void removeListener(Listener l) {
		listeners.remove(l);
	}

	interface Listener {
		void coinsReceived(Address receivingAddress, Coin amount);
		
		void coinsSent(String id, Address receivingAddress, Coin amount);
	}

	void start() {
		kit.startAsync();

		kit.awaitRunning();
	}

	void stop() {
		kit.stopAsync();
	}

	private void notifyCoinsReceived(Address receivingAddress, Coin amount) {
		for (Listener l : listeners) {
			l.coinsReceived(receivingAddress, amount);
		}
	}
	
	private void notifyCoinsSent(String id, Address receivingAddress, Coin amount) {
		for (Listener l : listeners) {
			l.coinsSent(id, receivingAddress, amount);
		}
	}

	Address createRegistrationInvoice() {
		Address address = kit.wallet().freshReceiveAddress();

		logger.info("[{}] - created new receiving address", address);

		return address;
	}

	Coin getWalletBalance() {
		return kit.wallet().getBalance(BalanceType.ESTIMATED_SPENDABLE);
	}

	void sendCoins(final String id, final Address address, final Coin coin) {
		Coin amountToSend = coin.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
		SendResult sendResult = null;
		
		try {
			sendResult = kit.wallet().sendCoins(kit.peerGroup(), address, amountToSend);
		} catch (InsufficientMoneyException e) {
			logger.warn("[{}] - failed to send {} BTC. Not enough money in wallet: {}", address, amountToSend,
					getWalletBalance());
		}
		
		sendResult.broadcastComplete.addListener(new Runnable() {
		    @Override
		    public void run() {
		    	notifyCoinsSent(id, address, coin);
		    }
		}, executor);
		
	}

}
