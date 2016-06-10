package de.rs01.bq.walletserver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

public class SocketIOFrontend {
	private static final Logger logger = LoggerFactory.getLogger(SocketIOFrontend.class);

	private static final String REGISTRATION_INVOICE_EVENT = "registrationInvoiceEvent";

	private static final String REGISTRATION_INVOICE_RESPONSE = "registrationInvoiceResponse";
	
	private static final String WALLET_BALANCE = "walletBalance";

	private static final String PAYMENT_ARRIVED = "paymentArrived";

	SocketIOServer server;

	List<Listener> listeners = new ArrayList<Listener>();

	public SocketIOFrontend(ServerConfiguration configuration) {

		Configuration config = new Configuration();
		SocketConfig soConfig = new SocketConfig();
		soConfig.setReuseAddress(true);
		config.setSocketConfig(soConfig);
		config.setHostname(configuration.getServerHostname());
		config.setPort(configuration.getServerPort());

		server = new SocketIOServer(config);
		
		server.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(SocketIOClient client) {
				notifyConnected();
			}
		});

		server.addEventListener(REGISTRATION_INVOICE_EVENT, RegistrationInvoiceObject.class,
				new DataListener<RegistrationInvoiceObject>() {

					@Override
					public void onData(SocketIOClient client, RegistrationInvoiceObject data, AckRequest ackRequest) {
						notifyRegistrationInvoice(data);
					}

				});
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.info("Shutting down SocketIO server in shutdown hook!");
				server.stop();
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
		void connected();
		
		void registrationInvoiceRequested(long playerId);
	}

	void start() {
		server.start();
	}

	void stop() {
		server.stop();
	}

	void notifyConnected() {
		for (Listener l : listeners) {
			l.connected();
		}
	}

	void notifyRegistrationInvoice(RegistrationInvoiceObject data) {
		for (Listener l : listeners) {
			l.registrationInvoiceRequested(data.playerId);
		}
	}

	public static class RegistrationInvoiceObject {

		private long playerId;

		public RegistrationInvoiceObject() {
		}

		public RegistrationInvoiceObject(long playerId) {
			this.playerId = playerId;
		}

		public long getPlayerId() {
			return playerId;
		}

		public void setPlayerId(long playerId) {
			this.playerId = playerId;
		}

	}
	
	public static class RegistrationInvoiceResponseObject {

		private long playerId;
		private String address;

		public RegistrationInvoiceResponseObject() {
		}

		public RegistrationInvoiceResponseObject(long playerId, String address) {
			this.playerId = playerId;
			this.address = address;
		}

		public long getPlayerId() {
			return playerId;
		}

		public void setPlayerId(long playerId) {
			this.playerId = playerId;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

	}

	void sendRegistrationInvoice(long playerId, String address) {
		server.getBroadcastOperations().sendEvent(REGISTRATION_INVOICE_RESPONSE,
				new RegistrationInvoiceResponseObject(playerId, address));
	}
	
	public static class WalletBalanceObject {
		
		private String address;
		
		private long balance;
		
		public WalletBalanceObject() {
		}
		
		public WalletBalanceObject(String address, long balanceInSatoshis) {
			this.address = address;
			balance = balanceInSatoshis;
		}
		
		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public long getBalance() {
			return balance;
		}

		public void setBalance(long balance) {
			this.balance = balance;
		}
		
	}
	
	void sendWalletBalance(long satoshis) {
		server.getBroadcastOperations().sendEvent(WALLET_BALANCE,
				new WalletBalanceObject(null, satoshis));
	}

	void sendPaymentArrived(String address, long amount) {
		server.getBroadcastOperations().sendEvent(PAYMENT_ARRIVED,
				new WalletBalanceObject(address, amount));
		
	}
}
