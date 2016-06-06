var Messages = require("./message");
var cls = require("./lib/class");
var fs = require("fs");

module.exports = Economy = Class.extend({

  init: function(registeredPlayersFile) {
    this.pendingRegistrations = { };

    this.registeredPlayers = JSON.parse(fs.readFileSync(registeredPlayersFile, "utf8"));

    var self = this;

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
    // TODO: provide asynchronously
    if (!this.pendingRegistrations[player.id]) {

      var registration = this.pendingRegistrations[player.id] = {
        player: player,
        address: null,
      };

      // TODO: Receive this from real server
      setTimeout(function() {
        // TODO: Placeholder values as if received from server
        var amount = 0.005;
        var address = "mhsChFHSZaxwQCP3P4gcTwSn6ENpGYkp8h";
        var label = "BrowserQuest";

        registration.address = address;

        player.send(new Messages.RegisterPlayerInvoice(true, address, amount, label).serialize());
      }, 100);

      // TODO: A fake response from the wallet server
      var self = this;
      setTimeout(function() {
        var succeeded = true;
        var registrationId = "bq_abcde";
        var address = registration.address;
        var initialBalance = 110;

        self.receiveRegistrationResponse(address, succeeded, registrationId, initialBalance);

      }, 5000);

    } else {
      // Registration cannot take place.
      player.send(new Messages.RegisterPlayerInvoice(false, "", 0, "").serialize());
    }

  },

  receiveRegistrationResponse : function (address, succeeded, registrationId, initialBalance) {
    var registration = null;
    for (var k in this.pendingRegistrations) {
      var e = this.pendingRegistrations[k];
      if (e.address === address) {
        registration = e;
        break;
      }
    }

    if (!registration) {
      log.warn('Could not find pending registration for address: '+ address);
    } else {
      delete this.pendingRegistrations[registrationId];

      var player = registration.player;

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
    }
  },

  maybeMakeTreasure : function() {
    // TODO: provide logic to make a treasure
  },

  checkBalance : function(player, callbackOrNull) {
    if (!player.registrationId) {
      this._tellBalance(-1, callbackOrNull);
    } else {
      var registeredPlayer = this.registeredPlayers[player.registrationId];
      if (registeredPlayer) {
        var balance = registeredPlayer.balance;

        this._tellBalance(balance, callbackOrNull);
      } else {
        log.debug('Did not find registered player for id:' + player.registrationId);
      }
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
