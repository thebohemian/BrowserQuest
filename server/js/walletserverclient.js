var cls = require("./lib/class");
var io = require('socket.io-client');

module.exports = WalletServerClient = Class.extend({

  init: function(url) {
    this.socket = io.connect(url, { });

    //this.socket.on('connect', function () { console.log("socket connected"); });
    var self = this;

    this.socket.on('connect', function () {
      log.info('connected to wallet server via SocketIO');
    });

    this.socket.on('walletBalance', function(data) {
      var walletBalance = data.balance;

      if (self.walletbalance_callback) {
        self.walletbalance_callback(walletBalance);
      }
    });

    this.socket.on('registrationInvoiceResponse', function(data) {
      var playerId = data.playerId;
      var address = data.address;

      if (self.registrationinvoiceresponse_callback) {
        self.registrationinvoiceresponse_callback(playerId, address);
      }
    });

    this.socket.on('paymentArrived', function(data) {
      var address = data.address;
      var amount = data.balance;

      if (self.paymentarrived_callback) {
        self.paymentarrived_callback(address, amount);
      }
    });
  },

  requestRegistrationInvoice: function(playerId) {
    this.socket.emit('registrationInvoiceEvent', {
      playerId: playerId
    });
  },

  setRegistrationInvoiceResponseCallback: function(callback) {
    this.registrationinvoiceresponse_callback = callback;
  },

  setWalletBalanceCallback: function(callback) {
    this.walletbalance_callback = callback;
  },

  setPaymentArrivedCallback: function(callback) {
    this.paymentarrived_callback = callback;
  },

});
