var Messages = require("./message");
var cls = require("./lib/class");
var fs = require("fs");
var _ = require('underscore');

module.exports = Economy = cls.Class.extend({

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
    this.walletServerClient.setCashoutExecutedCallback(function() {
      self.receiveCashoutExecuted.apply(self, arguments);
    });

    this.pendingRegistrations = { };

    this.registeredPlayers = JSON.parse(fs.readFileSync(registeredPlayersFile, "utf8"));

    this.onlineRegisteredPlayers = { };

    setInterval(function() {
      fs.writeFile(registeredPlayersFile, JSON.stringify(self.registeredPlayers), "utf8", (err) => {
          if (err) {
            log.warning("Error saving player database.");
          } else {
            log.info("Player database saved.");
          }
        })
      }, 30 * 1000);

  },

  tellOnline: function(player) {
    var reg;
    if (player.registrationId && (reg = this.registeredPlayers[player.registrationId])) {
      this.onlineRegisteredPlayers[player.registrationId] = player;
    }
  },

  tellOffline: function(player) {
    var reg;
    if (player.registrationId && (reg = this.registeredPlayers[player.registrationId])) {
      delete this.onlineRegisteredPlayers[player.registrationId];
    }

  },

  _tellBalance : function(b, callbackOrNull) {
    setTimeout(function() {
      callbackOrNull && callbackOrNull(b);
    }, 500);
  },

  attemptRegistration: function(player, redeemAddress) {
    if (player.registrationId) {
      log.debug('registration for player: ' + player.id + ' not possible. Already registered.');

      // Already registered
      player.send(new Messages.RegisterPlayerInvoice(false, "", 0, "Already registered.").serialize());
    } else if (this.pendingRegistrations[player.id]){
      log.debug('registration for player: ' + player.id + ' not possible. Already pending registration.');

      // Already a pending registration
      player.send(new Messages.RegisterPlayerInvoice(false, "", 0, "Registration already pending.").serialize());
    } else {
      log.debug('attempting registration for player: ' + player.id);

      var registration = this.pendingRegistrations[player.id] = {
        player: player,
        address: null,
        redeemAddress: redeemAddress,
        balance: 0,
      };

      // TODO: Send redeem address for checking it
      this.walletServerClient.requestRegistrationInvoice(player.id);

    }

  },

  cashout: function(player) {
    var reg;
    if (player.registrationId && (reg = this.registeredPlayers[player.registrationId]) && reg.redeemAddress) {
      if (reg.balance > 100) {
        log.info('cashing out 100000 satoshis for player: ' + player.id);

        this.walletServerClient.requestCashout(player.registrationId, reg.redeemAddress, 100 * 1000);
      }
    } else {
      log.warning('cashout request for unregistered player ignored.');
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
        log.warning('received registration invoice response for player that has no pending registration: ' + playerId);
    }
  },

  receivePaymentArrived: function(address, amount) {
    log.debug("received payment arrived: " + address + " - " + amount);

    var pendingRegistration = _.find(this.pendingRegistrations,
      (e) => { return e.address === address; });

    var registrationId = "invalid";

    if (pendingRegistration) {
      pendingRegistration.balance += amount;

      var succeeded = (pendingRegistration.balance >= this.minimumRequiredSatoshis);

      var player = pendingRegistration.player;

      if (succeeded) {
        registrationId = "foobar" + address; // TODO: Use hash function instead
        delete this.pendingRegistrations[player.id];

        this.registeredPlayers[registrationId] = {
          address: address,
          redeemAddress: pendingRegistration.redeemAddress,
          balance: (pendingRegistration.balance / 1000),
        };

        player.registrationId = registrationId;

        // Immediately put player into online set
        this.tellOnline(player);

        // Make player receive the current balance
        player.updateTreasureBalance();
      }

      player.send(new Messages.RegisterPlayerResponse(succeeded, registrationId).serialize());

    } else {
      var reg;
      var registrationId;

      for (var k in this.registeredPlayers) {
        if (this.registeredPlayers[k].address === address) {
            registrationId = k;
            reg = this.registeredPlayers[k];
        }
      }
      log.debug("received payment arrived: " + address + " - " + amount);

      if (reg) {
        reg.balance += (amount / 1000);

        var player = this.onlineRegisteredPlayers[registrationId];
        player && player.updateTreasureBalance();

      } else {
        log.warning('Payment arrived but no corresponding pending registration found!');
      }

    }
  },

  receiveCashoutExecuted: function(registrationId, address, amount) {
    log.debug("received cashout executed: " + address + " - " + amount);

    var reg = this.registeredPlayers[registrationId];

    if (reg) {
      reg.balance -= (amount / 1000);

      var player = this.onlineRegisteredPlayers[registrationId];

      if (player) {
        player.updateTreasureBalance();
      } else {
        log.info('Player was not online. Balance change will be updated on next connection.');
      }

    } else {
      // TODO: Try to send money to registered player

      log.warning('Payment arrived but no corresponding pending registration found!');
    }

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
