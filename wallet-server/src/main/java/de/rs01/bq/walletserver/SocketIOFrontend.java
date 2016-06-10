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
	
	private static final String CASHOUT_EVENT = "cashoutEvent";
	
	private static final String CASHOUT_EXECUTED = "cashoutExecuted";

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

		server.addEventListener(REGISTRATION_INVOICE_EVENT, PlayerPaymentAddressObject.class,
				new DataListener<PlayerPaymentAddressObject>() {

					@Override
					public void onData(SocketIOClient client, PlayerPaymentAddressObject data, AckRequest ackRequest) {
						notifyRegistrationInvoice(data);
					}

				});

		server.addEventListener(CASHOUT_EVENT, PaymentObject.class,
				new DataListener<PaymentObject>() {

					@Override
					public void onData(SocketIOClient client, PaymentObject data, AckRequest ackRequest) {
						notifyCashoutRequested(data);
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
		
		void cashoutRequested(String id, String address, long amount);
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

	void notifyRegistrationInvoice(PlayerPaymentAddressObject data) {
		for (Listener l : listeners) {
			l.registrationInvoiceRequested(data.playerId);
		}
	}
	
	void notifyCashoutRequested(PaymentObject data) {
		for (Listener l : listeners) {
			l.cashoutRequested(data.id, data.address, data.amount);
		}
	}
	
	public static class PlayerPaymentAddressObject {

		private long playerId;
		private String address;

		public PlayerPaymentAddressObject() {
		}

		public PlayerPaymentAddressObject(long playerId, String address) {
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
				new PlayerPaymentAddressObject(playerId, address));
	}
	
	public static class PaymentObject {
		
		private String id;
		
		private String address;
		
		private long amount;
		
		public PaymentObject() {
		}
		
		public PaymentObject(String id, String address, long amountInSatoshis) {
			this.id = id;
			this.address = address;
			amount = amountInSatoshis;
		}
		
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public long getAmount() {
			return amount;
		}

		public void setAmount(long balance) {
			this.amount = balance;
		}
		
	}
	
	void sendWalletBalance(long satoshis) {
		server.getBroadcastOperations().sendEvent(WALLET_BALANCE,
				new PaymentObject(null, null, satoshis));
	}

	void sendPaymentArrived(String address, long amount) {
		server.getBroadcastOperations().sendEvent(PAYMENT_ARRIVED,
				new PaymentObject(null, address, amount));
		
	}
	
	void sendCashoutExecuted(String id, String address, long amount) {
		server.getBroadcastOperations().sendEvent(CASHOUT_EXECUTED,
				new PaymentObject(id, address, amount));
		
	}
	
}
