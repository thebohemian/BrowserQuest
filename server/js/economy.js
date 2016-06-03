var Messages = require("./message");
var cls = require("./lib/class");

module.exports = Economy = Class.extend({

  init: function(config) {
    this.balance = 100;
  },

  _tellBalance : function(b, callbackOrNull) {
    setTimeout(function() {
      callbackOrNull && callbackOrNull(b);
    }, 500);
  },

  generateRegistrationInvoice: function(player) {
    // TODO: provide asynchronously
    var isPossible = true;
    var amount = 0.005;
    var address = "mhsChFHSZaxwQCP3P4gcTwSn6ENpGYkp8h";
    var label = "BrowserQuest";

    setTimeout(function() {
      player.send(new Messages.RegisterPlayerInvoice(isPossible, address, amount, label).serialize());
    }, 100);
  },

  maybeMakeTreasure : function() {
    // TODO: provide logic to make a treasure
  },

  checkBalance : function(player, callbackOrNull) {
    this._tellBalance(this.balance, callbackOrNull);
  },

  giveTreasure : function(player, treasureId, callbackOrNull) {
    var self = this;

    self.balance++;

    this._tellBalance(this.balance, callbackOrNull);
  }

});
