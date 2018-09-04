Wallet Server for interaction with BrowserQuest backend

- handles incoming and outgoing payments
- based on BitcoinJ, does unbatched onchain transactions ...

Prerequisites:
 - gradle (from your package manager)
 - OpenJDK8

How to use:
-----------


1. You first need a working wallet. Run

	gradle runCreateWallet

You'll find a wallet for testnet in config/test.json now.

If you feel adventurous you can also create a mainnet wallet using

	gradle runCreateWalletMain

The wallet details are in config/mainexample.json .

2. Next you need to add two config values to your configuration file.

The server's hostname and the port on which to run the backend. For experiments
on your machine it is best to use "localhost" and the default port to which
the BrowserQuest server will connect to 8000 .

So add the following to config/test.json or config/mainexample.json (depending on
what you intent to run later):

  "serverHostname": "localhost",
  "serverPort": 8000

3. Next you start the wallet server:

For Bitcoin testnet

	gradle runServer

For Bitcoin mainnet

	gradle runServerMain

4. Usage

On the first start the server will access the network and sync the blockchain (headers only).
This might take a while. When done you will be greeted with a terminal-like interface. This
is BeanShell. It is basically an interpreter for Java. The server code injected a few interesting
variables already and you can basically call any method on those instances. This way you will
make the server show you a wallet address.

Type this:

  backend.createRegistrationInvoice()

You will see some output, including an address. You can send BTC to that address to fill the wallet
of the game server. This is necessary for payouts to work.

With the wallet server running you need to start the BrowserQuest server and client now!
