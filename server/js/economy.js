var Messages = require("./message");
var cls = require("./lib/class");
var fs = require("fs");
var _ = require('underscore');

module.exports = Economy = Class.extend({

  init: function(walletServerClient, registeredPlayersFile) {
    var self = this;

    this.minimumRequiredSatoshis = 500000;

    this.walletServerClient = walletServerClient;
    this.walletServerClient.setWalletBalanceCallback(function() {
      self.receiveWalletBalance.apply(self, arguments);
    });
    this.walletServerClient.setRegistrationInvoiceResponseCallback(function() {
      self.receiveRegistrationInvoiceResponse.apply(self, arguments);
    });
    this.walletServerClient.setPaymentArrivedCallback(function() {
      self.receivePaymentArrived.apply(self, arguments);
    });

    this.pendingRegistrations = { };

    this.registeredPlayers = JSON.parse(fs.readFileSync(registeredPlayersFile, "utf8"));


    setInterval(function() {
      fs.writeFile(registeredPlayersFile, JSON.stringify(self.registeredPlayers), "utf8", (err) => {
          if (err) {
            log.warn("Error saving player database.");
          } else {
            log.info("Player database saved.");
          }
        })
      }, 30 * 1000);

  },

  _tellBalance : function(b, callbackOrNull) {
    setTimeout(function() {
      callbackOrNull && callbackOrNull(b);
    }, 500);
  },

  generateRegistrationInvoice: function(player) {
    if (!this.pendingRegistrations[player.id]) {
      log.debug('attempting registration for player: ' + player.id);

      var registration = this.pendingRegistrations[player.id] = {
        player: player,
        address: null,
        balance: 0,
      };

      this.walletServerClient.requestRegistrationInvoice(player.id);
    } else {
      // Registration cannot take place.
      player.send(new Messages.RegisterPlayerInvoice(false, "", 0, "").serialize());
    }

  },

  receiveWalletBalance: function(walletBalance) {
    log.info("game wallet contains: " + walletBalance + " satoshis");
  },

  receiveRegistrationInvoiceResponse: function(playerId, address) {
    log.debug("received registration invoice response: " + playerId + " - " + address);

    var registration = this.pendingRegistrations[playerId];
    if (registration) {
      var player = registration.player;

      var amount = 0.005;
      var label = "BrowserQuest";

      registration.address = address;

      player.send(new Messages.RegisterPlayerInvoice(true, address, amount, label).serialize());
    } else {
        log.warn('received registration invoice response for player that has no pending registration: ' + playerId);
    }
  },

  receivePaymentArrived: function(address, amount) {
    log.debug("received payment arrived: " + address + " - " + amount);

    var pendingRegistration = _.find(this.pendingRegistrations,
      (e) => { return e.address === address; });

    var registrationId = "invalid";
    var initialBalance = 0;

    if (pendingRegistration) {
      pendingRegistration.balance += amount;

      var succeeded = (pendingRegistration.balance >= this.minimumRequiredSatoshis);

      var player = pendingRegistration.player;

      if (succeeded) {
        registrationId = "foobar" + address; // TODO: Use hash function instead
        initialBalance = (pendingRegistration.balance / 1000);
        delete this.pendingRegistrations[player.id];
      }

      this.handleRegistration(player, address, succeeded, registrationId, initialBalance);

    } else {
      // TODO: Try to send money to player

      log.warn('Payment arrived but no corresponding pending registration found!');
    }
  },

  handleRegistration : function (player, address, succeeded, registrationId, initialBalance) {
    if (succeeded) {
      this.registeredPlayers[registrationId] = {
        address: address,
        balance: initialBalance,
      };

      player.registrationId = registrationId;

      // Make player receive the current balance
      player.updateTreasureBalance();
    }

    player.send(new Messages.RegisterPlayerResponse(succeeded, registrationId).serialize());
  },

  maybeMakeTreasure : function() {
    // TODO: provide logic to make a treasure
  },

  checkBalance : function(player, callbackOrNull) {
    if (!player.registrationId) {
      this._tellBalance(-1, callbackOrNull);
      return;
    }

    var registeredPlayer = this.registeredPlayers[player.registrationId];
    if (registeredPlayer) {
      var balance = registeredPlayer.balance;

      this._tellBalance(balance, callbackOrNull);
    } else {
      log.debug('Did not find registered player for id:' + player.registrationId);
      delete player.registrationId;

      this._tellBalance(-1, callbackOrNull);
    }

  },

  giveTreasure : function(player, treasureId, callbackOrNull) {
    var self = this;

    if (player.registrationId) {
      var registeredPlayer = this.registeredPlayers[player.registrationId];
      if (registeredPlayer) {
        // TODO: Lookup treasure
        // TODO: Add treasure value to player balance
        registeredPlayer.balance++;

        this._tellBalance(registeredPlayer.balance, callbackOrNull);
      } else {
        log.debug('Did not find registered player for id:' + player.registrationId);
      }
    } else {
      log.debug('player is not registered. Treasure lost.');
    }

  }

});
