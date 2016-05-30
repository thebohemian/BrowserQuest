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

  generateParticipationInvoice : function() {
    // TODO: provide asynchronously
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
