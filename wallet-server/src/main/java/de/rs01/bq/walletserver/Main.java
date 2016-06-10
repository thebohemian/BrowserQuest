package de.rs01.bq.walletserver;

import java.io.File;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.BriefLogFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static final String RUN = "run";
	private static final String CREATE_WALLET = "create-wallet";

	public static void main(final String[] args) throws Exception {
		BriefLogFormatter.init();

		if (args.length < 1) {
			System.out.println("Missing arguments");
			System.exit(-1);
		}

		switch (args[0]) {
		case RUN:
			new Main(args[1]);
			break;
		case CREATE_WALLET:
			Utils.createWalletAndConfiguration(args[1], args[2]);
			System.exit(0);
			break;
		default:
			System.out.println("Unknown command: " + args[0] + ". Exiting.");
			System.exit(-1);
			break;
		}
	}

	private Main(String configFileName) throws Exception {
		File configFile = new File(configFileName);
		ServerConfiguration configuration = ServerConfiguration.loadFromFile(configFile);
		logger.info("successfully loaded configuration: " + configFile);
		
		final BitcoinJBackend backend = new BitcoinJBackend(configuration);
		final SocketIOFrontend frontend = new SocketIOFrontend(configuration);
		
		backend.addListener(new BitcoinJBackend.Listener() {
			
			@Override
			public void coinsReceived(Address receivingAddress, Coin amount) {
				logger.info("[{}] - received coins: {}", receivingAddress, amount);
				frontend.sendPaymentArrived(receivingAddress.toBase58(), amount.longValue());
			}

			@Override
			public void coinsSent(String id, Address receivingAddress, Coin amount) {
				logger.info("[{}] - sent coins: {}", receivingAddress, amount);
				frontend.sendCashoutExecuted(id, receivingAddress.toBase58(), amount.longValue());
			}
		});
		
		frontend.addListener(new SocketIOFrontend.Listener() {
			
			@Override
			public void connected() {
				Coin coin = backend.getWalletBalance();
				logger.info("client connected. sending wallet balance: " + coin);
				frontend.sendWalletBalance(coin.longValue());
			}
			
			@Override
			public void registrationInvoiceRequested(long playerId) {
				logger.info("received invoice request for playerId: " + playerId);
				String addr = backend.createRegistrationInvoice().toBase58();
				
				frontend.sendRegistrationInvoice(playerId, addr);
			}

			@Override
			public void cashoutRequested(String id, String address, long amount) {
				logger.info("received cashout request to address: {} of {} satoshi", address, amount);
				
				Address addr = Address.fromBase58(null, address);
				Coin c = Coin.valueOf(amount);
				backend.sendCoins(id, addr, c);
				
				frontend.sendWalletBalance(backend.getWalletBalance().longValue());
			}
			
		});
		
		logger.info("configured frontend and backend");
		
		backend.start();
		logger.info("backend started");

		frontend.start();
		logger.info("frontend started");
		
		logger.info("making BeanShell available");
		Utils.startBeanShell(frontend, backend);
	}
}
