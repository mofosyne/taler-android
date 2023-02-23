# GNU Taler Android Code Repository

This git repository contains code for GNU Taler Android apps and libraries.
The official location is: 

    https://git.taler.net/taler-android.git
    
## Structure

* [**cashier**](/cashier) - an Android app that enables you to take cash and give out electronic cash
* [**merchant-lib**](/merchant-lib) - a library providing communication with a merchant backend
  to be used by the point of sale app below.
* [**merchant-terminal**](/merchant-terminal) - a merchant point of sale terminal Android app
  that allows sellers to
  process customers’ orders by adding or removing products,
  calculate the amount owed by the customer
  and let the customer make a Taler payment via QR code or NFC.
* [**taler-kotlin-android**](/taler-kotlin-android) - an Android library containing common code
  needed by more than one Taler Android app.
* [**wallet**](/wallet) - a GNU Taler wallet Android app

## Building

You can get a list of possible build tasks like this:
    
    $ ./gradlew tasks
    
See the [Taler developer manual](https://docs.taler.net/developers-manual.html#build-apps-from-source).
for more information about building individual apps.
